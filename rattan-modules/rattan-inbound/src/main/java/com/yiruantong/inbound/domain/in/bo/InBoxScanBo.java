package com.yiruantong.inbound.domain.in.bo;

import com.yiruantong.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 内外箱扫描业务对象
 *
 * @author YiRuanTong
 * @date 2026-11-24
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InBoxScanBo extends BaseEntity {

  /**
   * 预到货单号
   */
  private String orderCode;

  /**
   * 箱码（外箱码或内箱码）
   */
  private String boxCode;

  /**
   * 预到货明细ID
   */
  private Long orderDetailId;

  /**
   * 箱ID
   */
  private Long boxId;

  /**
   * 上架货位
   */
  private String positionName;

  /**
   * 是否直接上架
   */
  private Boolean isOnShelve;

  /**
   * 备注
   */
  private String remark;

  /**
   * 生产日期
   */
  private Date produceDate;

  /**
   * 批次号
   */
  private String batchNumber;

}
