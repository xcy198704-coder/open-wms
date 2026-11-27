package com.yiruantong.basic.tmx;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * BOM 一行（TMX 只读接口 DTO）。
 *
 * <p>每箱装数的推导原料：{@code qtyText} 是用量原文（如 {@code 1/16}），
 * {@code qtyScaled} 是 TMX 定点用量（×10⁴，{@code 1/16} → 625）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BomLineRow {

  private String childPartNo;

  /** 用量原文，如 "1/16"、"4"、"2PCS" */
  private String qtyText;

  /** 定点用量 ×10⁴；TMX 侧为 int64 */
  private Long qtyScaled;

  /** 物料类型：原料 / 包材 / 成品 / 客供… */
  private String materialType;

  private Boolean isVirtual;

  private Boolean isPurchased;

  private String unit;

  /** 明细行物料版本 */
  private String partVersion;

  /** 生效起始日（含），空 = 始终生效 */
  private String effectFrom;

  /** 失效日（含），空 = 不过期 */
  private String effectTo;
}
