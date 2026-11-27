package com.yiruantong.basic.controller.product;

import com.yiruantong.basic.domain.product.BaseBoxSpec;
import com.yiruantong.basic.domain.product.bo.BaseBoxSpecBo;
import com.yiruantong.basic.domain.product.vo.BaseBoxSpecVo;
import com.yiruantong.basic.mapper.product.BaseBoxSpecMapper;
import com.yiruantong.basic.service.product.IBaseBoxSpecService;
import com.yiruantong.common.core.domain.R;
import com.yiruantong.common.web.core.AbstractController;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 商品箱规
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/basic/product/boxSpec")
public class BaseBoxSpecController extends AbstractController<BaseBoxSpecMapper, BaseBoxSpec, BaseBoxSpecVo, BaseBoxSpecBo> {

  private final IBaseBoxSpecService baseBoxSpecService;

  /**
   * 查询商品下全部箱规
   */
  @PostMapping("/listByProduct")
  public R<List<BaseBoxSpecVo>> listByProduct(@RequestBody Map<String, Object> map) {
    Long productId = map.get("productId") == null ? null : Long.valueOf(map.get("productId").toString());
    return R.ok(baseBoxSpecService.listByProduct(productId));
  }

}
