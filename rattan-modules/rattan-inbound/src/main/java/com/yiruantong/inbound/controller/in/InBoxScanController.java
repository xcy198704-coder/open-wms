package com.yiruantong.inbound.controller.in;

import com.yiruantong.common.core.domain.R;
import com.yiruantong.common.mybatis.core.dto.QueryBo;
import com.yiruantong.common.web.core.BaseController;
import com.yiruantong.inbound.domain.in.bo.InBoxScanBo;
import com.yiruantong.inbound.domain.in.vo.InBoxTreeVo;
import com.yiruantong.inbound.service.in.IInBoxScanService;
import com.yiruantong.inbound.service.in.IInBoxCodeService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 内外箱扫描入库
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/inbound/in/inBoxScan")
public class InBoxScanController extends BaseController {

  private final IInBoxScanService inBoxScanService;
  private final IInBoxCodeService inBoxCodeService;

  /**
   * 查询箱树
   */
  @PostMapping("/getTree")
  public R<List<InBoxTreeVo>> getTree(@RequestBody InBoxScanBo bo) {
    return R.ok(inBoxScanService.getTree(bo.getOrderCode()));
  }

  /**
   * 扫码
   */
  @PostMapping("/scan")
  public R<InBoxTreeVo> scan(@RequestBody InBoxScanBo bo) {
    return R.ok(inBoxScanService.scan(bo));
  }

  /**
   * 封箱
   */
  @PostMapping("/close")
  public R<Void> close(@RequestBody InBoxScanBo bo) {
    inBoxScanService.close(bo.getBoxId());
    return R.ok();
  }

  /**
   * 撤销
   */
  @PostMapping("/cancel")
  public R<Void> cancel(@RequestBody InBoxScanBo bo) {
    inBoxScanService.cancel(bo.getBoxId());
    return R.ok();
  }

  /**
   * 提交入库
   */
  @PostMapping("/submit")
  public R<Void> submit(@RequestBody InBoxScanBo bo) {
    return inBoxScanService.submit(bo);
  }

  /**
   * 外箱标签/装箱单打印数据
   */
  @PostMapping("/printOuter")
  public R<Map<String, Object>> printOuter(@RequestBody List<QueryBo> queryBoList) {
    Long boxId = parseBoxId(queryBoList);
    var box = inBoxCodeService.getById(boxId);
    Assert.isFalse(ObjectUtil.isNull(box), "箱码不存在");
    Assert.isFalse(!"OUTER".equals(box.getBoxLevel()), "只能打印外箱");
    InBoxTreeVo node = inBoxCodeService.getTree(box.getOrderCode()).stream()
      .filter(x -> ObjectUtil.equal(boxId, x.getBoxId()))
      .findFirst().orElse(null);
    Assert.isFalse(ObjectUtil.isNull(node), "箱码不存在");
    Map<String, Object> data = BeanUtil.beanToMap(node);
    java.util.List<Map<String, Object>> innerList = new java.util.ArrayList<>();
    for (int i = 0; i < node.getInnerBoxList().size(); i++) {
      Map<String, Object> inner = BeanUtil.beanToMap(node.getInnerBoxList().get(i));
      inner.put("index", i + 1);
      innerList.add(inner);
    }
    data.put("innerBoxList", innerList);
    return R.ok(customData(data));
  }

  /**
   * 内箱标签打印数据
   */
  @PostMapping("/printInner")
  public R<Map<String, Object>> printInner(@RequestBody List<QueryBo> queryBoList) {
    Long boxId = parseBoxId(queryBoList);
    var box = inBoxCodeService.getById(boxId);
    Assert.isFalse(ObjectUtil.isNull(box), "箱码不存在");
    Assert.isFalse(!"INNER".equals(box.getBoxLevel()), "只能打印内箱");
    return R.ok(customData(BeanUtil.beanToMap(box)));
  }

  private Long parseBoxId(List<QueryBo> queryBoList) {
    Assert.isFalse(queryBoList == null, "打印参数不能为空");
    return queryBoList.stream()
      .filter(x -> "boxId".equals(x.getColumn()))
      .map(QueryBo::getValues)
      .findFirst()
      .map(Long::valueOf)
      .orElseThrow(() -> new IllegalArgumentException("缺少boxId"));
  }

  private Map<String, Object> customData(Map<String, Object> dataInfo) {
    Map<String, Object> result = new java.util.HashMap<>();
    result.put("isCustomData", true);
    result.put("dataInfo", dataInfo);
    return result;
  }

}
