package com.yiruantong.inbound.liteflow.InScanCmp;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.yiruantong.common.core.exception.ServiceException;
import com.yiruantong.common.core.utils.StringUtils;
import com.yiruantong.inbound.domain.in.InBoxCode;
import com.yiruantong.inbound.domain.in.bo.InScanOrderBo;
import com.yiruantong.inbound.domain.in.bo.InScanOrderDetailBo;
import com.yiruantong.inbound.liteflow.Context.InScanContext;
import com.yiruantong.inbound.service.in.IInBoxCodeService;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 内外箱入库前校验
 */
@LiteflowComponent(id = "inScanBoxValidationCmp", name = "内外箱入库校验组件")
@RequiredArgsConstructor
public class InScanBoxValidationCmp extends NodeComponent {

  private final IInBoxCodeService inBoxCodeService;

  @Override
  public void process() {
    InScanContext ctx = this.getContextBean(InScanContext.class);
    InScanOrderBo scanBo = ctx.getInScanOrderBo();
    List<InScanOrderDetailBo> dataList = scanBo.getDataList();
    if (dataList == null) {
      return;
    }
    for (InScanOrderDetailBo detail : dataList) {
      if (ObjectUtil.isNull(detail.getBoxId())) {
        continue;
      }
      InBoxCode box = inBoxCodeService.getById(detail.getBoxId());
      Assert.isFalse(ObjectUtil.isNull(box), "箱码不存在");
      if (!"INNER".equals(box.getBoxLevel())) {
        throw new ServiceException("入库明细只能绑定内箱");
      }
      if (!StringUtils.equals(box.getOrderCode(), scanBo.getOrderCode())) {
        throw new ServiceException("内箱【" + box.getBoxCode() + "】不属于当前预到货单");
      }
      if (!("RECEIVED".equals(box.getStatus()) || "SHELVED".equals(box.getStatus()))) {
        throw new ServiceException("内箱【" + box.getBoxCode() + "】尚未收货");
      }
      if (ObjectUtil.isNotNull(box.getEnterDetailId())) {
        throw new ServiceException("内箱【" + box.getBoxCode() + "】已生成入库明细");
      }
    }
  }

}
