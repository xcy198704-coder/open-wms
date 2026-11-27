package com.yiruantong.inbound.domain.in.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.yiruantong.inbound.domain.in.InBoxCode;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 入库内外箱树视图对象 in_box_code
 *
 * @author YiRuanTong
 * @date 2026-11-24
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = InBoxCode.class)
public class InBoxTreeVo implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * 箱ID
   */
  @ExcelProperty(value = "箱ID")
  private Long boxId;

  /**
   * 箱码
   */
  @ExcelProperty(value = "箱码")
  private String boxCode;

  /**
   * 箱码类型
   */
  @ExcelProperty(value = "箱码类型")
  private String codeType;

  /**
   * 外箱ID
   */
  private Long parentBoxId;

  /**
   * 外箱码
   */
  private String parentBoxCode;

  /**
   * 箱层级
   */
  @ExcelProperty(value = "箱层级")
  private String boxLevel;

  /**
   * 箱码来源
   */
  private String source;

  /**
   * 预到货单ID
   */
  private Long orderId;

  /**
   * 预到货单号
   */
  private String orderCode;

  /**
   * 预到货明细ID
   */
  private Long orderDetailId;

  /**
   * 入库单明细ID
   */
  private Long enterDetailId;

  /**
   * 上架单明细ID
   */
  private Long shelveDetailId;

  /**
   * 商品ID
   */
  private Long productId;

  /**
   * 商品编号
   */
  @ExcelProperty(value = "商品编号")
  private String productCode;

  /**
   * 商品名称
   */
  @ExcelProperty(value = "商品名称")
  private String productName;

  /**
   * 商品规格
   */
  private String productSpec;

  /**
   * 每内箱装数
   */
  @ExcelProperty(value = "每内箱装数")
  private BigDecimal innerQty;

  /**
   * 每外箱内箱数
   */
  @ExcelProperty(value = "每外箱内箱数")
  private Integer outerInnerCount;

  /**
   * 计划数量
   */
  private BigDecimal planQty;

  /**
   * 已收数量
   */
  @ExcelProperty(value = "已收数量")
  private BigDecimal scannedQty;

  /**
   * 箱状态
   */
  @ExcelProperty(value = "箱状态")
  private String status;

  /**
   * 扫描人
   */
  private String scanByName;

  /**
   * 扫描时间
   */
  private Date scanTime;

  /**
   * 封箱时间
   */
  private Date closeTime;

  /**
   * 备注
   */
  private String remark;

  /**
   * 扩展字段
   */
  private Map<String, Object> expandFields;

  /**
   * 内箱已收数，外箱节点统计用
   */
  private Integer innerTotal;

  /**
   * 内箱已收货数，外箱节点统计用
   */
  private Integer innerReceived;

  /**
   * 内箱集合，仅外箱节点填充
   */
  private List<InBoxTreeVo> innerBoxList;

}
