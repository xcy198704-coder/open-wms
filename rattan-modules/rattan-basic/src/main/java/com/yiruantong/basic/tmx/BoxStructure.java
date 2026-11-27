package com.yiruantong.basic.tmx;

import lombok.Data;

/**
 * 一个产品的箱规结构（由包装 BOM 行解析而来）。
 *
 * @see BoxStructureResolver#resolve
 */
@Data
public class BoxStructure {

  /** 每内箱装数（SKU 模式下 = 每外箱装数） */
  private int innerQty;

  /** 每外箱内箱数（SKU 模式恒为 1） */
  private int outerInnerCount;

  /** 箱码模式：SKU（无内盒）/ BOTH（内外盒都有） */
  private String boxCodeMode;

  /** 箱规名称（幂等键的一半；与 11-25 人工导入的命名同形） */
  private String specName;

  /** 备注：BOM 包装用量原值（外箱行的 qty_text） */
  private String remark;
}
