package com.yiruantong.job.snailjob.common;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.yiruantong.basic.domain.product.BaseBoxSpec;
import com.yiruantong.basic.domain.product.BaseProduct;
import com.yiruantong.basic.service.product.IBaseBoxSpecService;
import com.yiruantong.basic.service.product.IBaseProductService;
import com.yiruantong.basic.tmx.BomLineRow;
import com.yiruantong.basic.tmx.TmxApiException;
import com.yiruantong.basic.tmx.TmxClient;
import com.yiruantong.basic.tmx.TmxIntegrationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 箱规自动生成编排测试（Task 8）。
 *
 * <p>夹具形状取自真实 BOM（BOM汇总.xlsx）：T10110001 四条包装行（纸箱+缓冲垫×2+袋），
 * T10210003 四条（纸盒+纸箱+EPE×2）。断言落在：有名称能自动生成、无名称不猜、
 * 无包装行跳过、幂等更新、取数失败上抛。
 */
class BoxSpecSyncServiceTest {

  private IBaseProductService productService;
  private IBaseBoxSpecService boxSpecService;
  private TmxIntegrationConfig config;
  private FakeBoxClient client;

  private LambdaQueryChainWrapper<BaseProduct> productChain;
  private LambdaQueryChainWrapper<BaseBoxSpec> specChain;

  private BoxSpecSyncService service;

  @BeforeEach
  void setUp() {
    productService = mock(IBaseProductService.class);
    boxSpecService = mock(IBaseBoxSpecService.class);
    config = mock(TmxIntegrationConfig.class);
    client = new FakeBoxClient();

    productChain = mock(LambdaQueryChainWrapper.class);
    when(productService.lambdaQuery()).thenReturn(productChain);
    when(productChain.list()).thenReturn(List.of(product("T10110001"), product("T10210003")));

    specChain = mock(LambdaQueryChainWrapper.class);
    when(boxSpecService.lambdaQuery()).thenReturn(specChain);
    when(specChain.eq(any(), any())).thenReturn(specChain);

    when(config.buildClient()).thenReturn(client);
    service = new BoxSpecSyncService(config, productService, boxSpecService);
  }

  private static BaseProduct product(String code) {
    BaseProduct p = new BaseProduct();
    p.setProductId(code.hashCode() & 0xffffL);
    p.setProductCode(code);
    return p;
  }

  private static BomLineRow pkg(String no, String name, String qtyText, long scaled) {
    BomLineRow r = new BomLineRow();
    r.setChildPartNo(no);
    r.setChildPartName(name);
    r.setQtyText(qtyText);
    r.setQtyScaled(scaled);
    r.setMaterialType("包材");
    return r;
  }

  /** 脚本化客户端：按料号给 BOM 行（真实形状）。 */
  private static final class FakeBoxClient extends TmxClient {
    private final Map<String, List<BomLineRow>> byPart = new LinkedHashMap<>();
    private TmxApiException failWith;

    FakeBoxClient() {
      super("http://tmx.fake", () -> "token", 1000);
    }

    @Override
    public List<BomLineRow> fetchBomLines(String partNo) {
      if (failWith != null) {
        throw failWith;
      }
      return byPart.getOrDefault(partNo, List.of());
    }
  }

  private void wireRealBomLines(boolean withNames) {
    client.byPart.put("T10110001", List.of(
      pkg("T10510001-01", withNames ? "纸箱 材质：BC/F ECT=8000N规格：685*415*385mm；16pcs/CTN" : null, "1/16", 625),
      pkg("T10510001-02", withNames ? "缓冲垫  材质：25kg/m3 白色防静电EPE 规格：680*410*95mm" : null, "1/4", 2500),
      pkg("T10510001-03", withNames ? "缓冲垫  材质：25kg/m3 白色防静电EPE 规格：545*410*25mm" : null, "1/4", 2500),
      pkg("T10510001-04", withNames ? "透明尼龙袋900*580mm" : null, "1/4", 2500)));
    client.byPart.put("T10210003", List.of(
      pkg("T10510002-01", withNames ? "纸盒  材质：350g白卡 规格：110*90*57mm;1pcs/BOX" : null, "1", 10000),
      pkg("T10510002-02", withNames ? "纸箱 材质：BC/F ECT=8000N 规格：465*290*190mm；36pcs/CTN" : null, "1/36", 278),
      pkg("T10510002-03", withNames ? "22kg/m3 白色EPE 规格：110*90*45mm" : null, "1", 10000),
      pkg("T10510002-04", withNames ? "22kg/m3 白色EPE 规格：110*90*10mm" : null, "1", 10000)));
  }

  @Test
  @DisplayName("有子件名称：两个真实产品各自生成正确箱规（SKU 16/1 与 BOTH 1/36）")
  void generatesBothRealShapes() {
    wireRealBomLines(true);
    when(boxSpecService.save(any())).thenReturn(true);

    BoxSpecSyncReport report = service.sync();

    assertEquals(2, report.getScanned());
    assertEquals(2, report.getInserted());
    assertTrue(report.getExceptions().isEmpty(), report.getExceptions().toString());

    ArgumentCaptor<BaseBoxSpec> captor = ArgumentCaptor.forClass(BaseBoxSpec.class);
    verify(boxSpecService, times(2)).save(captor.capture());
    BaseBoxSpec sku = captor.getAllValues().get(0);
    assertEquals("外箱16pcs/CTN（纸箱T10510001-01）", sku.getSpecName());
    assertEquals(new BigDecimal("16"), sku.getInnerQty());
    assertEquals(1, sku.getOuterInnerCount());
    assertEquals("SKU", sku.getBoxCodeMode());
    assertEquals("BOM包装用量原值 1/16", sku.getRemark());
    assertEquals("系统导入", sku.getCreateByName());
    assertEquals(1, sku.getIsDefault());

    BaseBoxSpec both = captor.getAllValues().get(1);
    assertEquals("内盒1pcs/BOX+外箱36盒/CTN（T10510002-01/02）", both.getSpecName());
    assertEquals(new BigDecimal("1"), both.getInnerQty());
    assertEquals(36, both.getOuterInnerCount());
    assertEquals("BOTH", both.getBoxCodeMode());
  }

  @Test
  @DisplayName("无子件名称 + 多包装行：两个产品都进例外清单，一条箱规都不落")
  void withoutNamesNothingIsGuessed() {
    wireRealBomLines(false);

    BoxSpecSyncReport report = service.sync();

    assertEquals(2, report.getExceptions().size());
    verify(boxSpecService, never()).save(any());
    verify(boxSpecService, never()).updateById(any());
    assertTrue(report.getExceptions().stream().allMatch(e -> e.contains("child_part_name")));
  }

  @Test
  @DisplayName("无包装行：跳过（不需要箱规，不是错误）")
  void noPackagingSkipped() {
    BoxSpecSyncReport report = service.sync();

    assertEquals(2, report.getSkippedNoPackaging());
    assertEquals(0, report.getSpecsWritten());
    verify(boxSpecService, never()).save(any());
  }

  @Test
  @DisplayName("同名箱规已存在：更新而不是插第二条（幂等）")
  void existingSpecUpdatedNotDuplicated() {
    wireRealBomLines(true);
    BaseBoxSpec existing = new BaseBoxSpec();
    existing.setSpecId(99L);
    existing.setProductId(product("T10110001").getProductId());
    when(specChain.one()).thenReturn(existing, null);
    when(boxSpecService.updateById(any())).thenReturn(true);
    when(boxSpecService.save(any())).thenReturn(true);

    BoxSpecSyncReport report = service.sync();

    assertEquals(1, report.getUpdated());
    assertEquals(1, report.getInserted());
    ArgumentCaptor<BaseBoxSpec> captor = ArgumentCaptor.forClass(BaseBoxSpec.class);
    verify(boxSpecService).updateById(captor.capture());
    assertEquals(99L, captor.getValue().getSpecId(), "必须更新原行，不能换新行");
    assertEquals("外箱16pcs/CTN（纸箱T10510001-01）", captor.getValue().getSpecName());
  }

  @Test
  @DisplayName("取数失败：上抛让作业显式失败（禁静默跳过一个商品）")
  void fetchFailurePropagates() {
    client.failWith = new TmxApiException(503, "gateway down");

    assertThrows(TmxApiException.class, () -> service.sync());
    verify(boxSpecService, never()).save(any());
  }
}
