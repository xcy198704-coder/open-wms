package com.yiruantong.basic.service.product.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yiruantong.basic.domain.product.BaseBoxSpec;
import com.yiruantong.basic.domain.product.bo.BaseBoxSpecBo;
import com.yiruantong.basic.domain.product.vo.BaseBoxSpecVo;
import com.yiruantong.basic.mapper.product.BaseBoxSpecMapper;
import com.yiruantong.basic.service.product.IBaseBoxSpecService;
import com.yiruantong.common.core.exception.ServiceException;
import com.yiruantong.common.core.utils.StringUtils;
import com.yiruantong.common.mybatis.core.service.ServiceImplPlus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 商品箱规Service业务层处理
 */
@RequiredArgsConstructor
@Service
public class BaseBoxSpecServiceImpl extends ServiceImplPlus<BaseBoxSpecMapper, BaseBoxSpec, BaseBoxSpecVo, BaseBoxSpecBo> implements IBaseBoxSpecService {

  private static final Set<String> BOX_CODE_MODES = Set.of("NONE", "SKU", "INSTANCE", "BOTH");

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int insert(BaseBoxSpecBo bo) {
    validate(bo);
    if (ObjectUtil.isNotNull(bo.getSpecId()) && ObjectUtil.isNotNull(getById(bo.getSpecId()))) {
      throw new ServiceException("箱规ID已存在");
    }
    if (ObjectUtil.isNotNull(bo.getIsDefault()) && bo.getIsDefault() == 1) {
      clearDefault(bo.getProductId());
    }
    return super.insert(bo);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public BaseBoxSpecVo update(BaseBoxSpecBo bo) {
    validate(bo);
    if (ObjectUtil.isNull(bo.getSpecId())) {
      throw new ServiceException("箱规ID不能为空");
    }
    BaseBoxSpec old = getById(bo.getSpecId());
    if (ObjectUtil.isNull(old)) {
      throw new ServiceException("箱规不存在");
    }
    if (ObjectUtil.isNotNull(bo.getIsDefault()) && bo.getIsDefault() == 1) {
      clearDefault(bo.getProductId());
    }
    return super.update(bo);
  }

  private void validate(BaseBoxSpecBo bo) {
    if (ObjectUtil.isNull(bo.getProductId())) {
      throw new ServiceException("商品ID不能为空");
    }
    if (StringUtils.isBlank(bo.getSpecName())) {
      throw new ServiceException("规格名称不能为空");
    }
    if (ObjectUtil.isNull(bo.getInnerQty()) || bo.getInnerQty().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ServiceException("每内箱装数必须大于0");
    }
    if (ObjectUtil.isNull(bo.getOuterInnerCount()) || bo.getOuterInnerCount() <= 0) {
      throw new ServiceException("每外箱内箱数必须大于0");
    }
    if (!BOX_CODE_MODES.contains(bo.getBoxCodeMode())) {
      throw new ServiceException("箱码模式不正确");
    }
    if (bo.getIsDefault() == null) {
      bo.setIsDefault(0);
    }
    if (bo.getEnable() == null) {
      bo.setEnable(1);
    }
  }

  private void clearDefault(Long productId) {
    if (ObjectUtil.isNull(productId)) {
      return;
    }
    update(new LambdaUpdateWrapper<BaseBoxSpec>()
      .eq(BaseBoxSpec::getProductId, productId)
      .eq(BaseBoxSpec::getIsDefault, 1)
      .set(BaseBoxSpec::getIsDefault, 0));
  }

  @Override
  public BaseBoxSpec getDefaultSpec(Long productId) {
    if (ObjectUtil.isNull(productId)) {
      return null;
    }
    return getOne(new LambdaQueryWrapper<BaseBoxSpec>()
      .eq(BaseBoxSpec::getProductId, productId)
      .eq(BaseBoxSpec::getEnable, 1)
      .orderByDesc(BaseBoxSpec::getIsDefault)
      .last("limit 1"));
  }

  @Override
  public List<BaseBoxSpecVo> listByProduct(Long productId) {
    if (ObjectUtil.isNull(productId)) {
      return List.of();
    }
    List<BaseBoxSpec> list = list(new LambdaQueryWrapper<BaseBoxSpec>()
      .eq(BaseBoxSpec::getProductId, productId)
      .orderByDesc(BaseBoxSpec::getIsDefault)
      .orderByDesc(BaseBoxSpec::getSpecId));
    return list.stream().map(this::selectVo).toList();
  }

  private BaseBoxSpecVo selectVo(BaseBoxSpec entity) {
    return BeanUtil.copyProperties(entity, BaseBoxSpecVo.class);
  }

}
