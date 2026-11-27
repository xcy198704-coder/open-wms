package com.yiruantong.basic.tmx;

import com.yiruantong.basic.domain.product.bo.BaseProductBo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次料号映射的结果（TMX 详情 → WMS 商品档案的中间态）。
 *
 * <p>类别只给 {@code typeCode}（纯解析，不查库）：type_id/big_type_id 等 ID 要查
 * base_product_type，是编排层（{@code MaterialSyncService}）的事，映射器保持纯净可单测。
 */
@Data
public class MaterialMapping {

  /** TMX 权威字段集（仅这些字段；本地字段映射器根本不设，由编排层决定覆不覆盖）。 */
  private BaseProductBo bo;

  /** WMS 类别编码（BOM-*；未命中回落 {@link CategoryMapping#TBD}）。 */
  private String typeCode;

  /** 虚拟件：WMS 不建商品档案，只进例外清单。 */
  private boolean virtualPart;

  /** TMX 停用态（已驳回；废止态待 TMX DTO 补 product_state 后扩展，见类注释）。 */
  private boolean inactive;

  /** 映射过程中的告警（回落待分类等），编排层汇总进例外清单。 */
  private final List<String> warnings = new ArrayList<>();
}
