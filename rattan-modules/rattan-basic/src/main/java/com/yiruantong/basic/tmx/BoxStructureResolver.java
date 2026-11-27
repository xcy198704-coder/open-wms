package com.yiruantong.basic.tmx;

import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * 箱规结构解析器（Task 8）：产品的包装 BOM 行 → 箱规（内外盒装数 + 模式 + 名称）。
 *
 * <p>**为什么需要子件名称**：实测 16 个有包装行的产品全是 4~8 条包材行——真箱
 * （纸盒/纸箱）与缓冲垫/EPE/尼龙袋混在一起，只靠用量分不出谁是谁（T10110001：
 * 纸箱 1/16 + 缓冲垫 1/4×2 + 尼龙袋 1/4；T10210003：纸盒 1 + 纸箱 1/36 +
 * EPE×2）。因此：
 * <ul>
 *   <li>**有子件名称**（TMX 侧补 {@code child_part_name} 后）：按名称关键词分类——
 *       盒/BOX=内盒，箱/CTN=外箱，EPE/缓冲/垫/袋=辅材（忽略）；规则与 11-25 人工
 *       导入的 5 条箱规逐字同形；</li>
 *   <li>**没有名称**（当前契约）：只有单条包装行时能确定（SKU）；多包装行一律抛
 *       {@link BoxStructureUndeterminedException}——不猜，猜错就是箱规错、
 *       每箱装数错；</li>
 *   <li>没有包装行：返回 {@code null}（该产品不需要箱规，不是错误）。</li>
 * </ul>
 *
 * <p>命名关键词来自实测命名（见 {@code BoxStructureResolverTest} 的夹具）；
 * 遇到不认识的名字抛异常让人扩关键词或补数据，不静默忽略。
 */
public class BoxStructureResolver {

  /** 包材的 materialType 字面量（TMX 侧口径） */
  private static final String MATERIAL_TYPE_PACKAGING = "包材";

  private static final String MODE_SKU = "SKU";
  private static final String MODE_BOTH = "BOTH";

  private final BoxQtyDeriver deriver = new BoxQtyDeriver();

  /**
   * 解析箱规结构。
   *
   * @param partNo   产品料号
   * @param bomLines 该产品的全部 BOM 行（含非包装行，内部过滤）
   * @return 箱规结构；没有包装行时返回 {@code null}
   * @throws BoxQtyUndeterminedException      某行用量读不出装数
   * @throws BoxStructureUndeterminedException 结构推不定（原因见消息）
   */
  public BoxStructure resolve(String partNo, List<BomLineRow> bomLines) {
    List<BomLineRow> packaging = bomLines.stream()
      .filter(l -> MATERIAL_TYPE_PACKAGING.equals(StrUtil.str(l.getMaterialType()).trim()))
      .toList();
    if (packaging.isEmpty()) {
      return null;
    }

    boolean anyNamed = packaging.stream()
      .anyMatch(l -> StrUtil.isNotBlank(l.getChildPartName()));
    if (!anyNamed) {
      // 无子件名称：只有单条包装行无歧义；多条无法区分内外盒与缓冲材
      if (packaging.size() == 1) {
        BomLineRow only = packaging.get(0);
        return sku(derive(only), only, null);
      }
      throw new BoxStructureUndeterminedException(
        partNo + " 有 " + packaging.size() + " 条包装行但无子件名称，无法区分内外盒与缓冲材"
          + "（待 TMX DTO 补 child_part_name，或人工确认）");
    }

    BomLineRow inner = null;
    BomLineRow outer = null;
    for (BomLineRow line : packaging) {
      String name = StrUtil.str(line.getChildPartName()).trim().toLowerCase();
      if (containsAny(name, "epe", "缓冲", "垫", "袋")) {
        continue; // 辅材：不是箱
      }
      if (containsAny(name, "盒", "box")) {
        if (inner != null) {
          throw new BoxStructureUndeterminedException(partNo + " 有多条内盒包装行，结构待人工确认");
        }
        inner = line;
        continue;
      }
      if (containsAny(name, "箱", "ctn", "carton")) {
        if (outer != null) {
          throw new BoxStructureUndeterminedException(partNo + " 有多条外箱包装行，结构待人工确认");
        }
        outer = line;
        continue;
      }
      throw new BoxStructureUndeterminedException(
        partNo + " 的包装行「" + name + "」不在已知命名里（盒/箱/EPE/缓冲/垫/袋），需人工确认或扩关键词");
    }
    if (outer == null) {
      throw new BoxStructureUndeterminedException(partNo + " 没有外箱包装行（只有内盒/辅材），结构待人工确认");
    }

    int outerCap = derive(outer);
    if (inner == null) {
      return sku(outerCap, outer, null);
    }
    int innerCap = derive(inner);
    if (innerCap <= 0 || outerCap % innerCap != 0) {
      throw new BoxStructureUndeterminedException(
        partNo + " 外箱装数 " + outerCap + " 不是内盒装数 " + innerCap + " 的整数倍，结构待人工确认");
    }
    return both(innerCap, outerCap / innerCap, inner, outer);
  }

  // ---------------------------------------------------------------- 构造

  private int derive(BomLineRow line) {
    long scaled = line.getQtyScaled() == null ? 0L : line.getQtyScaled();
    return deriver.derive(line.getQtyText(), scaled);
  }

  /** 无内盒：SKU 模式，每外箱装数 = 外箱容量。 */
  private BoxStructure sku(int outerCap, BomLineRow outer, BomLineRow inner) {
    BoxStructure s = new BoxStructure();
    s.setInnerQty(outerCap);
    s.setOuterInnerCount(1);
    s.setBoxCodeMode(MODE_SKU);
    s.setSpecName("外箱" + outerCap + "pcs/CTN（纸箱" + outer.getChildPartNo() + "）");
    s.setRemark("BOM包装用量原值 " + StrUtil.str(outer.getQtyText()).trim());
    return s;
  }

  /** 有内盒：BOTH 模式，每外箱内箱数 = 外箱容量 / 内盒容量。 */
  private BoxStructure both(int innerCap, int outerInnerCount, BomLineRow inner, BomLineRow outer) {
    BoxStructure s = new BoxStructure();
    s.setInnerQty(innerCap);
    s.setOuterInnerCount(outerInnerCount);
    s.setBoxCodeMode(MODE_BOTH);
    s.setSpecName("内盒" + innerCap + "pcs/BOX+外箱" + outerInnerCount + "盒/CTN（"
      + joinPartNos(inner.getChildPartNo(), outer.getChildPartNo()) + "）");
    s.setRemark("BOM包装用量原值 " + StrUtil.str(outer.getQtyText()).trim());
    return s;
  }

  /**
   * 内外盒料号并排：同前缀（只差最后一段）时按人工导入的简写式
   * {@code T10510002-01/02}，否则全量并列 {@code A/B}。
   */
  private static String joinPartNos(String inner, String outer) {
    int i = inner.lastIndexOf('-');
    int j = outer.lastIndexOf('-');
    if (i > 0 && j > 0 && inner.substring(0, i).equals(outer.substring(0, j))
      && !inner.substring(i + 1).contains("-") && !outer.substring(j + 1).contains("-")) {
      return inner.substring(0, i) + "-" + inner.substring(i + 1) + "/" + outer.substring(j + 1);
    }
    return inner + "/" + outer;
  }

  private static boolean containsAny(String text, String... keywords) {
    for (String kw : keywords) {
      if (text.contains(kw)) {
        return true;
      }
    }
    return false;
  }
}
