package com.yiruantong.job.snailjob.common;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 一轮箱规同步的报告（作业结果与例外清单的载体）。
 */
@Data
public class BoxSpecSyncReport {

  /** 扫描过的商品数 */
  private int scanned;

  /** 写入的箱规数（新增 + 更新） */
  private int specsWritten;

  /** 新增落库 */
  private int inserted;

  /** 更新已有 */
  private int updated;

  /** 无包装行跳过（不需要箱规，不是错误） */
  private int skippedNoPackaging;

  private final List<String> exceptions = new ArrayList<>();

  public String summary() {
    return "箱规同步完成：扫描" + scanned
      + " 写入" + specsWritten + "（新增" + inserted + " 更新" + updated + "）"
      + " 无包装行跳过" + skippedNoPackaging
      + " 例外" + exceptions.size() + " 条";
  }
}
