package com.yiruantong.basic.tmx;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.Data;

import java.util.List;

/**
 * TMX 料号查询的一页（游标分页契约，2026-11-27 与 Go 侧对齐）。
 *
 * <p>对应 {@code GET /api/wms/parts} 的响应信封：
 * {@code {"parts":[…], "next_after":"<末条part_no或空>", "server_time":"…", "degraded":false}}。
 *
 * <ul>
 *   <li>{@code nextAfter} 空 = 没有了；非空 = 拿它当下一页的 {@code after}。
 *       满页才可能有值，不满页一定为空；</li>
 *   <li>{@code serverTime} 是 TMX 服务端时钟——**增量水位用它取**，不用 WMS 机器时钟
 *       （两台机器时钟有漂移，用客户端时钟会丢行）。</li>
 * </ul>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PartsPage {

  /** 本页料号摘要（恒按 part_no 升序）。 */
  private List<PartBrief> parts;

  /** 下一页游标（本页最后一条 part_no）；空 = 没有下一页。 */
  private String nextAfter;

  /** TMX 服务端当前时刻（{@code yyyy-MM-dd HH:mm:ss}），下次同步水位以它为锚。 */
  private String serverTime;
}
