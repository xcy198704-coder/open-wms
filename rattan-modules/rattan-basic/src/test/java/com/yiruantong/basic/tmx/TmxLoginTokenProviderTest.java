package com.yiruantong.basic.tmx;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TMX 登录取令牌Provider测试。
 *
 * <p>为什么需要它：平台 JWT 默认 7 天有效（config {@code auth.token_expiry: 168}），
 * 静态令牌一周后必失效。本组件负责登录换取、缓存、临近过期自动续期。
 */
class TmxLoginTokenProviderTest {

  private HttpServer server;
  private String baseUrl;
  private final AtomicInteger loginCount = new AtomicInteger();

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    loginCount.set(0);
    server.start();
  }

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  /** 造一个带 exp 声明的假 JWT（平台 JWT 的 payload 里有 exp，续期判断靠它）。 */
  private String fakeJwt(long expEpochSeconds) {
    String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
    String payload = Base64.getUrlEncoder().encodeToString(("{\"sub\":\"wms-svc\",\"exp\":" + expEpochSeconds + "}")
      .getBytes(StandardCharsets.UTF_8));
    return header + "." + payload + ".signature";
  }

  private void stubLogin(int status, String body) {
    server.createContext("/api/auth/login", exchange -> {
      loginCount.incrementAndGet();
      byte[] out = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
      exchange.sendResponseHeaders(status, out.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(out);
      }
    });
  }

  private TmxLoginTokenProvider newProvider() {
    return new TmxLoginTokenProvider(baseUrl, "wms-svc", "pwd-123", 5000);
  }

  @Test
  @DisplayName("首次取令牌走登录，之后复用缓存（不重复登录）")
  void getToken_logsInThenCaches() {
    stubLogin(200, "{\"token\":\"" + fakeJwt(Instant.now().plusSeconds(7200).getEpochSecond()) + "\"}");

    TmxLoginTokenProvider provider = newProvider();
    String first = provider.getToken();
    assertNotNull(first);
    assertTrue(first.contains("."), "应拿到 JWT");
    assertEquals(1, loginCount.get(), "第一次应登录一次");

    String second = provider.getToken();
    assertEquals(first, second, "同一实例应复用缓存令牌");
    assertEquals(1, loginCount.get(), "缓存有效期内第二次取令牌不应再登录");
  }

  @Test
  @DisplayName("令牌临近过期（<1 小时）：自动重新登录续期")
  void getToken_refreshesWhenNearExpiry() {
    long almostExpired = Instant.now().plusSeconds(60).getEpochSecond();
    stubLogin(200, "{\"token\":\"" + fakeJwt(almostExpired) + "\"}");

    TmxLoginTokenProvider provider = newProvider();
    String first = provider.getToken();
    assertEquals(1, loginCount.get());

    // 临界过期：应触发第二次登录
    String second = provider.getToken();
    assertEquals(2, loginCount.get(), "剩余不足 1 小时应重新登录");
    assertEquals(first, second, "测试替身返回同一个令牌，但登录次数应增加");
  }

  @Test
  @DisplayName("令牌已过期：重新登录")
  void getToken_refreshesWhenExpired() {
    stubLogin(200, "{\"token\":\"" + fakeJwt(Instant.now().minusSeconds(60).getEpochSecond()) + "\"}");

    TmxLoginTokenProvider provider = newProvider();
    provider.getToken();
    provider.getToken();

    assertEquals(2, loginCount.get(), "已过期的令牌必须重新登录");
  }

  @Test
  @DisplayName("登录失败（401）：抛异常，不返回空令牌")
  void getToken_throwsWhenLoginFails() {
    stubLogin(401, "{\"error\":\"invalid credentials\"}");

    TmxApiException ex = assertThrows(TmxApiException.class, () -> newProvider().getToken());

    assertEquals(401, ex.getStatus());
  }

  @Test
  @DisplayName("登录响应缺 token 字段：抛异常，不当成空字符串")
  void getToken_throwsWhenTokenMissing() {
    stubLogin(200, "{\"user\":{\"username\":\"wms-svc\"}}");

    assertThrows(TmxApiException.class, () -> newProvider().getToken());
  }

  @Test
  @DisplayName("并发取令牌：只登录一次（防并发风暴把平台登录限流打爆）")
  void getToken_concurrent_singleLogin() throws Exception {
    stubLogin(200, "{\"token\":\"" + fakeJwt(Instant.now().plusSeconds(7200).getEpochSecond()) + "\"}");
    TmxLoginTokenProvider provider = newProvider();

    int threads = 8;
    var start = new java.util.concurrent.CountDownLatch(1);
    var done = new java.util.concurrent.CountDownLatch(threads);
    var errors = new java.util.concurrent.atomic.AtomicInteger();
    for (int i = 0; i < threads; i++) {
      new Thread(() -> {
        try {
          start.await();
          provider.getToken();
        } catch (Exception e) {
          errors.incrementAndGet();
        } finally {
          done.countDown();
        }
      }).start();
    }
    start.countDown();
    assertTrue(done.await(10, java.util.concurrent.TimeUnit.SECONDS), "线程应全部结束");
    assertEquals(0, errors.get(), "并发取令牌不应报错");
    assertEquals(1, loginCount.get(), "并发只应登录一次");
  }

  @Test
  @DisplayName("JWT exp 解析：正常/畸形/缺失都有确定行为")
  void jwtExpiry_parsing() {
    long now = Instant.now().getEpochSecond();
    assertEquals(now, TmxLoginTokenProvider.parseJwtExp(fakeJwt(now)));

    assertEquals(0L, TmxLoginTokenProvider.parseJwtExp("not-a-jwt"));
    assertEquals(0L, TmxLoginTokenProvider.parseJwtExp("a.b.c"));
    assertEquals(0L, TmxLoginTokenProvider.parseJwtExp(null));
    // payload 不是 JSON
    String broken = Base64.getUrlEncoder().encodeToString("xxx".getBytes(StandardCharsets.UTF_8)) + "."
      + Base64.getUrlEncoder().encodeToString("not-json".getBytes(StandardCharsets.UTF_8)) + ".sig";
    assertEquals(0L, TmxLoginTokenProvider.parseJwtExp(broken));
  }
}
