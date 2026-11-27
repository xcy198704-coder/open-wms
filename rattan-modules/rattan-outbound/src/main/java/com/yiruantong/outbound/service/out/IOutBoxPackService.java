package com.yiruantong.outbound.service.out;

import com.yiruantong.common.core.domain.R;
import com.yiruantong.outbound.domain.out.bo.OutBoxPackBo;
import com.yiruantong.outbound.domain.out.vo.OutBoxTreeVo;

import java.util.List;

/**
 * 出库内外箱装箱Service接口
 */
public interface IOutBoxPackService {

  List<OutBoxTreeVo> getTree(String orderCode);

  OutBoxTreeVo scanOuter(OutBoxPackBo bo);

  OutBoxTreeVo scanInner(OutBoxPackBo bo);

  void closeBox(Long boxId);

  void verify(Long boxId);

  R<Void> submit(OutBoxPackBo bo);

}
