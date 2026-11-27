package com.yiruantong.basic.service.product;

import com.yiruantong.basic.domain.product.BaseBoxSpec;
import com.yiruantong.basic.domain.product.bo.BaseBoxSpecBo;
import com.yiruantong.basic.domain.product.vo.BaseBoxSpecVo;
import com.yiruantong.common.mybatis.core.service.IServicePlus;

import java.util.List;

/**
 * 商品箱规Service接口
 */
public interface IBaseBoxSpecService extends IServicePlus<BaseBoxSpec, BaseBoxSpecVo, BaseBoxSpecBo> {

  /**
   * 取商品默认启用的箱规
   */
  BaseBoxSpec getDefaultSpec(Long productId);

  /**
   * 查询商品下全部启用箱规
   */
  List<BaseBoxSpecVo> listByProduct(Long productId);

}
