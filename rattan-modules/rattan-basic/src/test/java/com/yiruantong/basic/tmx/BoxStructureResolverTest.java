package com.yiruantong.basic.tmx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 箱规结构解析测试（Task 8）。
 *
 * <p>夹具全部取自真实数据（BOM汇总.xlsx 工程BOM 表，2026-09-11）：T10110001 与
 * T10210003 的包装行原样录入——四个产品实测都是 4~8 条包材行，真箱与缓冲材
 * 只靠名称可分。
 */
class BoxStructureResolverTest {

  private final BoxStructureResolver resolver = new BoxStructureResolver();

  private static BomLineRow pkg(String childPartNo, String childPartName, String qtyText, long qtyScaled) {
    BomLineRow r = new BomLineRow();
    r.setChildPartNo(childPartNo);
    r.setChildPartName(childPartName);
    r.setQtyText(qtyText);
    r.setQtyScaled(qtyScaled);
    r.setMaterialType("包材");
    return r;
  }

  private static BomLineRow raw(String childPartNo, String qtyText, long qtyScaled) {
    BomLineRow r = new BomLineRow();
    r.setChildPartNo(childPartNo);
    r.setQtyText(qtyText);
    r.setQtyScaled(qtyScaled);
    r.setMaterialType("原料");
    return r;
  }

  /** T10110001 真实包装行：纸箱 1/16 + 缓冲垫 1/4×2 + 尼龙袋 1/4。 */
  private static List<BomLineRow> t10110001Packaging() {
    return List.of(
      pkg("T10510001-01", "纸箱 材质：BC/F ECT=8000N规格：685*415*385mm；16pcs/CTN", "1/16", 625),
      pkg("T10510001-02", "缓冲垫  材质：25kg/m3 白色防静电EPE 规格：680*410*95mm", "1/4", 2500),
      pkg("T10510001-03", "缓冲垫  材质：25kg/m3 白色防静电EPE 规格：545*410*25mm", "1/4", 2500),
      pkg("T10510001-04", "透明尼龙袋900*580mm", "1/4", 2500));
  }

  /** T10210003 真实包装行：纸盒 1pcs/BOX（用量 1）+ 纸箱 36pcs/CTN（1/36）+ EPE×2。 */
  private static List<BomLineRow> t10210003Packaging() {
    return List.of(
      pkg("T10510002-01", "纸盒  材质：350g白卡 规格：110*90*57mm;1pcs/BOX", "1", 10000),
      pkg("T10510002-02", "纸箱 材质：BC/F ECT=8000N 规格：465*290*190mm；36pcs/CTN", "1/36", 278),
      pkg("T10510002-03", "22kg/m3 白色EPE 规格：110*90*45mm", "1", 10000),
      pkg("T10510002-04", "22kg/m3 白色EPE 规格：110*90*10mm", "1", 10000));
  }

  @Test
  @DisplayName("T10110001（有名称）：SKU 16/1，名称与备注和人工导入逐字一致")
  void t10110001SkuWithNames() {
    BoxStructure s = resolver.resolve("T10110001", t10110001Packaging());

    assertEquals(16, s.getInnerQty());
    assertEquals(1, s.getOuterInnerCount());
    assertEquals("SKU", s.getBoxCodeMode());
    assertEquals("外箱16pcs/CTN（纸箱T10510001-01）", s.getSpecName());
    assertEquals("BOM包装用量原值 1/16", s.getRemark());
  }

  @Test
  @DisplayName("T10210003（有名称）：BOTH 内1外36，名称与备注和人工导入逐字一致")
  void t10210003BothWithNames() {
    BoxStructure s = resolver.resolve("T10210003", t10210003Packaging());

    assertEquals(1, s.getInnerQty());
    assertEquals(36, s.getOuterInnerCount());
    assertEquals("BOTH", s.getBoxCodeMode());
    assertEquals("内盒1pcs/BOX+外箱36盒/CTN（T10510002-01/02）", s.getSpecName());
    assertEquals("BOM包装用量原值 1/36", s.getRemark());
  }

  @Test
  @DisplayName("无子件名称 + 多包装行：抛异常不猜（当前契约下 T10210003 走这条）")
  void multiLineWithoutNamesThrows() {
    List<BomLineRow> noNames = t10210003Packaging().stream()
      .map(r -> {
        BomLineRow c = new BomLineRow();
        c.setChildPartNo(r.getChildPartNo());
        c.setQtyText(r.getQtyText());
        c.setQtyScaled(r.getQtyScaled());
        c.setMaterialType("包材");
        return c;
      }).toList();

    BoxStructureUndeterminedException ex = assertThrows(BoxStructureUndeterminedException.class,
      () -> resolver.resolve("T10210003", noNames));
    assertTrue(ex.getMessage().contains("child_part_name"), ex.getMessage());
  }

  @Test
  @DisplayName("无子件名称 + 单条包装行：无歧义，SKU")
  void singleLineWithoutNamesIsSku() {
    BoxStructure s = resolver.resolve("T99990001",
      List.of(pkg("T10519999-01", null, "1/24", 417)));

    assertEquals(24, s.getInnerQty());
    assertEquals(1, s.getOuterInnerCount());
    assertEquals("SKU", s.getBoxCodeMode());
  }

  @Test
  @DisplayName("没有包装行：返回 null（不需要箱规，不是错误）")
  void noPackagingReturnsNull() {
    assertNull(resolver.resolve("T20010001", List.of(raw("T20010002", "2", 20000))));
    assertNull(resolver.resolve("T20010001", List.of()));
  }

  @Test
  @DisplayName("外箱装数不是内盒整数倍：抛异常")
  void outerNotMultipleOfInnerThrows() {
    List<BomLineRow> lines = List.of(
      pkg("T10510002-01", "纸盒 2pcs/BOX", "1/2", 5000),
      pkg("T10510002-02", "纸箱 35pcs/CTN", "1/35", 286));

    assertThrows(BoxStructureUndeterminedException.class,
      () -> resolver.resolve("T10210003", lines));
  }

  @Test
  @DisplayName("包装行名称不在已知命名里：抛异常让人扩关键词/补数据，不静默忽略")
  void unknownPackagingNameThrows() {
    List<BomLineRow> lines = List.of(
      pkg("T10519999-01", "托盘 木制 1200×1000", "1/16", 625));

    assertThrows(BoxStructureUndeterminedException.class,
      () -> resolver.resolve("T99990001", lines));
  }

  @Test
  @DisplayName("多条内盒 / 多条外箱：抛异常")
  void duplicateBoxLinesThrow() {
    List<BomLineRow> twoInners = List.of(
      pkg("T10510002-01", "纸盒A", "1", 10000),
      pkg("T10510002-05", "纸盒B", "1", 10000),
      pkg("T10510002-02", "纸箱 36pcs/CTN", "1/36", 278));

    assertThrows(BoxStructureUndeterminedException.class,
      () -> resolver.resolve("T10210003", twoInners));
  }
}
