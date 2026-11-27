package com.yiruantong.basic.tmx;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * TMX 平台客户端（WMS 侧唯一出口）。
 *
 * <p>纪律：
 * <ul>
 *   <li>**只走 HTTP 服务接口**，不连 TMX 的 SQLite（K10「禁跨模块直写表」同款口径）；</li>
 *   <li>**失败不静默**：非 200、非 JSON、{@code degraded=true}（TMX 接口未接线）一律抛
 *       {@link TmxApiException}，禁止退化成「返回空列表」；</li>
 *   <li>JSON 字段 snake_case，与 TMX Go 侧 DTO 对齐。</li>
 * </ul>
 *
 * <p>端点路径与 TMX 侧《TMX对接-分工与接入说明》的契约一一对应；WMS 侧的开关与水位在
 * {@code TmxIntegrationConfig}，不在本类。
 *
 * <p>JSON 用自持 {@link ObjectMapper}（不用项目 {@code JsonUtils}：那个类从 Spring 容器取
 * ObjectMapper，离开 Spring 环境就初始化失败，纯单测跑不了）；并显式忽略未知字段——
 * TMX 以后加字段不应该把我们打断。
 */
public class TmxClient {

  private static final ObjectMapper MAPPER = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  /**
   * TMX 侧端点路径（2026-11-27 与 Go 侧接线结果对齐）。
   *
   * <p>为什么是 {@code /api/wms/*} 而不是 {@code /materials/api/*}：TMX 的 {@code /materials}
   * 页面组只认 {@code gfs_token} cookie，服务端到服务端没有 cookie 会被 302 弹回登录页；
   * {@code /api} 组走 {@code middleware.AuthRequired()}，认 {@code Authorization: Bearer}。
   * BOM 端点另挂 TMX 的 {@code materials.bom.views} 操作点（服务账号需持该码）。
   */
  private static final String PARTS_PATH = "/api/wms/parts";
  private static final String PART_PATH = "/api/wms/part";
  private static final String BOM_PATH = "/api/wms/bom-lines/";

  /** 库存接入接口挂载路径——待 TMX 确认（/inventory/api/ 还是 /erp/api/，见分工说明口径二） */
  private static final String INVENTORY_POST_PATH = "/inventory/api/post";

  private final String baseUrl;
  private final Supplier<String> tokenSupplier;
  private final int timeoutMs;

  /**
   * @param tokenSupplier 令牌提供者。平台 JWT 默认 7 天有效，生产必须传
   *                       {@link TmxLoginTokenProvider}（自动续期）；传固定值仅用于测试。
   */
  public TmxClient(String baseUrl, Supplier<String> tokenSupplier, int timeoutMs) {
    this.baseUrl = baseUrl;
    this.tokenSupplier = tokenSupplier;
    this.timeoutMs = timeoutMs;
  }

  // ---------------------------------------------------------------- 读

  /** 按关键字查料号摘要（分页）。 */
  public List<PartBrief> fetchParts(String kw, int limit) {
    Map<String, Object> params = new HashMap<>();
    params.put("q", kw == null ? "" : kw);
    params.put("limit", limit);
    PartsEnvelope envelope = get(buildUrl(PARTS_PATH), params, PartsEnvelope.class);
    return envelope.getParts() == null ? new ArrayList<>() : envelope.getParts();
  }

  /** 查单个料号详情。 */
  public PartDetail fetchPart(String partNo) {
    Map<String, Object> params = new HashMap<>();
    params.put("part_no", partNo);
    PartEnvelope envelope = get(buildUrl(PART_PATH), params, PartEnvelope.class);
    return envelope.getPart();
  }

  /** 查某个产品的 BOM 行（含包装行，每箱装数由 qtyText 推导）。 */
  public List<BomLineRow> fetchBomLines(String partNo) {
    LinesEnvelope envelope = get(buildUrl(BOM_PATH + partNo), null, LinesEnvelope.class);
    return envelope.getLines() == null ? new ArrayList<>() : envelope.getLines();
  }

  // ---------------------------------------------------------------- 写

  /** 推送库存单据；{@code duplicated=true} 表示 TMX 已过账，调用方按成功处理。 */
  public InventoryPostResult postInventory(InventoryPostRequest request) {
    PostEnvelope envelope = post(buildUrl(INVENTORY_POST_PATH), toJson(request), PostEnvelope.class);
    InventoryPostResult result = new InventoryPostResult();
    result.setDuplicated(envelope.getDuplicated());
    result.setPosted(envelope.getPosted());
    return result;
  }

  // ---------------------------------------------------------------- 内部

  private String buildUrl(String path) {
    return baseUrl + path;
  }

  private <T> T get(String url, Map<String, Object> params, Class<T> envelopeType) {
    HttpRequest request = HttpUtil.createRequest(Method.GET, url)
      .header("Authorization", "Bearer " + tokenSupplier.get())
      .timeout(timeoutMs);
    if (params != null) {
      request.form(params);
    }
    try (HttpResponse response = request.execute()) {
      return parseEnvelope(response.getStatus(), response.body(), envelopeType);
    }
  }

  private <T> T post(String url, String jsonBody, Class<T> envelopeType) {
    try (HttpResponse response = HttpUtil.createRequest(Method.POST, url)
      .header("Authorization", "Bearer " + tokenSupplier.get())
      .header("Content-Type", "application/json")
      .body(jsonBody)
      .timeout(timeoutMs)
      .execute()) {
      return parseEnvelope(response.getStatus(), response.body(), envelopeType);
    }
  }

  private static String toJson(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (Exception e) {
      throw new TmxApiException("序列化推送内容失败", e);
    }
  }

  /**
   * 统一解析 + 失败判定。三种失败都抛 {@link TmxApiException}，不返回空信封：
   * 非 200、响应体不可解析、{@code degraded=true}。
   */
  private <T> T parseEnvelope(int status, String body, Class<T> envelopeType) {
    if (status != 200) {
      throw new TmxApiException(status, body);
    }
    if (body == null || body.isBlank()) {
      throw new TmxApiException(status, "<empty body>");
    }
    T envelope;
    try {
      envelope = MAPPER.readValue(body, envelopeType);
    } catch (Exception e) {
      throw new TmxApiException("TMX 响应不是合法 JSON（status=" + status + "）", e);
    }
    if (envelope == null) {
      throw new TmxApiException(status, body);
    }
    if (envelope instanceof Degradable degradable && Boolean.TRUE.equals(degradable.getDegraded())) {
      throw new TmxApiException(status, "TMX 接口未接线（degraded=true）：" + body);
    }
    return envelope;
  }

  /** 所有信封都带 degraded 标志（TMX 侧未接线时置 true）。 */
  private interface Degradable {
    Boolean getDegraded();
  }

  @Data
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  static class PartsEnvelope implements Degradable {
    private List<PartBrief> parts;
    private Boolean degraded;
  }

  @Data
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  static class PartEnvelope implements Degradable {
    private PartDetail part;
    private Boolean degraded;
  }

  @Data
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  static class LinesEnvelope implements Degradable {
    private List<BomLineRow> lines;
    private Boolean degraded;
  }

  @Data
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  static class PostEnvelope implements Degradable {
    private Boolean duplicated;
    private Boolean posted;
    private Boolean degraded;
  }
}
