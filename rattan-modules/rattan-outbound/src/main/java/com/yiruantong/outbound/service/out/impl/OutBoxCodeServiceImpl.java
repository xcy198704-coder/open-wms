package com.yiruantong.outbound.service.out.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiruantong.common.core.utils.StringUtils;
import com.yiruantong.common.mybatis.core.service.ServiceImplPlus;
import com.yiruantong.outbound.domain.out.OutBoxCode;
import com.yiruantong.outbound.domain.out.bo.OutBoxPackBo;
import com.yiruantong.outbound.domain.out.vo.OutBoxTreeVo;
import com.yiruantong.outbound.mapper.out.OutBoxCodeMapper;
import com.yiruantong.outbound.service.out.IOutBoxCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 出库箱码Service业务层处理
 */
@RequiredArgsConstructor
@Service
public class OutBoxCodeServiceImpl extends ServiceImplPlus<OutBoxCodeMapper, OutBoxCode, OutBoxTreeVo, OutBoxPackBo> implements IOutBoxCodeService {

  @Override
  public OutBoxCode getByCode(String boxCode) {
    if (StringUtils.isBlank(boxCode)) {
      return null;
    }
    return getOne(new LambdaQueryWrapper<OutBoxCode>()
      .eq(OutBoxCode::getBoxCode, boxCode.trim())
      .last("limit 1"));
  }

  @Override
  public List<OutBoxTreeVo> getTree(String orderCode) {
    if (StringUtils.isBlank(orderCode)) {
      return List.of();
    }
    List<OutBoxCode> outerList = list(new LambdaQueryWrapper<OutBoxCode>()
      .eq(OutBoxCode::getOutOrderCode, orderCode)
      .eq(OutBoxCode::getBoxLevel, "OUTER")
      .ne(OutBoxCode::getStatus, "VOID")
      .orderByAsc(OutBoxCode::getBoxId));
    if (outerList.isEmpty()) {
      return List.of();
    }
    List<Long> outerIds = outerList.stream().map(OutBoxCode::getBoxId).toList();
    List<OutBoxCode> innerList = list(new LambdaQueryWrapper<OutBoxCode>()
      .in(OutBoxCode::getParentBoxId, outerIds)
      .eq(OutBoxCode::getBoxLevel, "INNER")
      .ne(OutBoxCode::getStatus, "VOID")
      .orderByAsc(OutBoxCode::getBoxId));
    Map<Long, List<OutBoxCode>> children = innerList.stream()
      .collect(Collectors.groupingBy(OutBoxCode::getParentBoxId));

    List<OutBoxTreeVo> result = new ArrayList<>();
    for (OutBoxCode outer : outerList) {
      OutBoxTreeVo outerVo = toVo(outer);
      List<OutBoxTreeVo> childVos = children.getOrDefault(outer.getBoxId(), List.of()).stream().map(this::toVo).toList();
      outerVo.setInnerBoxList(childVos);
      outerVo.setInnerTotal(childVos.size());
      outerVo.setInnerPacked((int) childVos.stream()
        .filter(x -> "PACKING".equals(x.getStatus()) || "PACKED".equals(x.getStatus()) || "VERIFIED".equals(x.getStatus()) || "SHIPPED".equals(x.getStatus()))
        .count());
      result.add(outerVo);
    }
    return result;
  }

  private OutBoxTreeVo toVo(OutBoxCode entity) {
    return BeanUtil.copyProperties(entity, OutBoxTreeVo.class);
  }

  @Override
  public String nextCode(String prefix) {
    String realPrefix = StringUtils.isBlank(prefix) ? "OUT-" : prefix.trim();
    return realPrefix + DateUtil.format(new Date(), "yyMMdd") + IdUtil.fastSimpleUUID().substring(0, 10).toUpperCase();
  }

}
