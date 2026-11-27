package com.yiruantong.basic.tmx;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 料号摘要（TMX 只读接口 DTO）。
 *
 * <p>JSON 字段与 TMX Go 侧一致，用 snake_case（参照 Go 侧
 * {@code internal/dcc/service/bom_tree_provider.go} 的 json tag 惯例）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PartBrief {

  /** 料号，唯一键 */
  private String partNo;

  private String name;

  private String spec;

  /** 计量单位：pcs / m */
  private String unit;

  /** TMX 类别原值 */
  private String category;

  /** 零件物料属性（PCB/塑料件/端子/紧固件/电缆/包材…）；WMS 分类以它为权威 */
  private String materialCategory;

  /** 料号类型：产品 / 零件 */
  private String partKind;

  /** 料号属性：自制 / 采购 / 虚拟（虚拟件在 WMS 不建商品档案） */
  private String partAttr;

  /** 版本 */
  private String version;

  /** 对应包装料号 */
  private String packagePartNo;

  private String engineer;

  /** 生命周期：试产 / 量产 */
  private String lifecycle;

  /** 审批状态：''/已承认/已审核/已核准 */
  private String approveState;

  /** 更新时间原文（TMX 侧为字符串） */
  private String updatedAt;
}
