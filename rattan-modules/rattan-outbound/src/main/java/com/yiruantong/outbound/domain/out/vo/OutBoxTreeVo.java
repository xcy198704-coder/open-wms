package com.yiruantong.outbound.domain.out.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.yiruantong.outbound.domain.out.OutBoxCode;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 出库内外箱树视图对象 out_box_code
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = OutBoxCode.class)
public class OutBoxTreeVo implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private Long boxId;

  @ExcelProperty(value = "箱码")
  private String boxCode;

  private String codeType;

  private Long parentBoxId;

  private String parentBoxCode;

  @ExcelProperty(value = "箱层级")
  private String boxLevel;

  private String source;

  private Long outOrderId;

  private String outOrderCode;

  private Long orderDetailId;

  private Long packageDetailId;

  private Long productId;

  @ExcelProperty(value = "商品编号")
  private String productCode;

  @ExcelProperty(value = "商品名称")
  private String productName;

  private String productSpec;

  @ExcelProperty(value = "每内箱装数")
  private BigDecimal innerQty;

  @ExcelProperty(value = "每外箱内箱数")
  private Integer outerInnerCount;

  private BigDecimal planQty;

  @ExcelProperty(value = "已装箱数量")
  private BigDecimal packedQty;

  @ExcelProperty(value = "箱状态")
  private String status;

  private String scanByName;

  private Date scanTime;

  private Date closeTime;

  private String remark;

  private Map<String, Object> expandFields;

  private Integer innerTotal;

  private Integer innerPacked;

  private List<OutBoxTreeVo> innerBoxList;

}
