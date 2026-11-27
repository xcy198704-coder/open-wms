package com.yiruantong.outbound.domain.out;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.yiruantong.common.mybatis.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * 出库箱码对象 out_box_code
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "out_box_code", autoResultMap = true)
public class OutBoxCode extends TenantEntity {

  @Serial
  private static final long serialVersionUID = 1L;

  @TableId(value = "box_id")
  private Long boxId;

  private String boxCode;

  /**
   * 码类型：FIXED/UNIQUE
   */
  private String codeType;

  private Long parentBoxId;

  private String parentBoxCode;

  /**
   * 箱层级：OUTER/INNER
   */
  private String boxLevel;

  /**
   * 箱码来源：SYSTEM/SUPPLIER
   */
  private String source;

  private Long outOrderId;

  private String outOrderCode;

  private Long orderId;

  private Long orderDetailId;

  private Long packageId;

  private Long packageDetailId;

  private Long productId;

  private String productCode;

  private String productName;

  private String productSpec;

  private BigDecimal innerQty;

  private Integer outerInnerCount;

  private BigDecimal planQty;

  private BigDecimal packedQty;

  /**
   * 状态：CREATED/PACKING/PACKED/VERIFIED/SHIPPED/VOID
   */
  private String status;

  private Long scanBy;

  private String scanByName;

  private Date scanTime;

  private Date closeTime;

  private String remark;

  @TableField(value = "expand_fields", typeHandler = JacksonTypeHandler.class)
  private Map<String, Object> expandFields;

}
