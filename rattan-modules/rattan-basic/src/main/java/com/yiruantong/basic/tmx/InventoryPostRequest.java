package com.yiruantong.basic.tmx;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 推送库存单据请求（WMS → TMX）。
 *
 * <p>幂等键 = {@code bizNo + partNo + direction}：TMX 侧按它拒绝重复过账。
 * 数量用字符串传递，避免 float 误差（与 TMX「金额/用量禁 float」同款纪律）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InventoryPostRequest {

  /** 业务单号（WMS 单据编号，幂等键之一） */
  private String bizNo;

  /** 业务类型：INBOUND_ENTER / OUTBOUND_PACKAGE / STOCKTAKING / TRANSFER… */
  private String bizType;

  private String partNo;

  /** 数量（十进制字符串） */
  private String qty;

  /** 方向：IN / OUT */
  private String direction;

  /** 箱码（WMS 差异化维度，映射规则见箱码口径确认项） */
  private String boxCode;

  /** 批次号 */
  private String batchNo;

  /** 业务发生时间原文（ISO-8601） */
  private String occurredAt;
}
