package com.yiruantong.outbound.service.out;

import com.yiruantong.common.mybatis.core.service.IServicePlus;
import com.yiruantong.outbound.domain.out.OutBoxCode;
import com.yiruantong.outbound.domain.out.bo.OutBoxPackBo;
import com.yiruantong.outbound.domain.out.vo.OutBoxTreeVo;

import java.util.List;

/**
 * 出库箱码Service接口
 */
public interface IOutBoxCodeService extends IServicePlus<OutBoxCode, OutBoxTreeVo, OutBoxPackBo> {

  OutBoxCode getByCode(String boxCode);

  List<OutBoxTreeVo> getTree(String orderCode);

  String nextCode(String prefix);

}
