package com.yiruantong.job.snailjob.common;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.yiruantong.basic.domain.product.BaseProduct;
import com.yiruantong.basic.domain.product.BaseProductType;
import com.yiruantong.basic.service.product.IBaseProductService;
import com.yiruantong.basic.service.product.IBaseProductTypeService;
import com.yiruantong.basic.tmx.PartBrief;
import com.yiruantong.basic.tmx.PartDetail;
import com.yiruantong.basic.tmx.PartsPage;
import com.yiruantong.basic.tmx.TmxApiException;
import com.yiruantong.basic.tmx.TmxClient;
import com.yiruantong.basic.tmx.TmxIntegrationConfig;
import com.yiruantong.inventory.domain.core.CoreInventory;
import com.yiruantong.inventory.service.core.ICoreInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TMX 物料增量同步编排测试（Task 4 Step 1 的口径）。
 *
 * <p>TMX 侧用 {@link FakeTmxClient} 脚本化翻页/详情（传输层真实性由
 * {@code TmxClientTest} 的真 HTTP 覆盖）；DB 协作用 Mockito 深桩。
 * 断言全部落在计划要求的行为上：幂等 upsert、字段权威、停用三态、水位锚。
 */
class MaterialSyncServiceTest {

  private static final String PART_NEW = "T10110001";      // WMS 没有 → 新增
  private static final String PART_EXISTING = "T20010001"; // WMS 已有且本地改过默认货位

  private IBaseProductService productService;
  private IBaseProductTypeService productTypeService;
  private ICoreInventoryService inventoryService;
  private TmxIntegrationConfig config;
  private FakeTmxClient client;

  /** MP 链式查询的显式桩（深桩对自返回链不稳，显式链确定性更好）。 */
  private LambdaQueryChainWrapper<BaseProduct> productChain;
  private LambdaQueryChainWrapper<BaseProductType> typeChain;
  private LambdaQueryChainWrapper<CoreInventory> inventoryChain;

  private MaterialSyncService service;

  @BeforeEach
  void setUp() {
    productService = mock(IBaseProductService.class);
    productTypeService = mock(IBaseProductTypeService.class);
    inventoryService = mock(ICoreInventoryService.class);
    config = mock(TmxIntegrationConfig.class);
    client = new FakeTmxClient();

    productChain = mock(LambdaQueryChainWrapper.class);
    when(productService.lambdaQuery()).thenReturn(productChain);
    when(productChain.eq(any(), any())).thenReturn(productChain);

    typeChain = mock(LambdaQueryChainWrapper.class);
    when(productTypeService.lambdaQuery()).thenReturn(typeChain);
    when(typeChain.eq(any(), any())).thenReturn(typeChain);

    inventoryChain = mock(LambdaQueryChainWrapper.class);
    when(inventoryService.lambdaQuery()).thenReturn(inventoryChain);
    when(inventoryChain.eq(any(), any())).thenReturn(inventoryChain);
    when(inventoryChain.gt(any(), any())).thenReturn(inventoryChain);

    when(config.buildClient()).thenReturn(client);
    when(config.getWatermark()).thenReturn(null); // 默认全量首同步

    service = new MaterialSyncService(config, productService, productTypeService, inventoryService);
  }

  // ---------------------------------------------------------------- 夹具

  private static PartDetail detail(String partNo, String name, String unit, String materialCategory) {
    PartDetail d = new PartDetail();
    d.setPartNo(partNo);
    d.setName(name);
    d.setSpec(partNo + "-spec");
    d.setUnit(unit);
    d.setMaterialCategory(materialCategory);
    d.setPartAttr("自制");
    d.setVersion("A0");
    d.setApproveState("已核准");
    d.setUpdatedAt("2026-11-20 10:00:00");
    d.setInternalCode("BY-ML");
    d.setCustomerCode("FT");
    return d;
  }

  private static BaseProduct existingProduct() {
    BaseProduct p = new BaseProduct();
    p.setProductId(777L);
    p.setProductCode(PART_EXISTING);
    p.setProductName("旧名字（本地改过）");
    p.setSmallUnit("pcs");
    // 本地作业属性：同步不得覆盖
    p.setPositionName("A-01-01");
    p.setPurchasePrice(new BigDecimal("12.34"));
    return p;
  }

  private static BaseProductType wmsType() {
    BaseProductType t = new BaseProductType();
    t.setTypeId(55L);
    t.setTypeName("电缆");
    t.setParentId(11L);
    t.setRootName("原材料");
    return t;
  }

  /** 脚本化 TMX 客户端：翻页序列 + 料号详情表；记录 after/updated_since 实参。 */
  private static final class FakeTmxClient extends TmxClient {
    private final List<PartsPage> pages = new ArrayList<>();
    private final Map<String, PartDetail> details = new LinkedHashMap<>();
    private final List<String> afterCalls = new ArrayList<>();
    private final List<String> sinceCalls = new ArrayList<>();
    /** 非空则 fetchPartsPage 直接抛（模拟 TMX 故障）。 */
    private TmxApiException failWith;
    private int call = 0;

    FakeTmxClient() {
      super("http://tmx.fake", () -> "token", 1000);
    }

    @Override
    public PartsPage fetchPartsPage(String kw, int limit, String after, String updatedSince) {
      if (failWith != null) {
        throw failWith;
      }
      afterCalls.add(after);
      sinceCalls.add(updatedSince);
      if (call >= pages.size()) {
        PartsPage empty = new PartsPage();
        empty.setParts(List.of());
        empty.setServerTime("2026-11-27 20:10:00");
        return empty;
      }
      return pages.get(call++);
    }

    @Override
    public PartDetail fetchPart(String partNo) {
      return details.get(partNo);
    }
  }

  private static PartsPage page(List<PartBrief> parts, String nextAfter, String serverTime) {
    PartsPage p = new PartsPage();
    p.setParts(parts);
    p.setNextAfter(nextAfter);
    p.setServerTime(serverTime);
    return p;
  }

  private static PartBrief brief(String partNo) {
    PartBrief b = new PartBrief();
    b.setPartNo(partNo);
    b.setName(partNo + "-name");
    return b;
  }

  // ---------------------------------------------------------------- 用例

  @Test
  @DisplayName("新增落库 + 已存在刷权威字段，但默认货位/价格等本地字段不动")
  void syncsNewAndExistingParts() {
    client.pages.add(page(List.of(brief(PART_NEW), brief(PART_EXISTING)), "", "2026-11-27 20:10:00"));
    client.details.put(PART_NEW, detail(PART_NEW, "线缆组件4116-1550-600", "pcs", "成品"));
    client.details.put(PART_EXISTING, detail(PART_EXISTING, "定制同轴电缆RF1.2(红色)", "m", "电缆"));

    // 顺序查库：T10110001 没有 → null；T20010001 已有 → 旧档（本地改过货位）
    when(productChain.one()).thenReturn(null, existingProduct());
    when(typeChain.one()).thenReturn(wmsType());
    when(productService.save(any())).thenReturn(true);
    when(productService.updateById(any())).thenReturn(true);

    SyncReport report = service.sync();

    assertEquals(1, report.getInserted());
    assertEquals(1, report.getUpdated());
    assertTrue(report.getExceptions().isEmpty(), report.getExceptions().toString());

    // 新增：权威字段 + 系统导入标记 + 正常态
    ArgumentCaptor<BaseProduct> saved = ArgumentCaptor.forClass(BaseProduct.class);
    verify(productService).save(saved.capture());
    BaseProduct inserted = saved.getValue();
    assertEquals(PART_NEW, inserted.getProductCode());
    assertEquals("线缆组件4116-1550-600", inserted.getProductName());
    assertEquals("pcs", inserted.getSmallUnit());
    assertEquals("BY-ML(FT)", inserted.getProductModel());
    assertEquals("系统导入", inserted.getCreateByName());
    assertEquals(Long.valueOf(1), inserted.getProductStatus());
    // 类别经查库解析落 ID
    assertEquals(55L, inserted.getTypeId());
    assertEquals("原材料", inserted.getBigTypeName());

    // 已存在：名称/单位被 TMX 覆盖，本地默认货位与价格原样保留
    ArgumentCaptor<BaseProduct> updated = ArgumentCaptor.forClass(BaseProduct.class);
    verify(productService).updateById(updated.capture());
    BaseProduct after = updated.getValue();
    assertEquals("定制同轴电缆RF1.2(红色)", after.getProductName(), "名称以 TMX 为准");
    assertEquals("m", after.getSmallUnit(), "单位以 TMX 为准");
    assertEquals("A-01-01", after.getPositionName(), "默认货位归 WMS 本地，不得覆盖");
    assertEquals(new BigDecimal("12.34"), after.getPurchasePrice(), "价格归 WMS 本地，不得覆盖");
  }

  @Test
  @DisplayName("游标翻页：next_after 带回下一页，两页都处理，updated_since 用水位原值")
  void cursorPagingTraversesAllPages() {
    client.pages.add(page(List.of(brief(PART_NEW)), PART_EXISTING, "2026-11-27 20:10:00"));
    client.pages.add(page(List.of(brief(PART_EXISTING)), "", "2026-11-27 20:10:01"));
    client.details.put(PART_NEW, detail(PART_NEW, "线缆组件", "pcs", "成品"));
    client.details.put(PART_EXISTING, detail(PART_EXISTING, "电缆", "m", "电缆"));
    when(config.getWatermark()).thenReturn("2026-11-27 19:00:00");
    when(productChain.one()).thenReturn(null, null);
    when(typeChain.one()).thenReturn(wmsType());
    when(productService.save(any())).thenReturn(true);

    SyncReport report = service.sync();

    assertEquals(2, report.getPages());
    assertEquals(2, report.getInserted());
    assertEquals(List.of("", PART_EXISTING), client.afterCalls, "第二页必须带第一页的 next_after");
    assertEquals(List.of("2026-11-27 19:00:00", "2026-11-27 19:00:00"), client.sinceCalls,
      "水位原样回传，不本地加工");
  }

  @Test
  @DisplayName("水位 = 首次响应的 server_time 留 5 分钟边际（服务端时钟，不用客户端时钟）")
  void watermarkAnchoredOnServerTimeMinusMargin() {
    client.pages.add(page(List.of(brief(PART_NEW)), "", "2026-11-27 20:10:00"));
    client.details.put(PART_NEW, detail(PART_NEW, "线缆组件", "pcs", "成品"));
    when(productChain.one()).thenReturn(null);
    when(typeChain.one()).thenReturn(wmsType());
    when(productService.save(any())).thenReturn(true);

    service.sync();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(config).saveWatermark(captor.capture());
    assertEquals("2026-11-27 20:05:00", captor.getValue());
  }

  @Test
  @DisplayName("停用料号有库存：只进例外清单，不动商品（禁静默停用）")
  void inactivePartWithStockIsNotDisabled() {
    PartDetail d = detail(PART_EXISTING, "电缆", "m", "电缆");
    d.setApproveState("已驳回"); // TMX 停用态
    client.pages.add(page(List.of(brief(PART_EXISTING)), "", "2026-11-27 20:10:00"));
    client.details.put(PART_EXISTING, d);
    when(productChain.one()).thenReturn(existingProduct());
    // 有库存：count > 0
    when(inventoryChain.count()).thenReturn(3L);

    SyncReport report = service.sync();

    assertEquals(1, report.getBlockedByInventory());
    verify(productService, never()).updateById(any());
    assertTrue(report.getExceptions().stream().anyMatch(e -> e.contains(PART_EXISTING) && e.contains("库存")));
  }

  @Test
  @DisplayName("停用料号无库存：标记停用（productStatus=2）")
  void inactivePartWithoutStockIsDisabled() {
    PartDetail d = detail(PART_EXISTING, "电缆", "m", "电缆");
    d.setApproveState("已驳回");
    client.pages.add(page(List.of(brief(PART_EXISTING)), "", "2026-11-27 20:10:00"));
    client.details.put(PART_EXISTING, d);
    when(productChain.one()).thenReturn(existingProduct());
    when(inventoryChain.count()).thenReturn(0L);
    when(productService.updateById(any())).thenReturn(true);

    SyncReport report = service.sync();

    assertEquals(1, report.getMarkedDisabled());
    ArgumentCaptor<BaseProduct> captor = ArgumentCaptor.forClass(BaseProduct.class);
    verify(productService).updateById(captor.capture());
    assertEquals(Long.valueOf(2), captor.getValue().getProductStatus());  }

  @Test
  @DisplayName("停用料号 WMS 没有：不建档，只记例外")
  void inactiveNewPartIsNotCreated() {
    PartDetail d = detail(PART_NEW, "线缆组件", "pcs", "成品");
    d.setApproveState("已驳回");
    client.pages.add(page(List.of(brief(PART_NEW)), "", "2026-11-27 20:10:00"));
    client.details.put(PART_NEW, d);
    when(productService.lambdaQuery().eq(any(), any()).one()).thenReturn(null);

    SyncReport report = service.sync();

    assertEquals(1, report.getSkippedInactiveNew());
    verify(productService, never()).save(any());
  }

  @Test
  @DisplayName("虚拟件不建档案，只进例外清单")
  void virtualPartIsSkipped() {
    PartDetail d = detail(PART_NEW, "虚拟PCB", "pcs", "PCB");
    d.setPartAttr("虚拟");
    client.pages.add(page(List.of(brief(PART_NEW)), "", "2026-11-27 20:10:00"));
    client.details.put(PART_NEW, d);

    SyncReport report = service.sync();

    assertEquals(1, report.getSkippedVirtual());
    verify(productService, never()).save(any());
    verify(productService, never()).updateById(any());
  }

  @Test
  @DisplayName("曾停用的商品恢复活跃：刷字段并置回正常态")
  void activePartRestoresFromDisabled() {
    BaseProduct existing = existingProduct();
    existing.setProductStatus(2L);
    client.pages.add(page(List.of(brief(PART_EXISTING)), "", "2026-11-27 20:10:00"));
    client.details.put(PART_EXISTING, detail(PART_EXISTING, "电缆新名", "m", "电缆"));
    when(productChain.one()).thenReturn(existing);
    when(typeChain.one()).thenReturn(wmsType());
    when(productService.updateById(any())).thenReturn(true);

    SyncReport report = service.sync();

    assertEquals(1, report.getRestoredActive());
    ArgumentCaptor<BaseProduct> captor = ArgumentCaptor.forClass(BaseProduct.class);
    verify(productService).updateById(captor.capture());
    assertEquals(Long.valueOf(1), captor.getValue().getProductStatus());
    assertEquals("电缆新名", captor.getValue().getProductName());
  }

  @Test
  @DisplayName("取数失败向上抛：作业显式失败，水位不推进（禁静默跳过一轮同步）")
  void clientFailurePropagatesAndKeepsWatermark() {
    client.failWith = new TmxApiException(503, "gateway down");
    String before = "2026-11-27 19:00:00";
    when(config.getWatermark()).thenReturn(before);

    TmxApiException ex = assertThrows(TmxApiException.class, () -> service.sync());

    assertTrue(ex.getMessage().contains("503") || ex.toString().contains("503"));
    verify(config, never()).saveWatermark(any());
  }

  @Test
  @DisplayName("TMX 返回空详情：记例外继续，不当成功吞掉")
  void nullDetailIsRecordedAsException() {
    client.pages.add(page(List.of(brief(PART_NEW)), "", "2026-11-27 20:10:00"));
    // details 故意不放 PART_NEW → fetchPart 返回 null

    SyncReport report = service.sync();

    assertTrue(report.getExceptions().stream().anyMatch(e -> e.contains(PART_NEW)));
    verify(productService, never()).save(any());
  }
}
