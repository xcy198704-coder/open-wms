package com.yiruantong.basic.tmx;

import com.sun.net.httpserver.HttpServer;
import com.yiruantong.system.domain.core.bo.SysConfigBo;
import com.yiruantong.system.domain.core.vo.SysConfigVo;
import com.yiruantong.system.service.core.ISysConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TMX 集成参数组件测试。
 *
 * <p>核心纪律：**开关开着但地址/令牌没配 = 显式报错**，不允许带着空配置去调接口然后
 * 「同步了 0 条」这种静默成功。
 */
class TmxIntegrationConfigTest {

  private ISysConfigService sysConfigService;
  private TmxIntegrationConfig config;

  @BeforeEach
  void setUp() {
    sysConfigService = mock(ISysConfigService.class);
    config = new TmxIntegrationConfig(sysConfigService);
  }

  @Test
  @DisplayName("同步开关：读 sys_config 的布尔值")
  void isSyncEnabled_readsConfigBool() {
    when(sysConfigService.getConfigBool("tmx_syncEnable")).thenReturn(true);
    assertTrue(config.isSyncEnabled());

    when(sysConfigService.getConfigBool("tmx_syncEnable")).thenReturn(false);
    assertFalse(config.isSyncEnabled());
  }

  @Test
  @DisplayName("开关开着但 baseUrl 为空：抛异常，不放行空配置")
  void buildClient_throws_whenBaseUrlBlank() {
    when(sysConfigService.getConfigBool("tmx_syncEnable")).thenReturn(true);
    when(sysConfigService.selectConfigByKey("tmx_baseUrl")).thenReturn("  ");
    when(sysConfigService.selectConfigByKey("tmx_token")).thenReturn("tok");

    IllegalStateException ex = assertThrows(IllegalStateException.class, config::buildClient);

    assertTrue(ex.getMessage().contains("tmx_baseUrl"), "异常应点名缺的键，实际: " + ex.getMessage());
    verify(sysConfigService, never()).selectConfigByKey("tmx_syncWatermark");
  }

  @Test
  @DisplayName("开关开着但令牌为空：抛异常")
  void buildClient_throws_whenTokenBlank() {
    when(sysConfigService.getConfigBool("tmx_syncEnable")).thenReturn(true);
    when(sysConfigService.selectConfigByKey("tmx_baseUrl")).thenReturn("https://192.168.110.222:6002");
    when(sysConfigService.selectConfigByKey("tmx_token")).thenReturn("");

    assertThrows(IllegalStateException.class, config::buildClient);
  }

  @Test
  @DisplayName("开关全关时不需要地址：不因缺配置而报错")
  void buildClient_notRequired_whenAllDisabled() {
    when(sysConfigService.getConfigBool("tmx_syncEnable")).thenReturn(false);
    when(sysConfigService.getConfigBool("tmx_writebackEnable")).thenReturn(false);

    assertFalse(config.isAnyEnabled());
  }

  @Test
  @DisplayName("超时：空值/非数字回落默认 5000，合法值照用")
  void getTimeoutMs_fallsBackToDefault() {
    when(sysConfigService.selectConfigByKey("tmx_timeoutMs")).thenReturn("");
    assertEquals(5000, config.getTimeoutMs());

    when(sysConfigService.selectConfigByKey("tmx_timeoutMs")).thenReturn("abc");
    assertEquals(5000, config.getTimeoutMs());

    when(sysConfigService.selectConfigByKey("tmx_timeoutMs")).thenReturn("8000");
    assertEquals(8000, config.getTimeoutMs());

    when(sysConfigService.selectConfigByKey("tmx_timeoutMs")).thenReturn(null);
    assertEquals(5000, config.getTimeoutMs());
  }

  @Test
  @DisplayName("配置齐全时构建出的客户端能真调到 TMX（端到端接线）")
  void buildClient_wiresConfiguredValues() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    String stubBase = "http://127.0.0.1:" + server.getAddress().getPort();
    server.createContext("/", exchange -> {
      byte[] payload = "{\"parts\":[],\"degraded\":false}".getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
      exchange.sendResponseHeaders(200, payload.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(payload);
      }
    });
    server.start();
    try {
      when(sysConfigService.getConfigBool("tmx_syncEnable")).thenReturn(true);
      when(sysConfigService.selectConfigByKey("tmx_baseUrl")).thenReturn(stubBase);
      when(sysConfigService.selectConfigByKey("tmx_token")).thenReturn("tok-abc");
      when(sysConfigService.selectConfigByKey("tmx_timeoutMs")).thenReturn("5000");

      List<PartBrief> parts = config.buildClient().fetchParts("T", 5);

      assertNotNull(parts);
      assertTrue(parts.isEmpty());
    } finally {
      server.stop(0);
    }
  }

  @Test
  @DisplayName("配了口令+账号：走登录Provider自动续期（真登录一次）")
  void buildClient_usesLoginProvider_whenPasswordConfigured() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    String stubBase = "http://127.0.0.1:" + server.getAddress().getPort();
    AtomicInteger logins = new AtomicInteger();
    String jwt = fakeJwt(Instant.now().plusSeconds(7200).getEpochSecond());
    server.createContext("/api/auth/login", ex -> {
      logins.incrementAndGet();
      byte[] out = ("{\"token\":\"" + jwt + "\"}").getBytes(StandardCharsets.UTF_8);
      ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
      ex.sendResponseHeaders(200, out.length);
      try (OutputStream os = ex.getResponseBody()) {
        os.write(out);
      }
    });
    server.createContext("/", ex -> {
      byte[] out = "{\"parts\":[],\"degraded\":false}".getBytes(StandardCharsets.UTF_8);
      ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
      ex.sendResponseHeaders(200, out.length);
      try (OutputStream os = ex.getResponseBody()) {
        os.write(out);
      }
    });
    server.start();
    try {
      when(sysConfigService.getConfigBool("tmx_syncEnable")).thenReturn(true);
      when(sysConfigService.selectConfigByKey("tmx_baseUrl")).thenReturn(stubBase);
      when(sysConfigService.selectConfigByKey("tmx_username")).thenReturn("wms-svc");
      when(sysConfigService.selectConfigByKey("tmx_password")).thenReturn("pwd-123");
      when(sysConfigService.selectConfigByKey("tmx_timeoutMs")).thenReturn("5000");

      List<PartBrief> parts = config.buildClient().fetchParts("T", 5);

      assertNotNull(parts);
      assertEquals(1, logins.get(), "首次调用应登录一次换令牌");
    } finally {
      server.stop(0);
    }
  }

  @Test
  @DisplayName("配了口令但没配账号：抛异常，不放行半配置")
  void buildClient_throws_whenPasswordWithoutUsername() {
    when(sysConfigService.getConfigBool("tmx_syncEnable")).thenReturn(true);
    when(sysConfigService.selectConfigByKey("tmx_baseUrl")).thenReturn("https://192.168.110.222:6002");
    when(sysConfigService.selectConfigByKey("tmx_username")).thenReturn("  ");
    when(sysConfigService.selectConfigByKey("tmx_password")).thenReturn("pwd-123");

    IllegalStateException ex = assertThrows(IllegalStateException.class, config::buildClient);

    assertTrue(ex.getMessage().contains("tmx_username"), "异常应点名缺的键，实际: " + ex.getMessage());
  }

  @Test
  @DisplayName("写水位：已存在则更新原行")
  void saveWatermark_updatesExistingRow() {
    SysConfigVo existing = new SysConfigVo();
    existing.setConfigId(7L);
    when(sysConfigService.selectOneByKey("tmx_syncWatermark")).thenReturn(existing);

    config.saveWatermark("2026-11-27T10:00:00");

    ArgumentCaptor<SysConfigBo> captor = ArgumentCaptor.forClass(SysConfigBo.class);
    verify(sysConfigService).updateConfig(captor.capture());
    assertEquals("2026-11-27T10:00:00", captor.getValue().getConfigValue());
    assertEquals(7L, captor.getValue().getConfigId());
    verify(sysConfigService, never()).insertConfig(any());
  }

  @Test
  @DisplayName("写水位：行不存在则插入")
  void saveWatermark_insertsWhenMissing() {
    when(sysConfigService.selectOneByKey("tmx_syncWatermark")).thenReturn(null);

    config.saveWatermark("2026-11-27T11:00:00");

    ArgumentCaptor<SysConfigBo> captor = ArgumentCaptor.forClass(SysConfigBo.class);
    verify(sysConfigService).insertConfig(captor.capture());
    assertEquals("tmx_syncWatermark", captor.getValue().getConfigKey());
    assertEquals("2026-11-27T11:00:00", captor.getValue().getConfigValue());
  }

  /** 造一个带 exp 声明的假 JWT（登录替身返回用）。 */
  private static String fakeJwt(long expEpochSeconds) {
    String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
    String payload = Base64.getUrlEncoder()
      .encodeToString(("{\"sub\":\"wms-svc\",\"exp\":" + expEpochSeconds + "}").getBytes(StandardCharsets.UTF_8));
    return header + "." + payload + ".signature";
  }
}
