package com.yiruantong.basic.tmx;

import com.yiruantong.basic.domain.product.bo.BaseProductBo;
import cn.hutool.core.util.StrUtil;

/**
 * TMX 料号详情 → WMS 商品档案的字段映射（Task 4 字段权威规则的落点）。
 *
 * <p>权威表（TMX 对接实施计划 Task 4）：
 * <ul>
 *   <li>TMX 权威（覆盖本地）：product_code / product_name / small_unit /
 *       product_model / product_desc，以及经查库解析的 type_id/big_type_id；</li>
 *   <li>WMS 本地（同步不碰）：默认货位、箱规、价格——本映射器**不设**这些字段，
 *       从结构上保证不会覆盖；</li>
 *   <li>虚拟件（partAttr=虚拟）：不建商品档案，只进例外清单。</li>
 * </ul>
 *
 * <p>product_model 存「项目/机型」串，与 2026-11-25 人工导入先例同形
 * （BY-ML(FT)、JN-CD-PJ444T(PM128) 见 script/sql/1.0.0.6）。xlsm 来源里的全角
 * 括号变体（PG-P7（PM256））不做复刻——同步只产出一种确定形态，对拍时核对。
 */
public class MaterialMapper {

  /** 虚拟件属性值（WMS 不建档案）。 */
  public static final String PART_ATTR_VIRTUAL = "虚拟";

  /** TMX 停用态：审批被驳回（废止态 product_state / 软删 is_deleted 当前 DTO 未暴露，待补）。 */
  private static final String APPROVE_STATE_REJECTED = "已驳回";

  private final CategoryMapping categoryMapping = new CategoryMapping();

  /**
   * 映射一个料号。虚拟件返回 {@code virtualPart=true} 且 {@code bo=null}。
   */
  public MaterialMapping map(PartDetail detail) {
    MaterialMapping m = new MaterialMapping();
    m.setInactive(isInactive(detail));

    if (PART_ATTR_VIRTUAL.equals(StrUtil.str(detail.getPartAttr()).trim())) {
      m.setVirtualPart(true);
      m.getWarnings().add("虚拟件不建商品档案：" + detail.getPartNo());
      return m;
    }

    BaseProductBo bo = new BaseProductBo();
    bo.setProductCode(str(detail.getPartNo()));
    bo.setProductName(str(detail.getName()));
    bo.setProductSpec(str(detail.getSpec()));
    bo.setSmallUnit(str(detail.getUnit()));
    bo.setProductModel(toProjectModel(detail));
    bo.setProductDesc("TMX同步 版本" + str(detail.getVersion())
      + " 更新于" + str(detail.getUpdatedAt()));
    m.setBo(bo);

    String typeCode = categoryMapping.resolve(detail.getMaterialCategory(), detail.getPartNo(), detail.getName());
    m.setTypeCode(typeCode);
    if (CategoryMapping.TBD.equals(typeCode)) {
      m.getWarnings().add("物料类别未映射，回落待分类：" + detail.getPartNo()
        + " materialCategory=" + detail.getMaterialCategory());
    }
    return m;
  }

  /**
   * TMX 停用态判定。
   *
   * <p>当前只看 approve_state=已驳回。TMX 的废止态（product_state=已废止/已迁移）与
   * 软删（is_deleted）在 PartBrief/PartDetail 里**没有字段**，WMS 侧看不见——
   * 已向 TMX 侧 flag 补字段；字段补上后在此扩展谓词即可，编排框架不变。
   */
  public boolean isInactive(PartDetail detail) {
    return APPROVE_STATE_REJECTED.equals(StrUtil.str(detail.getApproveState()).trim());
  }

  /**
   * 项目/机型串：双码 → {@code internalCode(customerCode)}；单码 → 该码；
   * 双码皆无 → 回落产品系列；全空 → 空串。
   */
  public String toProjectModel(PartDetail detail) {
    String internal = str(detail.getInternalCode());
    String customer = str(detail.getCustomerCode());
    if (!internal.isEmpty() && !customer.isEmpty()) {
      return internal + "(" + customer + ")";
    }
    if (!internal.isEmpty()) {
      return internal;
    }
    if (!customer.isEmpty()) {
      return customer;
    }
    return str(detail.getProductSeries());
  }

  private static String str(String value) {
    return value == null ? "" : value.strip();
  }
}
