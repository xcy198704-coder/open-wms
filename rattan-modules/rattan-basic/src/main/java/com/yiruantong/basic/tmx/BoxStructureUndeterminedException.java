package com.yiruantong.basic.tmx;

/**
 * 箱规结构推不定（Task 8）。
 *
 * <p>与 {@link BoxQtyUndeterminedException} 的分工：那个管「一行用量原文读不出
 * 装数」，这个管「多行包装料拼不出箱结构」——原因写进消息，作业捕获后把料号
 * 送进例外清单，**不落库、不猜**。
 */
public class BoxStructureUndeterminedException extends RuntimeException {

  public BoxStructureUndeterminedException(String message) {
    super(message);
  }
}
