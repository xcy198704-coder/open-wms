package com.yiruantong.basic.tmx;

/**
 * 每箱装数推不出来（Task 7）。
 *
 * <p>**这是特性不是故障**：包装行的用量原文不是 {@code 1/N} 形态、或与定点用量
 * 矛盾时，每箱装数就是不确定——按 0 或按猜落库会让箱规静默出错
 * （Review Focus #3：包装用量为分数或空值必须显式报错并跳过该行，
 * 不得按 0 装数落库）。调用方捕获后把料号送进例外清单。
 */
public class BoxQtyUndeterminedException extends RuntimeException {

  public BoxQtyUndeterminedException(String message) {
    super(message);
  }
}
