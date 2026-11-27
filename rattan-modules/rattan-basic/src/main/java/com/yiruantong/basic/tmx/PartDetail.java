package com.yiruantong.basic.tmx;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 单个物料详情（比 {@link PartBrief} 多项目三维与供应商字段）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PartDetail extends PartBrief {

  /** 客户代码（FT / HH / DJ-V200 / ZJ_DIAG…） */
  private String customerCode;

  /** 内部代码（BY-ML / SZ-MS / SY-P32 / PG-P7-JR…） */
  private String internalCode;

  /** 产品系列（Memory2 / MAP1 / LNP电源 / DIO256…） */
  private String productSeries;

  private String supplier;

  private String manufacturer;

  /** 厂商料号 */
  private String vendorPartNo;
}
