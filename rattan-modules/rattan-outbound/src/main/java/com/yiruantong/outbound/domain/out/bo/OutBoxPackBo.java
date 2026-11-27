package com.yiruantong.outbound.domain.out.bo;

import com.yiruantong.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 出库内外箱装箱业务对象
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OutBoxPackBo extends BaseEntity {

  private String orderCode;

  private String boxCode;

  private String productModel;

  private Long orderDetailId;

  private Long outerBoxId;

  private Long boxId;

  private BigDecimal qty;

  private String remark;

}
