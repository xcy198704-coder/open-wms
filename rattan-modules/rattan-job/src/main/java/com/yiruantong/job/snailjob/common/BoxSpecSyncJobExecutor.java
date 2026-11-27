package com.yiruantong.job.snailjob.common;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.yiruantong.basic.tmx.TmxIntegrationConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 箱规自动生成作业（Task 8）。
 *
 * <p>开关 {@code tmx_boxSpecEnable} 默认 0（与主数据同步分开灰度——箱规直接改
 * 作业底数）。关闭即返回 success「未开启」；例外清单逐条进 LOCAL 日志；
 * 取数失败（网络/鉴权）→ failure，推导失败的料号已在例外清单里、不影响其他商品。
 */
@Component
@RequiredArgsConstructor
@JobExecutor(name = "BoxSpecSyncJobExecutor")
public class BoxSpecSyncJobExecutor {

  private final TmxIntegrationConfig tmxConfig;
  private final BoxSpecSyncService boxSpecSyncService;

  public ExecuteResult jobExecute(JobArgs jobArgs) {
    if (!tmxConfig.isBoxSpecEnabled()) {
      return ExecuteResult.success("未开启箱规自动生成（tmx_boxSpecEnable=0）");
    }
    try {
      BoxSpecSyncReport report = boxSpecSyncService.sync();
      for (String exception : report.getExceptions()) {
        SnailJobLog.LOCAL.warn("TMX 箱规同步例外：{}", exception);
      }
      return ExecuteResult.success(report.summary());
    } catch (Exception exception) {
      return ExecuteResult.failure(exception, "箱规自动生成失败：" + exception.getMessage());
    }
  }
}
