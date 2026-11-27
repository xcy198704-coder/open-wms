package com.yiruantong.basic.tmx;

import java.util.Map;

/**
 * TMX 物料类别 → WMS 商品分类（{@code base_product_type.type_code}）映射。
 *
 * <p>权威方向：WMS 的 3 大类 / 16 中类是 TMX {@code parts.material_category} 的**投影**，
 * 类别以 TMX 为准；本类是全网唯一一处映射规则。
 *
 * <p>规则：
 * <ul>
 *   <li>「成品」按料号前缀再分：{@code T1011} → 线缆组件，其余 → 连接器成品；</li>
 *   <li>「包材」按品名再分：纸箱/纸盒、缓冲材、袋类、其他；</li>
 *   <li>其余一对一；</li>
 *   <li>**空 / 未知 / TMX 的「未定」一律回落 {@code BOM-RM-TBD}（待分类）**——
 *       不返回 null、不猜类别；调用方按待分类告警催研发补类。</li>
 * </ul>
 *
 * <p>说明：映射先做在代码里（可测试、随发布走）。等现场出现「不想发版就要改映射」的
 * 真实诉求，再搬到 sys_config；现在搬等于把契约拆成两处，改一处忘一处。
 */
public class CategoryMapping {

  /** 待分类中类编码 */
  public static final String TBD = "BOM-RM-TBD";

  private static final Map<String, String> ONE_TO_ONE = Map.of(
    "PCB", "BOM-RM-PCB",
    "塑料件", "BOM-RM-PLASTIC",
    "端子", "BOM-RM-TERMINAL",
    "紧固件", "BOM-RM-FASTENER",
    "金属件其他", "BOM-RM-METAL",
    "电缆", "BOM-RM-CABLE",
    "POGO", "BOM-RM-POGO",
    "连接器", "BOM-RM-CONN",
    "焊接套管", "BOM-RM-SLEEVE",
    "尼龙套管", "BOM-RM-SLEEVE");

  /** 线缆组件料号前缀 */
  private static final String CABLE_PREFIX = "T1011";

  /**
   * 解析 WMS 中类编码。
   *
   * @param materialCategory TMX 的 material_category（可为空）
   * @param partNo           料号（成品分流用；为空时不猜，回落待分类）
   * @param name             品名（包材分流用）
   * @return {@code base_product_type.type_code}，永不为 null
   */
  public String resolve(String materialCategory, String partNo, String name) {
    String category = materialCategory == null ? "" : materialCategory.strip();
    if (category.isEmpty()) {
      return TBD;
    }
    if ("成品".equals(category)) {
      if (partNo == null || partNo.strip().isEmpty()) {
        // 料号缺失时不猜：既可能是线缆组件也可能是连接器成品，落待分类由人工看
        return TBD;
      }
      return isCableAssembly(partNo) ? "BOM-FG-CABLE" : "BOM-FG-CONN";
    }
    if ("包材".equals(category)) {
      return resolvePackaging(name);
    }
    return ONE_TO_ONE.getOrDefault(category, TBD);
  }

  private boolean isCableAssembly(String partNo) {
    return partNo != null && partNo.strip().startsWith(CABLE_PREFIX);
  }

  private String resolvePackaging(String name) {
    String text = name == null ? "" : name;
    if (text.contains("纸箱") || text.contains("纸盒")) {
      return "BOM-PK-CARTON";
    }
    if (text.contains("缓冲") || text.contains("EPE") || text.contains("气泡")) {
      return "BOM-PK-CUSHION";
    }
    if (text.contains("袋")) {
      return "BOM-PK-BAG";
    }
    return "BOM-PK-OTHER";
  }
}
