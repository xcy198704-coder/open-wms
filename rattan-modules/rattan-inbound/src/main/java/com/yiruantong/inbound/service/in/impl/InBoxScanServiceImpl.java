package com.yiruantong.inbound.service.in.impl;

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
import com.yiruantong.inbound.domain.in.InBoxCode;
import com.yiruantong.inbound.domain.in.InOrder;
import com.yiruantong.inbound.domain.in.InOrderDetail;
import com.yiruantong.inbound.domain.in.bo.InBoxScanBo;
import com.yiruantong.inbound.domain.in.bo.InScanOrderBo;
import com.yiruantong.inbound.domain.in.bo.InScanOrderDetailBo;
import com.yiruantong.inbound.domain.in.vo.InBoxTreeVo;
import com.yiruantong.inbound.liteflow.Context.InScanContext;
import com.yiruantong.inbound.service.in.IInBoxCodeService;
import com.yiruantong.inbound.service.in.IInBoxScanService;
import com.yiruantong.inbound.service.in.IInOrderDetailService;
import com.yiruantong.inbound.service.in.IInOrderService;
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
import java.util.Map;

/**
 * 内外箱扫描入库Service业务层处理
 */
@RequiredArgsConstructor
@Service
public class InBoxScanServiceImpl implements IInBoxScanService {

  private final IInBoxCodeService inBoxCodeService;
  private final IInOrderService inOrderService;
  private final IInOrderDetailService inOrderDetailService;
  private final IBaseProductService baseProductService;
  private final IBaseBoxSpecService baseBoxSpecService;
  private final ISysConfigService sysConfigService;
  private final FlowExecutor flowExecutor;

  @Override
  public List<InBoxTreeVo> getTree(String orderCode) {
    return inBoxCodeService.getTree(orderCode);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public InBoxTreeVo scan(InBoxScanBo bo) {
    if (!sysConfigService.getConfigBool("in_boxLevelEnable")) {
      throw new ServiceException("内外箱入库功能未启用");
    }
    InOrder order = loadOrder(bo.getOrderCode());
    String code = StringUtils.trim(bo.getBoxCode());
    if (StringUtils.isBlank(code)) {
      throw new ServiceException("箱码不能为空");
    }

    InBoxCode existing = inBoxCodeService.getByCode(code);
    if (ObjectUtil.isNotNull(existing)) {
      checkOrder(existing, order);
      if ("INNER".equals(existing.getBoxLevel())) {
        receiveInner(existing);
      } else if ("OUTER".equals(existing.getBoxLevel())) {
        receiveOuter(existing);
      } else {
        throw new ServiceException("箱码层级不正确");
      }
      return BeanUtil.copyProperties(existing, InBoxTreeVo.class);
    }

    InOrderDetail orderDetail = resolveOrderDetail(bo, order, code);
    BaseProduct product = baseProductService.getById(orderDetail.getProductId());
    Assert.isFalse(ObjectUtil.isNull(product), "商品不存在");
    BaseBoxSpec spec = baseBoxSpecService.getDefaultSpec(product.getProductId());
    Assert.isFalse(ObjectUtil.isNull(spec), "商品【" + product.getProductCode() + "】未维护箱规");
    if (!("INSTANCE".equals(spec.getBoxCodeMode()) || "BOTH".equals(spec.getBoxCodeMode()))) {
      throw new ServiceException("商品【" + product.getProductCode() + "】的箱码模式不支持内外箱扫描");
    }

    String prefix = sysConfigService.selectConfigByKey("box_codeRuleIn");
    if (StringUtils.isBlank(prefix)) {
      prefix = "IN-";
    }
    String innerPrefix = prefix + "I-";

    if (code.startsWith(innerPrefix)) {
      InBoxCode outer = createOuter(inBoxCodeService.nextCode(prefix + "O-"), order, orderDetail, product, spec, "UNIQUE");
      InBoxCode inner = createInner(code, outer, order, orderDetail, product, spec);
      receiveInner(inner);
      receiveOuterSummary(outer.getBoxId());
      return findVo(outer.getBoxId());
    }

    InBoxCode outer = createOuter(code, order, orderDetail, product, spec,
      isFixedProductCode(product, code) ? "FIXED" : "UNIQUE");
    receiveOuter(outer);
    return findVo(outer.getBoxId());
  }

  private boolean isFixedProductCode(BaseProduct product, String code) {
    return StringUtils.equals(product.getProductModel(), code)
      || StringUtils.equals(product.getMiddleBarcode(), code)
      || StringUtils.equals(product.getBigBarcode(), code)
      || StringUtils.equals(product.getRelationCode(), code)
      || StringUtils.equals(product.getRelationCode2(), code)
      || StringUtils.equals(product.getRelationCode3(), code)
      || StringUtils.equals(product.getRelationCode4(), code)
      || StringUtils.equals(product.getRelationCode5(), code);
  }

  private InOrder loadOrder(String orderCode) {
    Assert.isFalse(StringUtils.isBlank(orderCode), "预到货单号不能为空");
    InOrder order = inOrderService.getOne(new LambdaQueryWrapper<InOrder>()
      .eq(InOrder::getOrderCode, orderCode)
      .last("limit 1"));
    Assert.isFalse(ObjectUtil.isNull(order), "预到货单不存在");
    return order;
  }

  private void checkOrder(InBoxCode box, InOrder order) {
    if (StringUtils.isNotBlank(box.getOrderCode()) && !StringUtils.equals(box.getOrderCode(), order.getOrderCode())) {
      throw new ServiceException("箱码【" + box.getBoxCode() + "】已用于预到货单【" + box.getOrderCode() + "】");
    }
  }

  private InOrderDetail resolveOrderDetail(InBoxScanBo bo, InOrder order, String code) {
    if (ObjectUtil.isNotNull(bo.getOrderDetailId())) {
      InOrderDetail detail = inOrderDetailService.getById(bo.getOrderDetailId());
      Assert.isFalse(ObjectUtil.isNull(detail), "预到货明细不存在");
      Assert.isFalse(!ObjectUtil.equal(detail.getOrderId(), order.getOrderId()), "预到货明细不属于当前单据");
      return detail;
    }

    BaseProduct product = baseProductService.getOne(new LambdaQueryWrapper<BaseProduct>()
      .and(w -> w.eq(BaseProduct::getProductModel, code)
        .or().eq(BaseProduct::getMiddleBarcode, code)
        .or().eq(BaseProduct::getBigBarcode, code)
        .or().eq(BaseProduct::getRelationCode, code)
        .or().eq(BaseProduct::getRelationCode2, code)
        .or().eq(BaseProduct::getRelationCode3, code)
        .or().eq(BaseProduct::getRelationCode4, code)
        .or().eq(BaseProduct::getRelationCode5, code))
      .last("limit 1"));

    if (ObjectUtil.isNotNull(product)) {
      InOrderDetail detail = inOrderDetailService.getOne(new LambdaQueryWrapper<InOrderDetail>()
        .eq(InOrderDetail::getOrderId, order.getOrderId())
        .eq(InOrderDetail::getProductId, product.getProductId())
        .last("limit 1"));
      Assert.isFalse(ObjectUtil.isNull(detail), "该商品不在当前预到货单中");
      return detail;
    }

    throw new ServiceException("未识别的箱码，请选择预到货明细后重试");
  }

  private InBoxCode createOuter(String code, InOrder order, InOrderDetail detail, BaseProduct product,
                                BaseBoxSpec spec, String codeType) {
    InBoxCode outer = new InBoxCode();
    outer.setBoxCode(code);
    outer.setCodeType(codeType);
    outer.setBoxLevel("OUTER");
    outer.setSource("SYSTEM");
    outer.setOrderId(order.getOrderId());
    outer.setOrderCode(order.getOrderCode());
    outer.setOrderDetailId(detail.getOrderDetailId());
    outer.setProductId(product.getProductId());
    outer.setProductCode(product.getProductCode());
    outer.setProductName(product.getProductName());
    outer.setProductSpec(product.getProductSpec());
    outer.setInnerQty(spec.getInnerQty());
    outer.setOuterInnerCount(spec.getOuterInnerCount());
    outer.setPlanQty(spec.getInnerQty().multiply(BigDecimal.valueOf(spec.getOuterInnerCount())));
    outer.setScannedQty(BigDecimal.ZERO);
    outer.setStatus("CREATED");
    inBoxCodeService.save(outer);
    return outer;
  }

  private InBoxCode createInner(String code, InBoxCode outer, InOrder order, InOrderDetail detail,
                                BaseProduct product, BaseBoxSpec spec) {
    InBoxCode inner = new InBoxCode();
    inner.setBoxCode(code);
    inner.setCodeType("UNIQUE");
    inner.setBoxLevel("INNER");
    inner.setParentBoxId(outer.getBoxId());
    inner.setParentBoxCode(outer.getBoxCode());
    inner.setSource("SYSTEM");
    inner.setOrderId(order.getOrderId());
    inner.setOrderCode(order.getOrderCode());
    inner.setOrderDetailId(detail.getOrderDetailId());
    inner.setProductId(product.getProductId());
    inner.setProductCode(product.getProductCode());
    inner.setProductName(product.getProductName());
    inner.setProductSpec(product.getProductSpec());
    inner.setInnerQty(spec.getInnerQty());
    inner.setOuterInnerCount(spec.getOuterInnerCount());
    inner.setPlanQty(spec.getInnerQty());
    inner.setScannedQty(BigDecimal.ZERO);
    inner.setStatus("CREATED");
    inBoxCodeService.save(inner);
    return inner;
  }

  private void receiveOuter(InBoxCode outer) {
    List<InBoxCode> children = children(outer.getBoxId());
    if (children.size() < outer.getOuterInnerCount()) {
      String prefix = sysConfigService.selectConfigByKey("box_codeRuleIn");
      if (StringUtils.isBlank(prefix)) {
        prefix = "IN-";
      }
      for (int i = children.size(); i < outer.getOuterInnerCount(); i++) {
        InOrder order = inOrderService.getById(outer.getOrderId());
        InOrderDetail detail = inOrderDetailService.getById(outer.getOrderDetailId());
        BaseProduct product = baseProductService.getById(outer.getProductId());
        BaseBoxSpec spec = new BaseBoxSpec();
        spec.setInnerQty(outer.getInnerQty());
        spec.setOuterInnerCount(outer.getOuterInnerCount());
        createInner(inBoxCodeService.nextCode(prefix + "I-"), outer, order, detail, product, spec);
      }
    }
    for (InBoxCode child : children(outer.getBoxId())) {
      if ("CREATED".equals(child.getStatus()) || "PARTIAL".equals(child.getStatus())) {
        receiveInner(child);
      }
    }
    receiveOuterSummary(outer.getBoxId());
  }

  private void receiveInner(InBoxCode inner) {
    if ("RECEIVED".equals(inner.getStatus()) || "SHELVED".equals(inner.getStatus())) {
      throw new ServiceException("内箱【" + inner.getBoxCode() + "】已随外箱【" + inner.getParentBoxCode() + "】收货");
    }
    if ("VOID".equals(inner.getStatus())) {
      throw new ServiceException("内箱【" + inner.getBoxCode() + "】已作废");
    }
    inner.setScannedQty(inner.getInnerQty());
    inner.setStatus("RECEIVED");
    inner.setScanBy(LoginHelper.getUserId());
    inner.setScanByName(LoginHelper.getNickname());
    inner.setScanTime(new Date());
    inBoxCodeService.updateById(inner);
    if (ObjectUtil.isNotNull(inner.getParentBoxId())) {
      receiveOuterSummary(inner.getParentBoxId());
    }
  }

  private void receiveOuterSummary(Long outerId) {
    InBoxCode outer = inBoxCodeService.getById(outerId);
    if (ObjectUtil.isNull(outer)) {
      return;
    }
    List<InBoxCode> children = children(outerId);
    long received = children.stream().filter(x -> "RECEIVED".equals(x.getStatus()) || "SHELVED".equals(x.getStatus())).count();
    BigDecimal qty = children.stream()
      .filter(x -> "RECEIVED".equals(x.getStatus()) || "SHELVED".equals(x.getStatus()))
      .map(InBoxCode::getScannedQty)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    outer.setScannedQty(qty);
    outer.setStatus(received >= outer.getOuterInnerCount() ? "RECEIVED" : received > 0 ? "PARTIAL" : "CREATED");
    if ("RECEIVED".equals(outer.getStatus())) {
      outer.setCloseTime(new Date());
    }
    inBoxCodeService.updateById(outer);
  }

  private List<InBoxCode> children(Long outerId) {
    return inBoxCodeService.list(new LambdaQueryWrapper<InBoxCode>()
      .eq(InBoxCode::getParentBoxId, outerId)
      .eq(InBoxCode::getBoxLevel, "INNER")
      .ne(InBoxCode::getStatus, "VOID")
      .orderByAsc(InBoxCode::getBoxId));
  }

  private InBoxTreeVo findVo(Long boxId) {
    List<InBoxTreeVo> tree = inBoxCodeService.getTree(inBoxCodeService.getById(boxId).getOrderCode());
    return tree.stream()
      .filter(x -> ObjectUtil.equal(boxId, x.getBoxId()) || x.getInnerBoxList() != null && x.getInnerBoxList().stream().anyMatch(y -> ObjectUtil.equal(boxId, y.getBoxId())))
      .findFirst().orElse(null);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void close(Long boxId) {
    InBoxCode box = inBoxCodeService.getById(boxId);
    Assert.isFalse(ObjectUtil.isNull(box), "箱码不存在");
    if (!"OUTER".equals(box.getBoxLevel())) {
      throw new ServiceException("只能封外箱");
    }
    receiveOuter(box);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void cancel(Long boxId) {
    InBoxCode box = inBoxCodeService.getById(boxId);
    Assert.isFalse(ObjectUtil.isNull(box), "箱码不存在");
    if (ObjectUtil.isNotNull(box.getEnterDetailId())) {
      throw new ServiceException("已生成入库明细，不能直接撤销");
    }
    if ("OUTER".equals(box.getBoxLevel())) {
      for (InBoxCode child : children(boxId)) {
        child.setStatus("VOID");
        inBoxCodeService.updateById(child);
      }
    }
    box.setStatus("VOID");
    inBoxCodeService.updateById(box);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public R<Void> submit(InBoxScanBo bo) {
    if (!sysConfigService.getConfigBool("in_boxLevelEnable")) {
      throw new ServiceException("内外箱入库功能未启用");
    }
    InOrder order = loadOrder(bo.getOrderCode());
    List<InBoxCode> received = inBoxCodeService.list(new LambdaQueryWrapper<InBoxCode>()
      .eq(InBoxCode::getOrderCode, order.getOrderCode())
      .eq(InBoxCode::getBoxLevel, "INNER")
      .in(InBoxCode::getStatus, "RECEIVED", "SHELVED")
      .isNull(InBoxCode::getEnterDetailId)
      .orderByAsc(InBoxCode::getBoxId));
    Assert.isFalse(received.isEmpty(), "没有可提交的已收内箱");

    List<InScanOrderDetailBo> dataList = new ArrayList<>();
    for (InBoxCode box : received) {
      InScanOrderDetailBo detail = new InScanOrderDetailBo();
      detail.setOrderId(order.getOrderId());
      detail.setOrderDetailId(box.getOrderDetailId());
      detail.setProductId(box.getProductId());
      detail.setProductCode(box.getProductCode());
      detail.setProductName(box.getProductName());
      detail.setFinishedQuantity(box.getInnerQty());
      detail.setBoxId(box.getBoxId());
      detail.setPositionName(bo.getPositionName());
      InOrderDetail orderDetail = inOrderDetailService.getById(box.getOrderDetailId());
      detail.setProduceDate(ObjectUtil.isNotNull(bo.getProduceDate()) ? bo.getProduceDate() : orderDetail.getProduceDate());
      detail.setBatchNumber(StringUtils.isNotBlank(bo.getBatchNumber()) ? bo.getBatchNumber() : orderDetail.getBatchNumber());
      dataList.add(detail);
    }

    InScanOrderBo scanBo = new InScanOrderBo();
    scanBo.setOrderId(order.getOrderId());
    scanBo.setOrderCode(order.getOrderCode());
    scanBo.setOnShelve(Boolean.TRUE.equals(bo.getIsOnShelve()));
    scanBo.setScanInType(InventorySourceTypeEnum.PC_CASING_IN);
    scanBo.setDataList(dataList);

    LiteflowResponse response = flowExecutor.execute2Resp("inBoxScanChain", scanBo, InScanContext.class);
    Assert.isFalse(!response.isSuccess(), response.getMessage());

    String finalStatus = scanBo.isOnShelve() ? "SHELVED" : "RECEIVED";
    Map<Long, InBoxCode> outerMap = new java.util.HashMap<>();
    for (InBoxCode receivedBox : received) {
      InBoxCode current = inBoxCodeService.getById(receivedBox.getBoxId());
      if (ObjectUtil.isNull(current)) {
        continue;
      }
      current.setStatus(finalStatus);
      inBoxCodeService.updateById(current);
      if (ObjectUtil.isNotNull(current.getParentBoxId())) {
        InBoxCode outer = outerMap.computeIfAbsent(current.getParentBoxId(), id -> inBoxCodeService.getById(id));
        if (ObjectUtil.isNotNull(outer)) {
          outer.setEnterId(current.getEnterId());
          outer.setStatus(finalStatus);
        }
      }
    }
    for (InBoxCode outer : outerMap.values()) {
      if (ObjectUtil.isNotNull(outer)) {
        inBoxCodeService.updateById(outer);
      }
    }
    return R.ok("内外箱入库成功");
  }

}
