package com.yiruantong.basic.tmx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TMX 物料类别 → WMS 商品分类 映射测试。
 *
 * <p>权威规则：WMS 的 3 大类 / 16 中类是 TMX {@code parts.material_category} 的投影，
 * 映射只此一处；**空/未知类别一律回落「待分类」并留给调用方告警**，不返回 null、不猜。
 */
class CategoryMappingTest {

  private final CategoryMapping mapping = new CategoryMapping();

  @Test
  @DisplayName("成品按料号前缀分：T1011=线缆组件，T1021=连接器成品")
  void finishedGoods_splitByPartNoPrefix() {
    assertEquals("BOM-FG-CABLE", mapping.resolve("成品", "T10110001", "线缆组件4116-1550-600"));
    assertEquals("BOM-FG-CABLE", mapping.resolve("成品", "T10110036", "线缆组件1272-2116-600"));
    assertEquals("BOM-FG-CONN", mapping.resolve("成品", "T10210003", "插座连接器P-550SRA-45"));
    assertEquals("BOM-FG-CONN", mapping.resolve("成品", "T10210016", "550S板端连接器"));
    assertEquals("BOM-FG-CONN", mapping.resolve("成品", "T10210032", "板端插座连接器P-098TRA-43"));
  }

  @Test
  @DisplayName("原材料 10 个类别一对一映射")
  void rawMaterials_oneToOne() {
    assertEquals("BOM-RM-PCB", mapping.resolve("PCB", "T10210001-02", "POGO连接器P256-PG116S：PCB-A"));
    assertEquals("BOM-RM-PLASTIC", mapping.resolve("塑料件", "T10210004-01", "Memory-HP288-400S：基座HOSING"));
    assertEquals("BOM-RM-TERMINAL", mapping.resolve("端子", "T10210007-02", "JN-CD-P-J444T：鱼眼端子"));
    assertEquals("BOM-RM-FASTENER", mapping.resolve("紧固件", "T10210001-07", "定位螺栓M1*8.8"));
    assertEquals("BOM-RM-METAL", mapping.resolve("金属件其他", "T10210003-09", "屏蔽罩"));
    assertEquals("BOM-RM-CABLE", mapping.resolve("电缆", "T20010001", "定制同轴电缆RF1.2(红色)"));
    assertEquals("BOM-RM-POGO", mapping.resolve("POGO", "T10330001", "单头探针PG-120143-C001"));
    assertEquals("BOM-RM-CONN", mapping.resolve("连接器", "T10110016-01", "SAM公头：SMA-J1.13"));
    assertEquals("BOM-RM-SLEEVE", mapping.resolve("焊接套管", "T10210019-03", "SY-A32-P-J100S:焊接套筒"));
    assertEquals("BOM-RM-SLEEVE", mapping.resolve("尼龙套管", "T10110001-01", "PET单根编织护套-EX601251501"));
  }

  @Test
  @DisplayName("包材按品名分 4 个子类")
  void packaging_splitByName() {
    assertEquals("BOM-PK-CARTON", mapping.resolve("包材", "T10510001-01", "纸箱 材质：BC/F ECT=8000N规格：685*415*385mm；16pcs/CTN"));
    assertEquals("BOM-PK-CARTON", mapping.resolve("包材", "T10510002-01", "纸盒 材质：350g白卡 规格：110*90*57mm"));
    assertEquals("BOM-PK-CUSHION", mapping.resolve("包材", "T10510001-02", "缓冲垫 材质：25kg/m3 白色防静电EPE"));
    assertEquals("BOM-PK-CUSHION", mapping.resolve("包材", "T10510002-03", "22kg/m3 白色EPE 规格：110*90*45mm"));
    assertEquals("BOM-PK-BAG", mapping.resolve("包材", "T10510001-04", "透明尼龙袋900*580mm"));
    assertEquals("BOM-PK-OTHER", mapping.resolve("包材", "T20040001", "纺织自卷管φ3mm-黑色"));
  }

  @Test
  @DisplayName("空类别、未知类别、TMX 的「未定」：一律回落待分类，不返回 null")
  void unknownOrBlank_fallsBackToTbd() {
    assertEquals("BOM-RM-TBD", mapping.resolve("未定", "T10210007-17", "JN-CD-P-J444T：护套"));
    assertEquals("BOM-RM-TBD", mapping.resolve("", "T10210008-01", "LNP60S-盖板"));
    assertEquals("BOM-RM-TBD", mapping.resolve(null, "T10210008-02", "LNP60S-铆钉"));
    assertEquals("BOM-RM-TBD", mapping.resolve("   ", "T10210016-08", "550S板端连接器：弹簧"));
    assertEquals("BOM-RM-TBD", mapping.resolve("将来新增的类别", "T99990001", "某种新物料"));
  }

  @Test
  @DisplayName("成品但料号为空：不当成连接器，回落待分类由人工看")
  void finishedGoods_withBlankPartNo_fallsBackToTbd() {
    assertEquals("BOM-RM-TBD", mapping.resolve("成品", "", "线缆组件"));
    assertEquals("BOM-RM-TBD", mapping.resolve("成品", null, "线缆组件"));
  }

  @Test
  @DisplayName("所有返回值都不是 null 且都以 BOM- 开头（防呆）")
  void alwaysReturnsKnownCode() {
    String[][] cases = {
      {"成品", "T10110001", "线缆组件"}, {"成品", "T10210003", "插座连接器"},
      {"PCB", "T1", "PCB"}, {"包材", "T10510001-01", "纸箱"}, {"未定", "T1", "护套"},
      {"", "T1", "x"}, {null, "T1", "x"},
    };
    for (String[] c : cases) {
      String code = mapping.resolve(c[0], c[1], c[2]);
      assertNotNull(code, "不得返回 null: " + String.join(",", c));
      assertTrue(code.startsWith("BOM-"), "必须返回已知类别编码: " + code);
    }
  }
}
