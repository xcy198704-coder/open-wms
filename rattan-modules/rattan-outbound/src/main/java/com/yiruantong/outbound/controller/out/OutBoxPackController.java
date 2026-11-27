package com.yiruantong.outbound.controller.out;

import com.yiruantong.common.core.domain.R;
import com.yiruantong.common.mybatis.core.dto.QueryBo;
import com.yiruantong.common.web.core.BaseController;
import com.yiruantong.outbound.domain.out.bo.OutBoxPackBo;
import com.yiruantong.outbound.domain.out.vo.OutBoxTreeVo;
import com.yiruantong.outbound.service.out.IOutBoxPackService;
import com.yiruantong.outbound.service.out.IOutBoxCodeService;
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
 * 内外箱装箱出库
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/outbound/out/outBoxPack")
public class OutBoxPackController extends BaseController {

  private final IOutBoxPackService outBoxPackService;
  private final IOutBoxCodeService outBoxCodeService;

  @PostMapping("/getTree")
  public R<List<OutBoxTreeVo>> getTree(@RequestBody OutBoxPackBo bo) {
    return R.ok(outBoxPackService.getTree(bo.getOrderCode()));
  }

  @PostMapping("/scanOuter")
  public R<OutBoxTreeVo> scanOuter(@RequestBody OutBoxPackBo bo) {
    return R.ok(outBoxPackService.scanOuter(bo));
  }

  @PostMapping("/scanInner")
  public R<OutBoxTreeVo> scanInner(@RequestBody OutBoxPackBo bo) {
    return R.ok(outBoxPackService.scanInner(bo));
  }

  @PostMapping("/closeBox")
  public R<Void> closeBox(@RequestBody OutBoxPackBo bo) {
    outBoxPackService.closeBox(bo.getBoxId());
    return R.ok();
  }

  @PostMapping("/verify")
  public R<Void> verify(@RequestBody OutBoxPackBo bo) {
    outBoxPackService.verify(bo.getBoxId());
    return R.ok();
  }

  @PostMapping("/submit")
  public R<Void> submit(@RequestBody OutBoxPackBo bo) {
    return outBoxPackService.submit(bo);
  }

  /**
   * 外箱装箱单打印数据
   */
  @PostMapping("/printOuter")
  public R<Map<String, Object>> printOuter(@RequestBody List<QueryBo> queryBoList) {
    Long boxId = parseBoxId(queryBoList);
    var box = outBoxCodeService.getById(boxId);
    Assert.isFalse(ObjectUtil.isNull(box), "箱码不存在");
    Assert.isFalse(!"OUTER".equals(box.getBoxLevel()), "只能打印外箱");
    OutBoxTreeVo node = outBoxCodeService.getTree(box.getOutOrderCode()).stream()
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
   * 内箱清单打印数据
   */
  @PostMapping("/printInner")
  public R<Map<String, Object>> printInner(@RequestBody List<QueryBo> queryBoList) {
    Long boxId = parseBoxId(queryBoList);
    var box = outBoxCodeService.getById(boxId);
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
