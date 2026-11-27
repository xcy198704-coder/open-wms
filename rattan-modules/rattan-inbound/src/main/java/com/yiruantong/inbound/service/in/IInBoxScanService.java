package com.yiruantong.inbound.service.in;

import com.yiruantong.common.core.domain.R;
import com.yiruantong.inbound.domain.in.bo.InBoxScanBo;
import com.yiruantong.inbound.domain.in.vo.InBoxTreeVo;

import java.util.List;

/**
 * 内外箱扫描入库Service接口
 */
public interface IInBoxScanService {

  /**
   * 查询箱树
   */
  List<InBoxTreeVo> getTree(String orderCode);

  /**
   * 扫码：外箱整箱收 / 内箱拆箱收 / 固定箱码自动建箱
   */
  InBoxTreeVo scan(InBoxScanBo bo);

  /**
   * 封箱
   */
  void close(Long boxId);

  /**
   * 撤销未提交的箱
   */
  void cancel(Long boxId);

  /**
   * 提交入库（每个已收内箱一条明细）
   */
  R<Void> submit(InBoxScanBo bo);

}
