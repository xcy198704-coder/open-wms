package com.yiruantong.basic.tmx;

import com.yiruantong.system.domain.core.bo.SysConfigBo;
import com.yiruantong.system.domain.core.vo.SysConfigVo;
import com.yiruantong.system.service.core.ISysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * TMX 集成参数（WMS 侧唯一读取入口）。
 *
 * <p>键位由 {@code script/sql/1.0.0.7_tmx_integration_config.sql} 创建：
 * {@code tmx_syncEnable} / {@code tmx_writebackEnable} / {@code tmx_baseUrl} /
 * {@code tmx_token} / {@code tmx_timeoutMs} / {@code tmx_syncWatermark}。
 *
 * <p>纪律：
 * <ul>
 *   <li>**开关开着但地址/令牌没配 = 显式抛错**（{@link #buildClient()}），不允许带空配置去调接口，
 *       最后变成「同步了 0 条」的静默成功；</li>
 *   <li>超时非法/空值回落 5000，不因一个脏配置让作业崩；</li>
 *   <li>令牌值不进 Git：SQL 脚本只建空值，真实值由现场在系统参数界面维护。
 *       若站点策略要求走环境变量，在本类读取处加一层「环境变量优先」即可，不要改调用方。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class TmxIntegrationConfig {

  private static final int DEFAULT_TIMEOUT_MS = 5000;

  private final ISysConfigService sysConfigService;

  // ---------------------------------------------------------------- 开关

  /** 物料主数据同步开关 */
  public boolean isSyncEnabled() {
    return sysConfigService.getConfigBool("tmx_syncEnable");
  }

  /** 出入库单据回写开关 */
  public boolean isWritebackEnabled() {
    return sysConfigService.getConfigBool("tmx_writebackEnable");
  }

  /** 任一开关开着（页面/作业用来判断是否需要暴露 TMX 入口） */
  public boolean isAnyEnabled() {
    return isSyncEnabled() || isWritebackEnabled();
  }

  // ---------------------------------------------------------------- 连接参数

  public String getBaseUrl() {
    return sysConfigService.selectConfigByKey("tmx_baseUrl");
  }

  public String getToken() {
    return sysConfigService.selectConfigByKey("tmx_token");
  }

  /** 超时（毫秒）；空值/非数字/非正数回落默认 5000 */
  public int getTimeoutMs() {
    String raw = sysConfigService.selectConfigByKey("tmx_timeoutMs");
    if (raw == null || raw.isBlank()) {
      return DEFAULT_TIMEOUT_MS;
    }
    try {
      int value = Integer.parseInt(raw.strip());
      return value > 0 ? value : DEFAULT_TIMEOUT_MS;
    } catch (NumberFormatException e) {
      return DEFAULT_TIMEOUT_MS;
    }
  }

  // ---------------------------------------------------------------- 水位

  public String getWatermark() {
    return sysConfigService.selectConfigByKey("tmx_syncWatermark");
  }

  /** 写同步水位：行已存在则更新原行，不存在则插入（幂等） */
  public void saveWatermark(String watermark) {
    SysConfigVo existing = sysConfigService.selectOneByKey("tmx_syncWatermark");
    SysConfigBo bo = new SysConfigBo();
    bo.setConfigKey("tmx_syncWatermark");
    bo.setConfigName("TMX 物料同步水位（作业自动维护，勿手改）");
    bo.setConfigType("N");
    bo.setConfigValue(watermark);
    if (existing == null) {
      sysConfigService.insertConfig(bo);
    } else {
      bo.setConfigId(existing.getConfigId());
      sysConfigService.updateConfig(bo);
    }
  }

  // ---------------------------------------------------------------- 客户端

  public String getUsername() {
    return sysConfigService.selectConfigByKey("tmx_username");
  }

  public String getPassword() {
    return sysConfigService.selectConfigByKey("tmx_password");
  }

  /**
   * 构建 TMX 客户端。**地址未配时抛 {@link IllegalStateException}**——
   * 调用方（作业/页面）应先判断开关，再取客户端；不要 try 掉这个异常当没看见。
   *
   * <p>令牌来源二选一（口令优先）：配了 {@code tmx_username}/{@code tmx_password} 就走
   * {@link TmxLoginTokenProvider} 自动续期（平台 JWT 默认 7 天有效，静态令牌一周必失效）；
   * 只配了 {@code tmx_token} 就用静态值（仅适合短期联调）。
   */
  public TmxClient buildClient() {
    String baseUrl = getBaseUrl();
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalStateException("TMX 对接未配置：sys_config.tmx_baseUrl 为空（开关开着但没配地址）");
    }
    String password = getPassword();
    if (password != null && !password.isBlank()) {
      String username = getUsername();
      if (username == null || username.isBlank()) {
        throw new IllegalStateException("TMX 对接未配置：sys_config.tmx_username 为空（配了口令但没配账号）");
      }
      TmxLoginTokenProvider tokenProvider = new TmxLoginTokenProvider(baseUrl.strip(), username.strip(), password, getTimeoutMs());
      return new TmxClient(baseUrl.strip(), tokenProvider::getToken, getTimeoutMs());
    }
    String token = getToken();
    if (token == null || token.isBlank()) {
      throw new IllegalStateException(
        "TMX 对接未配置：sys_config.tmx_token 为空（也未配 tmx_username/tmx_password，无法登录续期）");
    }
    return new TmxClient(baseUrl.strip(), () -> token.strip(), getTimeoutMs());
  }
}
