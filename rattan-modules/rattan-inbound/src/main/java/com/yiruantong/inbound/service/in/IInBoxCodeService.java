package com.yiruantong.inbound.service.in;

import com.yiruantong.common.mybatis.core.service.IServicePlus;
import com.yiruantong.inbound.domain.in.InBoxCode;
import com.yiruantong.inbound.domain.in.bo.InBoxScanBo;
import com.yiruantong.inbound.domain.in.vo.InBoxTreeVo;

import java.util.List;

/**
 * 入库箱码Service接口
 */
public interface IInBoxCodeService extends IServicePlus<InBoxCode, InBoxTreeVo, InBoxScanBo> {

  /**
   * 按箱码查询
   */
  InBoxCode getByCode(String boxCode);

  /**
   * 查询预到货单的箱树
   */
  List<InBoxTreeVo> getTree(String orderCode);

  /**
   * 生成自印箱码
   */
  String nextCode(String prefix);

}
