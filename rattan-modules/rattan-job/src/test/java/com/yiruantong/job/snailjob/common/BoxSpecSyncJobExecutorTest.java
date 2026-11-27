package com.yiruantong.job.snailjob.common;

import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.yiruantong.basic.tmx.TmxIntegrationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 箱规作业外壳测试：开关守卫与结果映射。同步编排行为由
 * {@code BoxSpecSyncServiceTest} 覆盖。
 */
class BoxSpecSyncJobExecutorTest {

  private final TmxIntegrationConfig tmxConfig = mock(TmxIntegrationConfig.class);
  private final BoxSpecSyncService syncService = mock(BoxSpecSyncService.class);

  private final BoxSpecSyncJobExecutor executor = new BoxSpecSyncJobExecutor(tmxConfig, syncService);

  @Test
  @DisplayName("开关关闭：success「未开启」，一次同步都不跑")
  void disabledSwitchShortCircuits() {
    when(tmxConfig.isBoxSpecEnabled()).thenReturn(false);

    ExecuteResult result = executor.jobExecute(new JobArgs());

    assertEquals(ExecuteResult.success("probe").getStatus(), result.getStatus());
    assertTrue(String.valueOf(result.getResult()).contains("未开启"));
    verifyNoInteractions(syncService);
  }

  @Test
  @DisplayName("开关开启：透传报告摘要为 success")
  void enabledDelegatesAndReports() {
    when(tmxConfig.isBoxSpecEnabled()).thenReturn(true);
    BoxSpecSyncReport report = new BoxSpecSyncReport();
    report.setSpecsWritten(5);
    when(syncService.sync()).thenReturn(report);

    ExecuteResult result = executor.jobExecute(new JobArgs());

    assertEquals(ExecuteResult.success("probe").getStatus(), result.getStatus());
    assertTrue(String.valueOf(result.getResult()).contains("写入5"));
  }

  @Test
  @DisplayName("同步失败：ExecuteResult.failure")
  void failureMapsToExecuteResultFailure() {
    when(tmxConfig.isBoxSpecEnabled()).thenReturn(true);
    when(syncService.sync()).thenThrow(new IllegalStateException("TMX 503"));

    ExecuteResult result = executor.jobExecute(new JobArgs());

    assertEquals(ExecuteResult.failure("probe").getStatus(), result.getStatus());
    assertTrue(String.valueOf(result.getMessage()).contains("箱规自动生成失败"));
  }
}
