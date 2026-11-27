package com.yiruantong.inbound.service.in.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiruantong.inbound.domain.in.InBoxCode;
import com.yiruantong.inbound.domain.in.bo.InBoxScanBo;
import com.yiruantong.inbound.domain.in.vo.InBoxTreeVo;
import com.yiruantong.inbound.mapper.in.InBoxCodeMapper;
import com.yiruantong.inbound.service.in.IInBoxCodeService;
import com.yiruantong.common.mybatis.core.service.ServiceImplPlus;
import com.yiruantong.common.core.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 入库箱码Service业务层处理
 */
@RequiredArgsConstructor
@Service
public class InBoxCodeServiceImpl extends ServiceImplPlus<InBoxCodeMapper, InBoxCode, InBoxTreeVo, InBoxScanBo> implements IInBoxCodeService {

  @Override
  public InBoxCode getByCode(String boxCode) {
    if (StringUtils.isBlank(boxCode)) {
      return null;
    }
    return getOne(new LambdaQueryWrapper<InBoxCode>()
      .eq(InBoxCode::getBoxCode, boxCode.trim())
      .last("limit 1"));
  }

  @Override
  public List<InBoxTreeVo> getTree(String orderCode) {
    if (StringUtils.isBlank(orderCode)) {
      return List.of();
    }
    List<InBoxCode> outerList = list(new LambdaQueryWrapper<InBoxCode>()
      .eq(InBoxCode::getOrderCode, orderCode)
      .eq(InBoxCode::getBoxLevel, "OUTER")
      .ne(InBoxCode::getStatus, "VOID")
      .orderByAsc(InBoxCode::getBoxId));
    if (outerList.isEmpty()) {
      return List.of();
    }
    List<Long> outerIds = outerList.stream().map(InBoxCode::getBoxId).toList();
    List<InBoxCode> innerList = list(new LambdaQueryWrapper<InBoxCode>()
      .in(InBoxCode::getParentBoxId, outerIds)
      .eq(InBoxCode::getBoxLevel, "INNER")
      .ne(InBoxCode::getStatus, "VOID")
      .orderByAsc(InBoxCode::getBoxId));
    Map<Long, List<InBoxCode>> children = innerList.stream()
      .collect(Collectors.groupingBy(InBoxCode::getParentBoxId));

    List<InBoxTreeVo> result = new ArrayList<>();
    for (InBoxCode outer : outerList) {
      InBoxTreeVo outerVo = toVo(outer);
      List<InBoxCode> childrenList = children.getOrDefault(outer.getBoxId(), List.of());
      List<InBoxTreeVo> childVos = childrenList.stream().map(this::toVo).toList();
      outerVo.setInnerBoxList(childVos);
      outerVo.setInnerTotal(childVos.size());
      outerVo.setInnerReceived((int) childVos.stream()
        .filter(x -> "RECEIVED".equals(x.getStatus()) || "SHELVED".equals(x.getStatus()))
        .count());
      result.add(outerVo);
    }
    return result;
  }

  private InBoxTreeVo toVo(InBoxCode entity) {
    return BeanUtil.copyProperties(entity, InBoxTreeVo.class);
  }

  @Override
  public String nextCode(String prefix) {
    String realPrefix = StringUtils.isBlank(prefix) ? "IN-" : prefix.trim();
    return realPrefix + DateUtil.format(new Date(), "yyMMdd") + IdUtil.fastSimpleUUID().substring(0, 10).toUpperCase();
  }

}
