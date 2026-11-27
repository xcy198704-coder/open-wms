package com.yiruantong.basic.tmx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 每箱装数推导器测试（Task 7）。
 *
 * <p>口径：包装行的用量原文形如 {@code 1/16}（1 个包装单位装 16 个产品），
 * 每箱装数 = 分母。**推不出来就抛异常**——按 0 装数落库会让箱规静默出错
 * （禁静默失败，Review Focus #3）。
 */
class BoxQtyDeriverTest {

  private final BoxQtyDeriver deriver = new BoxQtyDeriver();

  @Test
  @DisplayName("1/N 形态：1/16→16、1/36→36")
  void fractionalForms() {
    assertEquals(16, deriver.derive("1/16", 625));
    assertEquals(36, deriver.derive("1/36", 278));
  }

  @Test
  @DisplayName("定点换算交叉验证：1/16 的 QtyScaled 应为 625（0.0625×10⁴），容差 ±1")
  void scaledCrossCheck() {
    // 与原文一致的定点值（TMX 四舍五入）——通过
    assertEquals(16, deriver.derive("1/16", 625));
    assertEquals(36, deriver.derive("1/36", 278));
    // 与原文矛盾（1/16 却带 0.1 的定点值）——数据坏了，抛
    BoxQtyUndeterminedException ex = assertThrows(BoxQtyUndeterminedException.class,
      () -> deriver.derive("1/16", 1000));
    assertTrue(ex.getMessage().contains("1/16"));
  }

  @Test
  @DisplayName("空值/零定点：抛异常，不按 0 装数猜")
  void emptyOrZeroThrows() {
    assertThrows(BoxQtyUndeterminedException.class, () -> deriver.derive("", 0));
    assertThrows(BoxQtyUndeterminedException.class, () -> deriver.derive(null, 0));
    assertThrows(BoxQtyUndeterminedException.class, () -> deriver.derive("   ", 0));
  }

  @Test
  @DisplayName("非 1/N 形态（整数用量、2/16、N/1 等）：抛异常并说明口径")
  void nonFractionalFormsThrow() {
    // 整数用量「2」不是每箱装数口径
    BoxQtyUndeterminedException e1 = assertThrows(BoxQtyUndeterminedException.class,
      () -> deriver.derive("2", 20000));
    assertTrue(e1.getMessage().contains("每箱装数"), e1.getMessage());

    // 分子不是 1
    assertThrows(BoxQtyUndeterminedException.class, () -> deriver.derive("2/16", 1250));
    // 分母不是正整数
    assertThrows(BoxQtyUndeterminedException.class, () -> deriver.derive("1/0", 0));
    assertThrows(BoxQtyUndeterminedException.class, () -> deriver.derive("1/abc", 0));
    // 裸文字
    assertThrows(BoxQtyUndeterminedException.class, () -> deriver.derive("按箱", 0));
  }

  @Test
  @DisplayName("容错：前后空白与分子分母间空白可剥；1/1 与整数 1 都受理（1pcs/BOX 原文就是「1」）")
  void tolerantWhitespace() {
    assertEquals(16, deriver.derive("  1/16 ", 625));
    assertEquals(16, deriver.derive("1 / 16", 625));
    assertEquals(1, deriver.derive("1/1", 10000));
    assertEquals(1, deriver.derive("1", 10000));
    // 整数 2 不是每箱装数口径（2 个包装单位装 1 个产品没有意义）
    assertThrows(BoxQtyUndeterminedException.class, () -> deriver.derive("2", 20000));
  }
}
