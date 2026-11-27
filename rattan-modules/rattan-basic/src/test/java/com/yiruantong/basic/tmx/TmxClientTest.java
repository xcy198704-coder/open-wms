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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TMX 只读/写入客户端测试。
 *
 * <p>用 JDK 内置 {@link HttpServer} 起真 HTTP 服务打全链路（HttpUtil → HTTP → JSON 解析），
 * 不 mock 传输层；断言全部落在真实行为上。响应体用手写 JSON 字面量，线上契约一眼可读。
 *
 * <p>约定：TMX 侧 DTO 的 JSON 字段是 snake_case（见 Go 侧
 * {@code internal/dcc/service/bom_tree_provider.go} 的 json tag）。
 */
class TmxClientTest {

  private HttpServer server;
  private String baseUrl;
  private final AtomicReference<String> lastAuthHeader = new AtomicReference<>();
  private final AtomicReference<String> lastRequestBody = new AtomicReference<>();
  private final AtomicReference<String> lastPath = new AtomicReference<>();

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    // 上下文只注册一次（同一 path 不能重复 createContext）；stub() 只换响应内容。
    server.createContext("/", exchange -> {
      lastAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
      lastPath.set(exchange.getRequestURI().getPath());
      if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      }
      byte[] payload = stubBody == null ? new byte[0] : stubBody.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
      exchange.sendResponseHeaders(stubStatus, payload.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(payload);
      }
    });
    server.start();
  }

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  private volatile int stubStatus = 200;
  private volatile String stubBody = "";

  private void stub(int status, String body) {
    this.stubStatus = status;
    this.stubBody = body;
  }

  private TmxClient newClient() {
    return new TmxClient(baseUrl, () -> "test-token-123", 5000);
  }

  @Test
  @DisplayName("查物料列表：按 snake_case 信封解析出料号字段")
  void fetchParts_returnsBriefs_fromSnakeCaseEnvelope() {
    stub(200, """
      {"parts":[
        {"part_no":"T10110001","name":"线缆组件4116-1550-600","unit":"pcs","material_category":"成品",
         "part_kind":"产品","part_attr":"自制","package_part_no":"T10510001","version":"A0"},
        {"part_no":"T20010001","name":"定制同轴电缆RF1.2(红色)","unit":"m","material_category":"电缆",
         "part_kind":"零件","part_attr":"采购"}
      ],"degraded":false}""");

    List<PartBrief> parts = newClient().fetchParts("T", 10);

    assertEquals(2, parts.size());
    PartBrief first = parts.get(0);
    assertEquals("T10110001", first.getPartNo());
    assertEquals("线缆组件4116-1550-600", first.getName());
    assertEquals("pcs", first.getUnit());
    assertEquals("成品", first.getMaterialCategory());
    assertEquals("自制", first.getPartAttr());
    assertEquals("T10510001", first.getPackagePartNo());
    assertEquals("m", parts.get(1).getUnit());
  }

  @Test
  @DisplayName("TMX 以后加了新字段：忽略未知字段，不报错")
  void fetchParts_ignoresUnknownFields() {
    stub(200, """
      {"parts":[{"part_no":"T10110001","name":"线缆组件4116-1550-600","unit":"pcs",
                 "some_future_field":"whatever","another":{"nested":1}}],"degraded":false}""");

    List<PartBrief> parts = newClient().fetchParts("T", 10);

    assertEquals(1, parts.size());
    assertEquals("T10110001", parts.get(0).getPartNo());
  }

  @Test
  @DisplayName("查单个物料：解析 part 信封与项目三维")
  void fetchPart_returnsDetail() {
    stub(200, """
      {"part":{"part_no":"T10110001","name":"线缆组件4116-1550-600","customer_code":"FT",
               "internal_code":"BY-ML","product_series":"Memory2"},"degraded":false}""");

    PartDetail detail = newClient().fetchPart("T10110001");

    assertEquals("T10110001", detail.getPartNo());
    assertEquals("BY-ML", detail.getInternalCode());
    assertEquals("Memory2", detail.getProductSeries());
  }

  @Test
  @DisplayName("查 BOM 行：qty_text 原文与定点用量都取到")
  void fetchBomLines_returnsRows() {
    stub(200, """
      {"lines":[
        {"child_part_no":"T10510001-01","qty_text":"1/16","qty_scaled":625,
         "material_type":"包材","is_virtual":false,"is_purchased":true,"unit":"pcs"},
        {"child_part_no":"T10210001-01","qty_text":"4","qty_scaled":40000,
         "material_type":"原料","is_virtual":false,"is_purchased":true,"unit":"pcs"}
      ],"degraded":false}""");

    List<BomLineRow> rows = newClient().fetchBomLines("T10110001");

    assertEquals(2, rows.size());
    assertEquals("T10510001-01", rows.get(0).getChildPartNo());
    assertEquals("1/16", rows.get(0).getQtyText());
    assertEquals(625L, rows.get(0).getQtyScaled());
    assertTrue(rows.get(0).getIsPurchased());
    assertFalse(rows.get(0).getIsVirtual());
  }

  @Test
  @DisplayName("请求带 Bearer 令牌")
  void fetchParts_sendsBearerToken() {
    stub(200, "{\"parts\":[],\"degraded\":false}");

    newClient().fetchParts("T", 5);

    assertEquals("Bearer test-token-123", lastAuthHeader.get());
  }

  @Test
  @DisplayName("TMX 返回 500：抛异常带状态码，不静默返回空列表")
  void fetchParts_throws_onServerError() {
    stub(500, "{\"error\":\"boom\"}");

    TmxApiException ex = assertThrows(TmxApiException.class, () -> newClient().fetchParts("T", 5));

    assertEquals(500, ex.getStatus());
    assertTrue(ex.getMessage().contains("500"), "异常信息应含状态码，实际: " + ex.getMessage());
  }

  @Test
  @DisplayName("端点路径与 TMX 侧接线一致（/api/wms/*，Bearer 通道）")
  void endpointPaths_matchTmxWiring() {
    stub(200, "{\"parts\":[],\"degraded\":false}");
    newClient().fetchParts("T", 5);
    assertEquals("/api/wms/parts", lastPath.get());

    stub(200, "{\"part\":null,\"degraded\":false}");
    newClient().fetchPart("T10110001");
    assertEquals("/api/wms/part", lastPath.get());

    stub(200, "{\"lines\":[],\"degraded\":false}");
    newClient().fetchBomLines("T10110001");
    assertEquals("/api/wms/bom-lines/T10110001", lastPath.get());
  }

  @Test
  @DisplayName("TMX 返回非 JSON：抛异常，不当成空结果")
  void fetchParts_throws_onInvalidJson() {
    stub(200, "<html>gateway error</html>");

    assertThrows(TmxApiException.class, () -> newClient().fetchParts("T", 5));
  }

  @Test
  @DisplayName("TMX 接口未接线（degraded=true）：抛异常，不当成空结果")
  void fetchParts_throws_whenDegraded() {
    stub(200, "{\"parts\":[],\"degraded\":true}");

    TmxApiException ex = assertThrows(TmxApiException.class, () -> newClient().fetchParts("T", 5));

    assertTrue(ex.getMessage().toLowerCase().contains("degraded"),
      "异常信息应说明接口未接线，实际: " + ex.getMessage());
  }

  @Test
  @DisplayName("推送库存单据：请求体带业务单号，duplicated 响应被解析")
  void postInventory_sendsBody_andParsesDuplicated() {
    stub(200, "{\"duplicated\":true,\"posted\":false}");

    InventoryPostRequest request = new InventoryPostRequest();
    request.setBizNo("IN20261127-0001");
    request.setBizType("INBOUND_ENTER");
    request.setPartNo("T10110001");
    request.setQty("16");
    request.setDirection("IN");
    request.setBoxCode("BOX-0001");
    request.setOccurredAt("2026-11-27T10:00:00");

    InventoryPostResult result = newClient().postInventory(request);

    assertTrue(result.getDuplicated());
    String body = lastRequestBody.get();
    assertNotNull(body, "POST 必须有请求体");
    assertTrue(body.contains("IN20261127-0001"), "请求体应含业务单号，实际: " + body);
    assertTrue(body.contains("T10110001"), "请求体应含料号，实际: " + body);
    assertTrue(body.contains("biz_no"), "字段应为 snake_case，实际: " + body);
  }

  @Test
  @DisplayName("推送库存单据被拒（500）：抛异常，由调用方重试")
  void postInventory_throws_onServerError() {
    stub(500, "{\"error\":\"db busy\"}");

    InventoryPostRequest request = new InventoryPostRequest();
    request.setBizNo("IN20261127-0002");
    request.setPartNo("T10110001");

    TmxApiException ex = assertThrows(TmxApiException.class, () -> newClient().postInventory(request));

    assertEquals(500, ex.getStatus());
  }
}
