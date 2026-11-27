package com.yiruantong.basic.tmx;

import com.yiruantong.basic.domain.product.bo.BaseProductBo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TMX 料号 → WMS 商品档案的字段映射测试（Task 4 字段权威规则）。
 *
 * <p>权威表（TMX 对接实施计划 Task 4）：
 * product_code/product_name/small_unit/type_id/big_type_id/product_model/product_desc
 * 以 TMX 为准；默认库位/箱规/价格归 WMS 本地，映射器根本不碰这些字段。
 */
class MaterialMapperTest {

  private final MaterialMapper mapper = new MaterialMapper();

  /** 造一个字段齐全的详情（对齐 TMX 侧 fixture 的真实形态）。 */
  private static PartDetail detail() {
    PartDetail d = new PartDetail();
    d.setPartNo("T10110001");
    d.setName("线缆组件4116-1550-600");
    d.setSpec("4116-1550-600");
    d.setUnit("pcs");
    d.setMaterialCategory("成品");
    d.setPartKind("产品");
    d.setPartAttr("自制");
    d.setVersion("A0");
    d.setLifecycle("量产");
    d.setApproveState("已核准");
    d.setUpdatedAt("2026-11-20 10:00:00");
    d.setCustomerCode("FT");
    d.setInternalCode("BY-ML");
    d.setProductSeries("Memory2");
    return d;
  }

  @Test
  @DisplayName("权威字段逐一映射：编号/名称/规格/单位/项目/描述")
  void mapsAuthoritativeFields() {
    BaseProductBo bo = mapper.map(detail()).getBo();

    assertEquals("T10110001", bo.getProductCode());
    assertEquals("线缆组件4116-1550-600", bo.getProductName());
    assertEquals("4116-1550-600", bo.getProductSpec());
    assertEquals("pcs", bo.getSmallUnit());
    // 项目/机型串与 11-25 人工导入先例同形：BY-ML(FT)
    assertEquals("BY-ML(FT)", bo.getProductModel());
    // 描述记录版本/来源/同步时间，对拍可 grep
    assertTrue(bo.getProductDesc().contains("TMX同步"));
    assertTrue(bo.getProductDesc().contains("A0"));
    assertTrue(bo.getProductDesc().contains("2026-11-20 10:00:00"));
  }

  @Test
  @DisplayName("映射器不碰本地字段：默认货位/箱规/价格一律不设")
  void localFieldsUntouched() {
    BaseProductBo bo = mapper.map(detail()).getBo();

    assertNull(bo.getPositionName(), "默认货位归 WMS 本地，同步不得覆盖");
    assertNull(bo.getPurchasePrice());
    assertNull(bo.getSalePrice());
  }

  @Test
  @DisplayName("项目/机型串的四级回落：双码→internalCode(customerCode)，单码→该码，皆无→系列")
  void projectModelFallbacks() {
    PartDetail d = detail();
    d.setInternalCode("BY-ML");
    d.setCustomerCode("FT");
    assertEquals("BY-ML(FT)", mapper.map(d).getBo().getProductModel());

    d.setCustomerCode(null);
    assertEquals("BY-ML", mapper.map(d).getBo().getProductModel());

    d.setInternalCode(null);
    d.setCustomerCode("FT");
    assertEquals("FT", mapper.map(d).getBo().getProductModel());

    d.setInternalCode(null);
    d.setCustomerCode(null);
    assertEquals("Memory2", mapper.map(d).getBo().getProductModel());

    d.setProductSeries(null);
    assertEquals("", mapper.map(d).getBo().getProductModel());
  }

  @Test
  @DisplayName("虚拟件不建商品档案：virtualPart=true 且不给 Bo")
  void virtualPartSkipped() {
    PartDetail d = detail();
    d.setPartAttr("虚拟");

    MaterialMapping m = mapper.map(d);

    assertTrue(m.isVirtualPart());
    assertNull(m.getBo(), "虚拟件只进例外清单，不产出商品 Bo");
  }

  @Test
  @DisplayName("停用态判定：已驳回=停用；存量空值/已承认/已审核/已核准=活跃")
  void inactiveByApproveState() {
    PartDetail d = detail();

    d.setApproveState("已驳回");
    assertTrue(mapper.map(d).isInactive());

    for (String active : new String[]{"", "已承认", "已审核", "已核准"}) {
      d.setApproveState(active);
      assertFalse(mapper.map(d).isInactive(), active + " 应判活跃（存量未标按活跃）");
    }
  }

  @Test
  @DisplayName("类别未命中回落 BOM-RM-TBD 并记 WARN，不返回 null")
  void unknownCategoryFallsBackToTbd() {
    PartDetail d = detail();
    d.setMaterialCategory("不存在的类别");

    MaterialMapping m = mapper.map(d);

    assertEquals(CategoryMapping.TBD, m.getTypeCode());
    assertFalse(m.getWarnings().isEmpty(), "回落待分类必须记 WARN");
  }

  @Test
  @DisplayName("已知类别原样解析：成品 → BOM-FG-*")
  void knownCategoryResolves() {
    MaterialMapping m = mapper.map(detail());

    assertTrue(m.getTypeCode().startsWith("BOM-FG-"), m.getTypeCode());
    assertTrue(m.getWarnings().isEmpty());
  }
}
