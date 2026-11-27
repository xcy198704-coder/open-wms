package com.yiruantong.job.snailjob.common;

import cn.hutool.core.util.StrUtil;
import com.yiruantong.basic.domain.product.BaseBoxSpec;
import com.yiruantong.basic.domain.product.BaseProduct;
import com.yiruantong.basic.service.product.IBaseBoxSpecService;
import com.yiruantong.basic.service.product.IBaseProductService;
import com.yiruantong.basic.tmx.BomLineRow;
import com.yiruantong.basic.tmx.BoxStructure;
import com.yiruantong.basic.tmx.BoxStructureResolver;
import com.yiruantong.basic.tmx.TmxClient;
import com.yiruantong.basic.tmx.TmxIntegrationConfig;
import com.yiruantong.common.core.constant.TenantConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 箱规自动生成编排（Task 8）：商品的包装 BOM 行 → base_box_spec 幂等 upsert。
 *
 * <p>口径：
 * <ul>
 *   <li>逐商品取 BOM 行 → {@link BoxStructureResolver} 解箱结构（有子件名称才分
 *       内外盒；无名称多包装行一律进例外清单，不猜）；</li>
 *   <li>按 {@code product_id + spec_name} 幂等 upsert——与 11-25 人工导入的命名
 *       逐字同形，已存在则刷新数值而不是插第二条；</li>
 *   <li>推导失败（用量读不出/结构推不定）→ 例外清单 + **不落库**；</li>
 *   <li>取数失败（网络/鉴权/degraded）→ 上抛让作业显式失败（禁静默跳过一个商品）。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class BoxSpecSyncService {

  /** 新增箱规的创建人标记（与主数据同步、人工导入一致） */
  private static final String SYNC_CREATOR = "系统导入";

  private final TmxIntegrationConfig config;
  private final IBaseProductService productService;
  private final IBaseBoxSpecService boxSpecService;

  private final BoxStructureResolver resolver = new BoxStructureResolver();

  public BoxSpecSyncReport sync() {
    TmxClient client = config.buildClient();
    BoxSpecSyncReport report = new BoxSpecSyncReport();

    List<BaseProduct> products = productService.lambdaQuery().list();
    for (BaseProduct product : products) {
      report.setScanned(report.getScanned() + 1);
      String partNo = product.getProductCode();
      if (StrUtil.isBlank(partNo)) {
        continue;
      }
      // 取数失败直接上抛：作业失败重来，不吃掉异常当成「这个商品没箱规」
      List<BomLineRow> lines = client.fetchBomLines(partNo);
      BoxStructure structure;
      try {
        structure = resolver.resolve(partNo, lines);
      } catch (RuntimeException e) {
        // 推导失败：记例外、不落库、继续下一个商品
        report.getExceptions().add(partNo + "：" + e.getMessage());
        continue;
      }
      if (structure == null) {
        report.setSkippedNoPackaging(report.getSkippedNoPackaging() + 1);
        continue;
      }
      upsert(product.getProductId(), structure, report);
    }
    return report;
  }

  // ---------------------------------------------------------------- upsert

  private void upsert(Long productId, BoxStructure s, BoxSpecSyncReport report) {
    BaseBoxSpec existing = boxSpecService.lambdaQuery()
      .eq(BaseBoxSpec::getProductId, productId)
      .eq(BaseBoxSpec::getSpecName, s.getSpecName())
      .one();

    if (existing == null) {
      BaseBoxSpec spec = new BaseBoxSpec();
      apply(spec, s);
      spec.setProductId(productId);
      spec.setTenantId(TenantConstants.DEFAULT_TENANT_ID);
      spec.setCreateByName(SYNC_CREATOR);
      if (!boxSpecService.save(spec)) {
        throw new IllegalStateException("箱规保存失败：productId=" + productId);
      }
      report.setInserted(report.getInserted() + 1);
    } else {
      apply(existing, s);
      if (!boxSpecService.updateById(existing)) {
        throw new IllegalStateException("箱规更新失败：specId=" + existing.getSpecId());
      }
      report.setUpdated(report.getUpdated() + 1);
    }
    report.setSpecsWritten(report.getSpecsWritten() + 1);
  }

  /** 只写推导出的字段；不碰箱码规则、扩展字段等本地维护列。 */
  private void apply(BaseBoxSpec spec, BoxStructure s) {
    spec.setSpecName(s.getSpecName());
    spec.setInnerQty(BigDecimal.valueOf(s.getInnerQty()));
    spec.setOuterInnerCount(s.getOuterInnerCount());
    spec.setBoxCodeMode(s.getBoxCodeMode());
    spec.setRemark(s.getRemark());
    spec.setIsDefault(1);
    spec.setEnable(1);
  }
}
