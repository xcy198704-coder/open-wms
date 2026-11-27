package com.yiruantong.job.snailjob.common;

import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.yiruantong.basic.tmx.TmxIntegrationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 同步作业外壳测试：开关守卫、结果映射、失败不吞。
 * 同步编排的真实行为由 {@code MaterialSyncServiceTest} 覆盖。
 */
class MaterialSyncJobExecutorTest {

  private final TmxIntegrationConfig tmxConfig = mock(TmxIntegrationConfig.class);
  private final MaterialSyncService syncService = mock(MaterialSyncService.class);

  private final MaterialSyncJobExecutor executor =
    new MaterialSyncJobExecutor(tmxConfig, syncService);

  @Test
  @DisplayName("开关关闭：success「未开启」，一次同步都不跑")
  void disabledSwitchShortCircuits() {
    when(tmxConfig.isSyncEnabled()).thenReturn(false);

    ExecuteResult result = executor.jobExecute(new JobArgs());

    assertEquals(ExecuteResult.success("probe").getStatus(), result.getStatus(), "应为成功状态");
    assertTrue(String.valueOf(result.getResult()).contains("未开启"));
    verifyNoInteractions(syncService);
  }

  @Test
  @DisplayName("开关开启：透传同步报告摘要为 success")
  void enabledDelegatesAndReports() {
    when(tmxConfig.isSyncEnabled()).thenReturn(true);
    SyncReport report = new SyncReport();
    report.setInserted(3);
    when(syncService.sync()).thenReturn(report);

    ExecuteResult result = executor.jobExecute(new JobArgs());

    assertEquals(ExecuteResult.success("probe").getStatus(), result.getStatus(), "应为成功状态");
    assertTrue(String.valueOf(result.getResult()).contains("新增3"));
  }

  @Test
  @DisplayName("同步失败：ExecuteResult.failure，消息带「水位未推进」")
  void failureMapsToExecuteResultFailure() {
    when(tmxConfig.isSyncEnabled()).thenReturn(true);
    when(syncService.sync()).thenThrow(new IllegalStateException("TMX 503"));

    ExecuteResult result = executor.jobExecute(new JobArgs());

    assertEquals(ExecuteResult.failure("probe").getStatus(), result.getStatus(), "应为失败状态");
    assertTrue(String.valueOf(result.getMessage()).contains("水位未推进"));
  }
}
