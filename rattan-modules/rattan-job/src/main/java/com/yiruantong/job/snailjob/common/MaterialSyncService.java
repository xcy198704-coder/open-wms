package com.yiruantong.job.snailjob.common;

import cn.hutool.core.util.StrUtil;
import com.yiruantong.basic.domain.product.BaseProduct;
import com.yiruantong.basic.domain.product.BaseProductType;
import com.yiruantong.basic.domain.product.bo.BaseProductBo;
import com.yiruantong.basic.service.product.IBaseProductService;
import com.yiruantong.basic.service.product.IBaseProductTypeService;
import com.yiruantong.basic.tmx.MaterialMapper;
import com.yiruantong.basic.tmx.MaterialMapping;
import com.yiruantong.basic.tmx.PartBrief;
import com.yiruantong.basic.tmx.PartDetail;
import com.yiruantong.basic.tmx.PartsPage;
import com.yiruantong.basic.tmx.TmxClient;
import com.yiruantong.basic.tmx.TmxIntegrationConfig;
import com.yiruantong.common.core.constant.TenantConstants;
import com.yiruantong.inventory.domain.core.CoreInventory;
import com.yiruantong.inventory.service.core.ICoreInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * TMX 物料主数据增量同步编排（Task 4）。
 *
 * <p>流程：开关由作业外壳守卫 → 游标翻页拉料号摘要 → 逐条取详情 → 字段权威规则
 * upsert（按 product_code 幂等）→ 推进水位。
 *
 * <p>纪律：
 * <ul>
 *   <li>**禁静默失败**：取数/解析任何异常向上抛，作业显式失败、**水位不推进**，
 *       下一轮从头再来；绝不吃掉异常返回「同步了 0 条」；</li>
 *   <li>**字段权威**：名称/规格/单位/类别/项目/描述以 TMX 为准；默认货位/箱规/价格
 *       归 WMS 本地，本类只碰权威字段（见 {@link MaterialMapper}）；</li>
 *   <li>**停用三态**：TMX 停用料号——无库存才标记停用（productStatus=2）；有库存只进
 *       例外清单；WMS 未建档则不建档；</li>
 *   <li>**水位锚在服务端**：下次水位 = 本次首次响应的 server_time 留 5 分钟边际。
 *       WMS 与 TMX 是两台机器，客户端时钟有漂移，用它会丢行；边际内的重复扫描由
 *       按 product_code 的幂等 upsert 消化。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class MaterialSyncService {

  /** 每页条数（TMX 侧上限 200） */
  private static final int PAGE_SIZE = 200;

  /** 防呆：游标异常时最多翻这么多页 */
  private static final int MAX_PAGES = 1000;

  /** 水位安全边际（分钟）：覆盖「本轮查询已执行但响应在路上」的边界行 */
  private static final int WATERMARK_MARGIN_MINUTES = 5;

  /** 新增商品的创建人标记（对拍用：create_by_name='系统导入'） */
  private static final String SYNC_CREATOR = "系统导入";

  /** 商品状态：1=正常，2=停用（该列原为休眠字段，值词汇由本作业定义并在此登记） */
  private static final int STATUS_NORMAL = 1;
  private static final int STATUS_DISABLED = 2;

  private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final TmxIntegrationConfig config;
  private final IBaseProductService productService;
  private final IBaseProductTypeService productTypeService;
  private final ICoreInventoryService inventoryService;

  private final MaterialMapper mapper = new MaterialMapper();

  /**
   * 跑一轮同步。开关守卫在 {@code MaterialSyncJobExecutor}，本方法假定开关已开。
   *
   * @throws RuntimeException 任何失败（TMX 不可达/被拒/degraded、游标异常、写库失败）——
   *         水位保持原值，下一轮全量重扫（幂等）
   */
  public SyncReport sync() {
    // 每次运行现取客户端：地址/口令/令牌可能在系统参数里刚改过
    TmxClient client = config.buildClient();
    String rawWatermark = config.getWatermark();
    String watermark = (rawWatermark == null || rawWatermark.isBlank()) ? "" : rawWatermark.strip();
    SyncReport report = new SyncReport();

    String after = "";
    String prevAfter = null;
    String firstServerTime = null;

    for (int page = 1; page <= MAX_PAGES; page++) {
      PartsPage partsPage = client.fetchPartsPage("", PAGE_SIZE, after, watermark);
      report.setPages(report.getPages() + 1);

      if (firstServerTime == null && partsPage.getServerTime() != null
        && !partsPage.getServerTime().isBlank()) {
        firstServerTime = partsPage.getServerTime().strip();
      }
      for (PartBrief brief : orEmpty(partsPage.getParts())) {
        syncOne(client, brief.getPartNo(), report);
      }

      String next = partsPage.getNextAfter();
      if (next == null || next.isBlank()) {
        break;
      }
      next = next.strip();
      if (next.equals(prevAfter)) {
        // 游标不前进 = TMX 侧分页契约坏了；继续翻只是空转，宁可显式失败
        throw new IllegalStateException("TMX 游标不前进（next_after 重复：" + next + "），终止同步保水位");
      }
      prevAfter = next;
      after = next;
    }

    if (firstServerTime != null) {
      String newWatermark = minusMinutes(firstServerTime, WATERMARK_MARGIN_MINUTES);
      config.saveWatermark(newWatermark);
      report.setNewWatermark(newWatermark);
    }
    return report;
  }

  // ---------------------------------------------------------------- 单条

  private void syncOne(TmxClient client, String partNo, SyncReport report) {
    PartDetail detail = client.fetchPart(partNo);
    if (detail == null) {
      report.getExceptions().add("TMX 返回空详情，跳过：" + partNo);
      return;
    }
    MaterialMapping mapping = mapper.map(detail);
    report.getWarnings().addAll(mapping.getWarnings());

    if (mapping.isVirtualPart()) {
      report.setSkippedVirtual(report.getSkippedVirtual() + 1);
      report.getExceptions().addAll(mapping.getWarnings());
      return;
    }

    BaseProduct existing = productService.lambdaQuery()
      .eq(BaseProduct::getProductCode, partNo).one();

    if (existing == null) {
      if (mapping.isInactive()) {
        // 停用料号不往 WMS 录
        report.setSkippedInactiveNew(report.getSkippedInactiveNew() + 1);
        report.getExceptions().add("TMX 停用料号，WMS 未建档，跳过：" + partNo);
        return;
      }
      insertProduct(mapping, report);
      return;
    }

    if (mapping.isInactive()) {
      if (hasStock(partNo)) {
        // 只标记（例外清单）不停用：有库存的商品停用会丢作业底数
        report.setBlockedByInventory(report.getBlockedByInventory() + 1);
        report.getExceptions().add("TMX 停用但 WMS 有库存，未停用（待处理）：" + partNo);
        return;
      }
      existing.setProductStatus((long) STATUS_DISABLED);
      if (!productService.updateById(existing)) {
        throw new IllegalStateException("标记停用写库失败：" + partNo);
      }
      report.setMarkedDisabled(report.getMarkedDisabled() + 1);
      return;
    }

    applyAuthoritativeFields(existing, mapping, report);
    if (existing.getProductStatus() != null && existing.getProductStatus() == STATUS_DISABLED) {
      existing.setProductStatus((long) STATUS_NORMAL);
      report.setRestoredActive(report.getRestoredActive() + 1);
    }
    if (!productService.updateById(existing)) {
      throw new IllegalStateException("商品更新写库失败：" + partNo);
    }
    report.setUpdated(report.getUpdated() + 1);
  }

  private void insertProduct(MaterialMapping mapping, SyncReport report) {
    BaseProductBo bo = mapping.getBo();
    BaseProduct product = new BaseProduct();
    applyAuthoritativeFields(product, mapping, report);
    product.setProductStatus((long) STATUS_NORMAL);
    product.setTenantId(TenantConstants.DEFAULT_TENANT_ID);
    product.setCreateBy(0L);
    product.setCreateByName(SYNC_CREATOR);
    if (!productService.save(product)) {
      throw new IllegalStateException("商品保存失败：" + bo.getProductCode());
    }
    report.setInserted(report.getInserted() + 1);
  }

  /**
   * 把 TMX 权威字段写到商品上（新增与更新共用）。
   *
   * <p>**只写权威字段**：默认货位（positionName）、价格、箱规等本地字段这里根本不出现，
   * 从结构上不可能被覆盖。类别 ID 查库解析；查不到只记例外、不动原值。
   */
  private void applyAuthoritativeFields(BaseProduct product, MaterialMapping mapping, SyncReport report) {
    BaseProductBo bo = mapping.getBo();
    product.setProductCode(bo.getProductCode());
    product.setProductName(bo.getProductName());
    product.setProductSpec(bo.getProductSpec());
    product.setSmallUnit(bo.getSmallUnit());
    product.setProductModel(bo.getProductModel());
    product.setProductDesc(bo.getProductDesc());

    BaseProductType type = productTypeService.lambdaQuery()
      .eq(BaseProductType::getTypeCode, mapping.getTypeCode()).one();
    if (type == null) {
      report.getExceptions().add("WMS 类别不存在，类别字段未更新：" + bo.getProductCode()
        + " typeCode=" + mapping.getTypeCode());
      return;
    }
    product.setTypeId(type.getTypeId());
    product.setTypeName(type.getTypeName());
    product.setBigTypeId(type.getParentId());
    product.setBigTypeName(type.getRootName());
  }

  /** 是否仍有实际库存（存量 > 0；零库存行不阻止停用）。 */
  private boolean hasStock(String partNo) {
    return inventoryService.lambdaQuery()
      .eq(CoreInventory::getProductCode, partNo)
      .gt(CoreInventory::getProductStorage, BigDecimal.ZERO)
      .count() > 0;
  }

  // ---------------------------------------------------------------- 工具

  private static <T> List<T> orEmpty(List<T> list) {
    return list == null ? Collections.emptyList() : list;
  }

  /**
   * 水位回退：server_time 留 {@value #WATERMARK_MARGIN_MINUTES} 分钟边际。
   *
   * <p>格式非法直接抛——水位是同步正确性的锚，猜格式比失败更危险。
   */
  private static String minusMinutes(String serverTime, int minutes) {
    LocalDateTime t = LocalDateTime.parse(serverTime, TS_FORMAT);
    return t.minusMinutes(minutes).format(TS_FORMAT);
  }
}
