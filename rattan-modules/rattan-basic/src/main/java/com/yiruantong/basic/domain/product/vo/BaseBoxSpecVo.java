package com.yiruantong.basic.domain.product.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.yiruantong.basic.domain.product.BaseBoxSpec;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * 商品箱规视图对象 base_box_spec
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = BaseBoxSpec.class)
public class BaseBoxSpecVo implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @ExcelProperty(value = "箱规ID")
  private Long specId;

  @ExcelProperty(value = "商品ID")
  private Long productId;

  @ExcelProperty(value = "规格名称")
  private String specName;

  @ExcelProperty(value = "每内箱装数")
  private BigDecimal innerQty;

  @ExcelProperty(value = "每外箱内箱数")
  private Integer outerInnerCount;

  @ExcelProperty(value = "箱码模式")
  private String boxCodeMode;

  @ExcelProperty(value = "自印码规则")
  private String boxBarcodeRule;

  @ExcelProperty(value = "是否默认")
  private Integer isDefault;

  @ExcelProperty(value = "是否启用")
  private Integer enable;

  private String remark;

  private Map<String, Object> expandFields;

  private Long createBy;

  private String createByName;

  private Date createTime;

  private Long updateBy;

  private String updateByName;

  private Date updateTime;

}
