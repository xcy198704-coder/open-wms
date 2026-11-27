package com.yiruantong.basic.domain.product;

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
import java.util.Map;

/**
 * 商品箱规对象 base_box_spec
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "base_box_spec", autoResultMap = true)
public class BaseBoxSpec extends TenantEntity {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * 箱规ID
   */
  @TableId(value = "spec_id")
  private Long specId;

  /**
   * 商品ID
   */
  private Long productId;

  /**
   * 规格名称
   */
  private String specName;

  /**
   * 每内箱装数（小单位）
   */
  private BigDecimal innerQty;

  /**
   * 每外箱内箱数
   */
  private Integer outerInnerCount;

  /**
   * 箱码模式：NONE/SKU/INSTANCE/BOTH
   */
  private String boxCodeMode;

  /**
   * 自印码前缀或规则
   */
  private String boxBarcodeRule;

  /**
   * 是否默认
   */
  private Integer isDefault;

  /**
   * 是否启用
   */
  private Integer enable;

  /**
   * 备注
   */
  private String remark;

  /**
   * 扩展字段
   */
  @TableField(value = "expand_fields", typeHandler = JacksonTypeHandler.class)
  private Map<String, Object> expandFields;

}
