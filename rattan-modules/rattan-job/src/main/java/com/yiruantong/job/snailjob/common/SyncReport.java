package com.yiruantong.job.snailjob.common;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 一轮物料同步的报告（作业结果与例外清单的载体）。
 *
 * <p>例外分两级：{@code exceptions}（需要人处理的：虚拟件、有库存未停用、TMX 空详情…）
 * 与 {@code warnings}（自动回落但要知道的：类别待分类）。
 */
@Data
public class SyncReport {

  /** 翻页轮数 */
  private int pages;

  /** 新增落库 */
  private int inserted;

  /** 已存在按权威规则更新 */
  private int updated;

  /** 标记停用（productStatus=2，仅无库存时） */
  private int markedDisabled;

  /** 曾停用后恢复活跃（productStatus 2→1） */
  private int restoredActive;

  /** 虚拟件跳过（不建档案） */
  private int skippedVirtual;

  /** TMX 停用且 WMS 未建档，跳过 */
  private int skippedInactiveNew;

  /** TMX 停用但 WMS 有库存，未停用（待处理） */
  private int blockedByInventory;

  /** 新水位（服务端时钟留边际后）；未能确定则为空 */
  private String newWatermark;

  private final List<String> exceptions = new ArrayList<>();
  private final List<String> warnings = new ArrayList<>();

  /** 作业结果消息（snail-job 日志直接可见）。 */
  public String summary() {
    return "物料同步完成：页" + pages
      + " 新增" + inserted
      + " 更新" + updated
      + " 标记停用" + markedDisabled
      + " 恢复活跃" + restoredActive
      + " 虚拟件跳过" + skippedVirtual
      + " 停用未建档" + skippedInactiveNew
      + " 有库存未停用" + blockedByInventory
      + " 例外" + exceptions.size() + " 条"
      + " 水位→" + (newWatermark == null ? "（未变）" : newWatermark);
  }
}
