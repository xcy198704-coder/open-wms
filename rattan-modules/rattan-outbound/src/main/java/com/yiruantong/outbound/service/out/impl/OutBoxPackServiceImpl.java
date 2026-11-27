package com.yiruantong.outbound.service.out.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiruantong.basic.domain.product.BaseBoxSpec;
import com.yiruantong.basic.domain.product.BaseProduct;
import com.yiruantong.basic.service.product.IBaseBoxSpecService;
import com.yiruantong.basic.service.product.IBaseProductService;
import com.yiruantong.common.core.domain.R;
import com.yiruantong.common.core.enums.base.InventorySourceTypeEnum;
import com.yiruantong.common.core.exception.ServiceException;
import com.yiruantong.common.core.utils.StringUtils;
import com.yiruantong.common.satoken.utils.LoginHelper;
import com.yiruantong.outbound.domain.out.OutBoxCode;
import com.yiruantong.outbound.domain.out.OutOrder;
import com.yiruantong.outbound.domain.out.OutOrderDetail;
import com.yiruantong.outbound.domain.out.bo.OutBoxPackBo;
import com.yiruantong.outbound.domain.out.bo.OutScanDetailBo;
import com.yiruantong.outbound.domain.out.bo.OutScanMainBo;
import com.yiruantong.outbound.domain.out.vo.OutBoxTreeVo;
import com.yiruantong.outbound.liteflow.Context.OutScanContext;
import com.yiruantong.outbound.service.out.IOutBoxCodeService;
import com.yiruantong.outbound.service.out.IOutBoxPackService;
import com.yiruantong.outbound.service.out.IOutOrderDetailService;
import com.yiruantong.outbound.service.out.IOutOrderService;
import com.yiruantong.system.service.core.ISysConfigService;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 出库内外箱装箱Service业务层处理
 */
@RequiredArgsConstructor
@Service
public class OutBoxPackServiceImpl implements IOutBoxPackService {

  private final IOutBoxCodeService outBoxCodeService;
  private final IOutOrderService outOrderService;
  private final IOutOrderDetailService outOrderDetailService;
  private final IBaseProductService baseProductService;
  private final IBaseBoxSpecService baseBoxSpecService;
  private final ISysConfigService sysConfigService;
  private final FlowExecutor flowExecutor;

  @Override
  public List<OutBoxTreeVo> getTree(String orderCode) {
    return outBoxCodeService.getTree(orderCode);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OutBoxTreeVo scanOuter(OutBoxPackBo bo) {
    if (!sysConfigService.getConfigBool("out_boxLevelEnable")) {
      throw new ServiceException("内外箱出库功能未启用");
    }
    OutOrder order = loadOrder(bo.getOrderCode());
    String code = StringUtils.trim(bo.getBoxCode());
    if (StringUtils.isBlank(code)) {
      code = outBoxCodeService.nextCode(outerPrefix());
    }
    OutBoxCode outer = outBoxCodeService.getByCode(code);
    if (ObjectUtil.isNotNull(outer)) {
      checkOrder(outer, order.getOrderCode());
      if (!"OUTER".equals(outer.getBoxLevel())) {
        throw new ServiceException("该箱码是内箱码，请扫外箱码");
      }
      if ("VOID".equals(outer.getStatus()) || "SHIPPED".equals(outer.getStatus())) {
        throw new ServiceException("外箱【" + code + "】当前状态不可装箱");
      }
      outer.setStatus("PACKING");
      outer.setScanBy(LoginHelper.getUserId());
      outer.setScanByName(LoginHelper.getNickname());
      outer.setScanTime(new Date());
      outBoxCodeService.updateById(outer);
      return BeanUtil.copyProperties(outer, OutBoxTreeVo.class);
    }

    outer = new OutBoxCode();
    outer.setBoxCode(code);
    outer.setCodeType(code.startsWith(outerPrefix()) ? "UNIQUE" : "FIXED");
    outer.setBoxLevel("OUTER");
    outer.setSource("SYSTEM");
    outer.setOutOrderId(order.getOrderId());
    outer.setOutOrderCode(order.getOrderCode());
    outer.setOrderId(order.getOrderId());
    outer.setPlanQty(BigDecimal.ZERO);
    outer.setPackedQty(BigDecimal.ZERO);
    outer.setStatus("PACKING");
    outer.setScanBy(LoginHelper.getUserId());
    outer.setScanByName(LoginHelper.getNickname());
    outer.setScanTime(new Date());
    outBoxCodeService.save(outer);
    return BeanUtil.copyProperties(outer, OutBoxTreeVo.class);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OutBoxTreeVo scanInner(OutBoxPackBo bo) {
    if (!sysConfigService.getConfigBool("out_boxLevelEnable")) {
      throw new ServiceException("内外箱出库功能未启用");
    }
    OutOrder order = loadOrder(bo.getOrderCode());
    Assert.isFalse(ObjectUtil.isNull(bo.getOuterBoxId()), "请先扫描或选择外箱");
    OutBoxCode outer = outBoxCodeService.getById(bo.getOuterBoxId());
    Assert.isFalse(ObjectUtil.isNull(outer), "外箱不存在");
    checkOrder(outer, order.getOrderCode());
    if (!"PACKING".equals(outer.getStatus())) {
      throw new ServiceException("外箱当前状态不可装箱");
    }

    OutOrderDetail detail = outOrderDetailService.getById(bo.getOrderDetailId());
    Assert.isFalse(ObjectUtil.isNull(detail), "出库明细不存在");
    Assert.isFalse(!ObjectUtil.equal(detail.getOrderId(), order.getOrderId()), "出库明细不属于当前单据");
    BaseProduct product = baseProductService.getById(detail.getProductId());
    Assert.isFalse(ObjectUtil.isNull(product), "商品不存在");
    BaseBoxSpec spec = baseBoxSpecService.getDefaultSpec(product.getProductId());
    Assert.isFalse(ObjectUtil.isNull(spec), "商品【" + product.getProductCode() + "】未维护箱规");

    String code = StringUtils.trim(bo.getBoxCode());
    if (StringUtils.isBlank(code)) {
      code = outBoxCodeService.nextCode(innerPrefix());
    }
    BigDecimal qty = ObjectUtil.isNotNull(bo.getQty()) && bo.getQty().compareTo(BigDecimal.ZERO) > 0 ? bo.getQty() : spec.getInnerQty();

    OutBoxCode inner = outBoxCodeService.getByCode(code);
    if (ObjectUtil.isNotNull(inner)) {
      checkOrder(inner, order.getOrderCode());
      if (!"INNER".equals(inner.getBoxLevel())) {
        throw new ServiceException("该箱码是外箱码，请扫内箱码");
      }
      if ("PACKED".equals(inner.getStatus()) || "VERIFIED".equals(inner.getStatus()) || "SHIPPED".equals(inner.getStatus()) || "VOID".equals(inner.getStatus())) {
        throw new ServiceException("内箱【" + code + "】当前状态不可重复装箱");
      }
      if (ObjectUtil.isNotNull(inner.getParentBoxId()) && !ObjectUtil.equal(inner.getParentBoxId(), outer.getBoxId())) {
        throw new ServiceException("内箱【" + code + "】已装入外箱【" + inner.getParentBoxCode() + "】");
      }
      if (ObjectUtil.isNotNull(inner.getOrderDetailId()) && !ObjectUtil.equal(inner.getOrderDetailId(), detail.getOrderDetailId())) {
        throw new ServiceException("内箱【" + code + "】已绑定其他出库明细");
      }
    } else {
      inner = new OutBoxCode();
      inner.setBoxCode(code);
      inner.setCodeType(code.startsWith(innerPrefix()) ? "UNIQUE" : "FIXED");
      inner.setBoxLevel("INNER");
      inner.setSource("SYSTEM");
      inner.setOutOrderId(order.getOrderId());
      inner.setOutOrderCode(order.getOrderCode());
      inner.setOrderId(order.getOrderId());
    }
    inner.setParentBoxId(outer.getBoxId());
    inner.setParentBoxCode(outer.getBoxCode());
    inner.setOrderDetailId(detail.getOrderDetailId());
    inner.setProductId(product.getProductId());
    inner.setProductCode(product.getProductCode());
    inner.setProductName(product.getProductName());
    inner.setProductSpec(product.getProductSpec());
    inner.setInnerQty(spec.getInnerQty());
    inner.setOuterInnerCount(spec.getOuterInnerCount());
    inner.setPlanQty(qty);
    inner.setPackedQty(qty);
    inner.setStatus("PACKING");
    inner.setScanBy(LoginHelper.getUserId());
    inner.setScanByName(LoginHelper.getNickname());
    inner.setScanTime(new Date());
    outBoxCodeService.saveOrUpdate(inner);

    updateOuterSummary(outer.getBoxId());
    return BeanUtil.copyProperties(inner, OutBoxTreeVo.class);
  }

  private void updateOuterSummary(Long outerId) {
    OutBoxCode outer = outBoxCodeService.getById(outerId);
    if (ObjectUtil.isNull(outer)) {
      return;
    }
    List<OutBoxCode> children = children(outerId);
    outer.setPlanQty(children.stream().map(OutBoxCode::getPlanQty).filter(ObjectUtil::isNotNull).reduce(BigDecimal.ZERO, BigDecimal::add));
    outer.setPackedQty(children.stream().map(OutBoxCode::getPackedQty).filter(ObjectUtil::isNotNull).reduce(BigDecimal.ZERO, BigDecimal::add));
    outer.setOuterInnerCount(children.isEmpty() ? 1 : children.get(0).getOuterInnerCount());
    outBoxCodeService.updateById(outer);
  }

  private List<OutBoxCode> children(Long outerId) {
    return outBoxCodeService.list(new LambdaQueryWrapper<OutBoxCode>()
      .eq(OutBoxCode::getParentBoxId, outerId)
      .eq(OutBoxCode::getBoxLevel, "INNER")
      .ne(OutBoxCode::getStatus, "VOID")
      .orderByAsc(OutBoxCode::getBoxId));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void closeBox(Long boxId) {
    OutBoxCode outer = outBoxCodeService.getById(boxId);
    Assert.isFalse(ObjectUtil.isNull(outer), "外箱不存在");
    if (!"OUTER".equals(outer.getBoxLevel())) {
      throw new ServiceException("只能封外箱");
    }
    List<OutBoxCode> children = children(boxId);
    if (children.size() != outer.getOuterInnerCount()) {
      throw new ServiceException("内箱数量不足，应有" + outer.getOuterInnerCount() + "个，实际" + children.size() + "个");
    }
    if (outer.getPackedQty().compareTo(outer.getPlanQty()) != 0) {
      throw new ServiceException("装箱数量与计划数量不一致");
    }
    outer.setStatus("PACKED");
    outer.setCloseTime(new Date());
    outBoxCodeService.updateById(outer);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void verify(Long boxId) {
    OutBoxCode outer = outBoxCodeService.getById(boxId);
    Assert.isFalse(ObjectUtil.isNull(outer), "外箱不存在");
    if (!"OUTER".equals(outer.getBoxLevel())) {
      throw new ServiceException("只能核验外箱");
    }
    if (!("PACKED".equals(outer.getStatus()) || "VERIFIED".equals(outer.getStatus()))) {
      throw new ServiceException("外箱未封箱，不能核验");
    }
    outer.setStatus("VERIFIED");
    outBoxCodeService.updateById(outer);
    for (OutBoxCode child : children(boxId)) {
      child.setStatus("VERIFIED");
      outBoxCodeService.updateById(child);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public R<Void> submit(OutBoxPackBo bo) {
    if (!sysConfigService.getConfigBool("out_boxLevelEnable")) {
      throw new ServiceException("内外箱出库功能未启用");
    }
    OutOrder order = loadOrder(bo.getOrderCode());
    List<OutBoxCode> list = outBoxCodeService.list(new LambdaQueryWrapper<OutBoxCode>()
      .eq(OutBoxCode::getOutOrderCode, order.getOrderCode())
      .eq(OutBoxCode::getBoxLevel, "INNER")
      .in(OutBoxCode::getStatus, "PACKED", "VERIFIED")
      .isNull(OutBoxCode::getPackageDetailId)
      .orderByAsc(OutBoxCode::getBoxId));
    Assert.isFalse(list.isEmpty(), "没有可提交的已封箱内箱");

    List<OutScanDetailBo> dataList = new ArrayList<>();
    for (OutBoxCode box : list) {
      OutScanDetailBo detail = BeanUtil.copyProperties(box, OutScanDetailBo.class);
      detail.setOrderId(order.getOrderId());
      detail.setOrderCode(order.getOrderCode());
      detail.setOrderDetailId(box.getOrderDetailId());
      detail.setFinishedQuantity(box.getPackedQty());
      detail.setBoxId(box.getBoxId());
      detail.setOuterBoxId(box.getParentBoxId());
      dataList.add(detail);
    }

    OutScanMainBo scanBo = new OutScanMainBo();
    scanBo.setOrderId(order.getOrderId());
    scanBo.setOrderCode(order.getOrderCode());
    scanBo.setScanInType(InventorySourceTypeEnum.PC_ORDER_OUT);
    scanBo.setDataList(dataList);

    LiteflowResponse response = flowExecutor.execute2Resp("outBoxScanChain", scanBo, OutScanContext.class);
    Assert.isFalse(!response.isSuccess(), response.getMessage());
    return R.ok("内外箱装箱出库成功");
  }

  private OutOrder loadOrder(String orderCode) {
    Assert.isFalse(StringUtils.isBlank(orderCode), "出库单号不能为空");
    OutOrder order = outOrderService.getOne(new LambdaQueryWrapper<OutOrder>()
      .eq(OutOrder::getOrderCode, orderCode)
      .last("limit 1"));
    Assert.isFalse(ObjectUtil.isNull(order), "出库单不存在");
    return order;
  }

  private void checkOrder(OutBoxCode box, String orderCode) {
    if (StringUtils.isNotBlank(box.getOutOrderCode()) && !StringUtils.equals(box.getOutOrderCode(), orderCode)) {
      throw new ServiceException("箱码【" + box.getBoxCode() + "】已用于出库单【" + box.getOutOrderCode() + "】");
    }
  }

  private String outerPrefix() {
    String prefix = sysConfigService.selectConfigByKey("box_codeRuleOut");
    return (StringUtils.isBlank(prefix) ? "OUT-" : prefix) + "O-";
  }

  private String innerPrefix() {
    String prefix = sysConfigService.selectConfigByKey("box_codeRuleOut");
    return (StringUtils.isBlank(prefix) ? "OUT-" : prefix) + "I-";
  }

}
