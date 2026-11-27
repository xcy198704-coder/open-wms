package com.yiruantong.basic.tmx;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 推送库存单据结果（TMX → WMS）。
 *
 * <p>{@code duplicated=true} 表示该单号已过过账——WMS 侧应视为**成功**，不得重推。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InventoryPostResult {

  private Boolean duplicated;

  private Boolean posted;
}
