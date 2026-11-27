package com.yiruantong.basic.tmx;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 每箱装数推导器（Task 7）：包装 BOM 行的用量原文 → 每箱装数。
 *
 * <p>口径：包装行用量原文形如 {@code 1/16}（1 个包装单位装 16 个产品），
 * 每箱装数 = 分母 N。原文保真来自 TMX（{@code qty_text}，服务端不改写），
 * 定点用量（{@code qty_scaled}，×10⁴）只作**交叉验证**：
 * 原文说 1/16 而定点值是 0.1，说明有一边数据坏了——抛异常让人核，
 * 不擅自选一边信。
 *
 * <p>禁静默失败：空值、非 {@code 1/N} 形态（整数用量「2」不是每箱装数口径）、
 * 分母非正数、定点矛盾，一律抛 {@link BoxQtyUndeterminedException}；
 * 调用方（箱规同步作业）捕获后把料号记入例外清单且**不落库**。
 */
public class BoxQtyDeriver {

  /** TMX 定点用量基数（×10⁴） */
  private static final long SCALE = 10_000;

  /** 定点交叉验证容差：TMX 对 1/36（277.78）存 278，±1 覆盖取整差异 */
  private static final long TOLERANCE = 1;

  /** 1/N 形态（容忍分子分母间的空白；前后空白由 strip 处理）。 */
  private static final Pattern FRACTION = Pattern.compile("^1\\s*/\\s*(\\d+)$");

  /**
   * 推导每箱装数。
   *
   * @param qtyText   包装行用量原文（如 {@code "1/16"}；整数 {@code "1"} 按 1/1 受理——
   *                   1pcs/BOX 内盒的用量原文就是「1」，见 T10210003 纸盒行）
   * @param qtyScaled TMX 定点用量（×10⁴；{@code <=0} 表示没给，只信原文）
   * @return 每箱装数 N（{@code 1/N} 的分母）
   * @throws BoxQtyUndeterminedException 推不出来（见类注释）
   */
  public int derive(String qtyText, long qtyScaled) {
    String text = qtyText == null ? "" : qtyText.strip();
    if (text.isEmpty()) {
      throw new BoxQtyUndeterminedException("包装行用量原文为空，无法推导每箱装数");
    }
    long n;
    if ("1".equals(text)) {
      // 整数 1 = 1/1（1 个包装单位装 1 个产品）
      n = 1;
    } else {
      Matcher m = FRACTION.matcher(text);
      if (!m.matches()) {
        throw new BoxQtyUndeterminedException(
          "用量原文「" + text + "」不是 1/N 形态，非每箱装数口径（整数用量/按箱等原文需人工确认）");
      }
      n = Long.parseLong(m.group(1));
    }
    if (n <= 0) {
      throw new BoxQtyUndeterminedException("用量原文「" + text + "」分母非正数");
    }
    if (n > Integer.MAX_VALUE) {
      throw new BoxQtyUndeterminedException("用量原文「" + text + "」分母超出 int 范围");
    }
    if (qtyScaled > 0) {
      long expected = Math.round((double) SCALE / n);
      if (Math.abs(qtyScaled - expected) > TOLERANCE) {
        throw new BoxQtyUndeterminedException(
          "用量原文「" + text + "」与定点用量 " + qtyScaled + " 矛盾（1/" + n + " 应对应 "
            + expected + " 左右），两侧数据待核");
      }
    }
    return (int) n;
  }
}
