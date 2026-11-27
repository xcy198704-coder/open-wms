package com.yiruantong.inbound.domain.in;

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
 * 入库箱码对象 in_box_code
 *
 * @author YiRuanTong
 * @date 2026-11-24
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "in_box_code", autoResultMap = true)
public class InBoxCode extends TenantEntity {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * 箱ID
   */
  @TableId(value = "box_id")
  private Long boxId;

  /**
   * 箱码
   */
  private String boxCode;

  /**
   * 箱码类型：FIXED 固定箱码，UNIQUE 唯一箱码
   */
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
   * 箱层级：OUTER 外箱，INNER 内箱
   */
  private String boxLevel;

  /**
   * 箱码来源：SYSTEM 系统生成，SUPPLIER 供应商
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
   * 入库单ID
   */
  private Long enterId;

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
  private String productCode;

  /**
   * 商品名称
   */
  private String productName;

  /**
   * 商品规格
   */
  private String productSpec;

  /**
   * 每内箱装数
   */
  private BigDecimal innerQty;

  /**
   * 每外箱内箱数
   */
  private Integer outerInnerCount;

  /**
   * 计划数量
   */
  private BigDecimal planQty;

  /**
   * 已收数量
   */
  private BigDecimal scannedQty;

  /**
   * 箱状态：CREATED，PARTIAL，RECEIVED，SHELVED，VOID
   */
  private String status;

  /**
   * 扫描人ID
   */
  private Long scanBy;

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
   * 扩展字段
   */
  @TableField(value = "expand_fields", typeHandler = JacksonTypeHandler.class)
  private Map<String, Object> expandFields;

  /**
   * 备注
   */
  private String remark;

  /**
   * 删除时间
   */
  private Date deleteTime;

}
