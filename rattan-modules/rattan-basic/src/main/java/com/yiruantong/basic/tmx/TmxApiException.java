package com.yiruantong.basic.tmx;

import lombok.Getter;

/**
 * TMX 平台调用异常。
 *
 * <p>**禁止把失败吞成空结果**：HTTP 非 200、响应体非 JSON、TMX 返回 degraded（接口未接线）
 * 一律抛本异常，由调用方显式处理（告警/重试），不允许「同步了 0 条」这种静默成功。
 */
@Getter
public class TmxApiException extends RuntimeException {

  /** HTTP 状态码；本地解析失败时为 0 */
  private final int status;

  /** TMX 响应体原文（截断到 500 字符，避免把大段 HTML 塞进日志） */
  private final String responseBody;

  public TmxApiException(int status, String responseBody) {
    super("TMX 接口调用失败（status=" + status + "）：" + truncate(responseBody));
    this.status = status;
    this.responseBody = truncate(responseBody);
  }

  public TmxApiException(String message, Throwable cause) {
    super(message, cause);
    this.status = 0;
    this.responseBody = null;
  }

  private static String truncate(String body) {
    if (body == null) {
      return "<empty>";
    }
    String trimmed = body.strip();
    return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500) + "…";
  }
}
