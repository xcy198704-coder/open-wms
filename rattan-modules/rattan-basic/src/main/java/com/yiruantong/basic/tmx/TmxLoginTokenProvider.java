package com.yiruantong.basic.tmx;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * TMX 令牌提供者：登录换取 JWT、缓存、临近过期自动续期。
 *
 * <p>为什么需要：平台 JWT 默认 7 天有效（TMX {@code config.yaml} 的
 * {@code auth.token_expiry: 168}，单位小时）。把令牌静态配进 {@code sys_config} 意味着
 * 每周要人工换一次，忘了就是同步全线 401——所以由本组件按需登录续期。
 *
 * <p>口径：
 * <ul>
 *   <li>剩余有效期不足 {@link #REFRESH_MARGIN_SECONDS} 才重新登录（避免每次都打登录接口）；</li>
 *   <li>解析不出 {@code exp}（畸形令牌）时按「已过期」处理——宁可多登一次，也不拿一个
 *       不知道什么时候过期的令牌用到底；</li>
 *   <li>登录失败抛 {@link TmxApiException}，不返回空令牌（调用方据此告警，不会静默跳过同步）；</li>
 *   <li>整个方法 synchronized：并发取令牌只登录一次（平台登录带限流，并发风暴会被打爆）。</li>
 * </ul>
 *
 * <p>口令来源：{@code sys_config} 的 {@code tmx_username}/{@code tmx_password}（现场维护，
 * 不进 Git）。站点若要求口令只走环境变量，在 {@code TmxIntegrationConfig} 构造本对象时
 * 改取值来源即可，调用方无感。
 */
public class TmxLoginTokenProvider {

  /** 剩余不足 1 小时就续期 */
  static final long REFRESH_MARGIN_SECONDS = 3600L;

  private static final String LOGIN_PATH = "/api/auth/login";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String baseUrl;
  private final String username;
  private final String password;
  private final int timeoutMs;

  private String token;
  private long expiresAtEpochSeconds;

  public TmxLoginTokenProvider(String baseUrl, String username, String password, int timeoutMs) {
    this.baseUrl = baseUrl;
    this.username = username;
    this.password = password;
    this.timeoutMs = timeoutMs;
  }

  public synchronized String getToken() {
    long now = Instant.now().getEpochSecond();
    if (token == null || token.isBlank() || now >= expiresAtEpochSeconds - REFRESH_MARGIN_SECONDS) {
      login();
    }
    return token;
  }

  private void login() {
    String body;
    try (HttpResponse response = HttpUtil.createRequest(Method.POST, baseUrl + LOGIN_PATH)
      .header("Content-Type", "application/json")
      .body("{\"username\":\"" + escapeJson(username) + "\",\"password\":\"" + escapeJson(password) + "\"}")
      .timeout(timeoutMs)
      .execute()) {
      int status = response.getStatus();
      body = response.body();
      if (status != 200) {
        throw new TmxApiException(status, body);
      }
    }
    String newToken;
    long exp;
    try {
      JsonNode node = MAPPER.readTree(body);
      JsonNode tokenNode = node == null ? null : node.get("token");
      if (tokenNode == null || !tokenNode.isTextual() || tokenNode.asText().isBlank()) {
        throw new TmxApiException(200, "登录响应缺 token 字段：" + body);
      }
      newToken = tokenNode.asText();
      exp = parseJwtExp(newToken);
    } catch (TmxApiException e) {
      throw e;
    } catch (Exception e) {
      throw new TmxApiException("TMX 登录响应不是合法 JSON", e);
    }
    if (exp <= 0) {
      // 解析不出 exp = 不知道什么时候过期，按已过期处理（下次取令牌会再登一次）
      exp = Instant.now().getEpochSecond();
    }
    this.token = newToken;
    this.expiresAtEpochSeconds = exp;
  }

  /**
   * 解析 JWT 的 {@code exp}（秒级时间戳）。取不到/畸形返回 0（调用方按已过期处理）。
   */
  static long parseJwtExp(String jwt) {
    if (jwt == null) {
      return 0L;
    }
    String[] parts = jwt.split("\\.");
    if (parts.length < 2 || parts[1].isBlank()) {
      return 0L;
    }
    try {
      String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
      JsonNode node = MAPPER.readTree(json);
      JsonNode exp = node == null ? null : node.get("exp");
      if (exp == null || !exp.canConvertToLong()) {
        return 0L;
      }
      return exp.asLong();
    } catch (Exception e) {
      return 0L;
    }
  }

  private static String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
