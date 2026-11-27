package com.yiruantong.job.snailjob.common;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.yiruantong.basic.tmx.TmxIntegrationConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 物料主数据增量同步作业（Task 4）。
 *
 * <p>开关 {@code tmx_syncEnable} 默认 0：关闭即返回 success「未开启」，
 * 行为与对接前逐字节一致。同步编排在 {@link MaterialSyncService}，本类只做
 * 守卫、日志与结果映射。
 *
 * <p>失败口径：任何异常 → {@link ExecuteResult#failure}，**水位不推进**
 * （service 保证），下一轮从头重扫；例外清单逐条进 LOCAL 日志，不静默。
 */
@Component
@RequiredArgsConstructor
@JobExecutor(name = "MaterialSyncJobExecutor")
public class MaterialSyncJobExecutor {

  private final TmxIntegrationConfig tmxConfig;
  private final MaterialSyncService materialSyncService;

  public ExecuteResult jobExecute(JobArgs jobArgs) {
    if (!tmxConfig.isSyncEnabled()) {
      return ExecuteResult.success("未开启物料主数据同步（tmx_syncEnable=0）");
    }
    try {
      SyncReport report = materialSyncService.sync();
      for (String exception : report.getExceptions()) {
        SnailJobLog.LOCAL.warn("TMX 物料同步例外：{}", exception);
      }
      for (String warning : report.getWarnings()) {
        SnailJobLog.LOCAL.warn("TMX 物料同步告警：{}", warning);
      }
      return ExecuteResult.success(report.summary());
    } catch (Exception exception) {
      return ExecuteResult.failure(exception, "物料主数据同步失败（水位未推进）：" + exception.getMessage());
    }
  }
}
