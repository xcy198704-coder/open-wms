package com.yiruantong.outbound.liteflow.OutScanCmp;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.yiruantong.common.core.exception.ServiceException;
import com.yiruantong.common.core.utils.StringUtils;
import com.yiruantong.outbound.domain.out.OutBoxCode;
import com.yiruantong.outbound.domain.out.bo.OutScanDetailBo;
import com.yiruantong.outbound.domain.out.bo.OutScanMainBo;
import com.yiruantong.outbound.liteflow.Context.OutScanContext;
import com.yiruantong.outbound.service.out.IOutBoxCodeService;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 内外箱出库前校验
 */
@LiteflowComponent(id = "outScanBoxValidationCmp", name = "内外箱出库校验组件")
@RequiredArgsConstructor
public class OutScanBoxValidationCmp extends NodeComponent {

  private final IOutBoxCodeService outBoxCodeService;

  @Override
  public void process() {
    OutScanContext ctx = this.getContextBean(OutScanContext.class);
    OutScanMainBo scanBo = ctx.getOutScanMainBo();
    List<OutScanDetailBo> dataList = scanBo.getDataList();
    if (dataList == null) {
      return;
    }
    for (OutScanDetailBo detail : dataList) {
      if (ObjectUtil.isNull(detail.getBoxId())) {
        continue;
      }
      OutBoxCode box = outBoxCodeService.getById(detail.getBoxId());
      Assert.isFalse(ObjectUtil.isNull(box), "箱码不存在");
      if (!"INNER".equals(box.getBoxLevel())) {
        throw new ServiceException("出库明细只能绑定内箱");
      }
      if (!StringUtils.equals(box.getOutOrderCode(), scanBo.getOrderCode())) {
        throw new ServiceException("内箱【" + box.getBoxCode() + "】不属于当前出库单");
      }
      if (!("PACKED".equals(box.getStatus()) || "VERIFIED".equals(box.getStatus()))) {
        throw new ServiceException("内箱【" + box.getBoxCode() + "】尚未封箱");
      }
      if (ObjectUtil.isNotNull(box.getPackageDetailId())) {
        throw new ServiceException("内箱【" + box.getBoxCode() + "】已生成打包明细");
      }
    }
  }

}
