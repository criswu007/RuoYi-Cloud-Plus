import { describe, expect, it, vi } from 'vitest';
import SearchOpsConsole from './SearchOpsConsole.vue';

describe('ES 运维页状态模型', () => {
  it('默认状态应维护概览、重建任务和 repair 查询模型', () => {
    const state = SearchOpsConsole.data();

    expect(state.overview).toEqual({});
    expect(state.taskQuery).toEqual({
      taskType: '',
      status: ''
    });
    expect(state.repairQuery).toEqual({
      entityType: '',
      status: ''
    });
    expect(state.standardConfirmationCode).toBe('');
    expect(state.installationConfirmationCode).toBe('');
    expect(state.repairConfirmationCode).toBe('');
  });

  it('buildTaskQueryParams 应输出 GET 查询兼容字段', () => {
    const params = SearchOpsConsole.methods.buildTaskQueryParams.call({
      taskQuery: {
        taskType: 'REBUILD_STANDARD',
        status: 'RUNNING'
      },
      taskPageNum: 2,
      taskPageSize: 10
    });

    expect(params).toEqual({
      taskType: 'REBUILD_STANDARD',
      status: 'RUNNING',
      pageNum: 2,
      pageSize: 10
    });
  });

  it('存在运行中任务时应启动 5 秒轮询', () => {
    const setIntervalSpy = vi.spyOn(global, 'setInterval').mockReturnValue(1);
    const ctx = {
      pollingTimer: null,
      refreshOverviewAndTasks: vi.fn()
    };

    SearchOpsConsole.methods.startPolling.call(ctx);

    expect(setIntervalSpy).toHaveBeenCalledWith(expect.any(Function), 5000);
    setIntervalSpy.mockRestore();
  });
});
