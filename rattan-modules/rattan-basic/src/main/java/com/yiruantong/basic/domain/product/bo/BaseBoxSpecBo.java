package com.yiruantong.basic.domain.product.bo;

import com.yiruantong.basic.domain.product.BaseBoxSpec;
import com.yiruantong.common.core.validate.AddGroup;
import com.yiruantong.common.core.validate.EditGroup;
import com.yiruantong.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 商品箱规业务对象 base_box_spec
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = BaseBoxSpec.class, reverseConvertGenerate = false)
public class BaseBoxSpecBo extends BaseEntity {

  private Long specId;

  @NotNull(message = "商品ID不能为空", groups = {AddGroup.class, EditGroup.class})
  private Long productId;

  @NotBlank(message = "规格名称不能为空", groups = {AddGroup.class, EditGroup.class})
  private String specName;

  @NotNull(message = "每内箱装数不能为空", groups = {AddGroup.class, EditGroup.class})
  private BigDecimal innerQty;

  @NotNull(message = "每外箱内箱数不能为空", groups = {AddGroup.class, EditGroup.class})
  @Min(value = 1, message = "每外箱内箱数至少为1", groups = {AddGroup.class, EditGroup.class})
  @Max(value = 999, message = "每外箱内箱数不能超过999", groups = {AddGroup.class, EditGroup.class})
  private Integer outerInnerCount;

  @NotBlank(message = "箱码模式不能为空", groups = {AddGroup.class, EditGroup.class})
  private String boxCodeMode;

  private String boxBarcodeRule;

  private Integer isDefault;

  private Integer enable;

  private String remark;

  private Map<String, Object> expandFields;

}
