import { describe, expect, it } from 'vitest';

import createConfig from '../vite.config.js';

describe('vite 代理配置', () => {
  it('应将 workflow 代理前缀重写为后端原始路径', () => {
    const config = createConfig({ mode: 'test' });
    const workflowProxy = config.server.proxy['/workflow'];

    expect(workflowProxy).toBeTruthy();
    expect(workflowProxy.target).toBe('http://localhost:9205');
    expect(workflowProxy.rewrite('/workflow/task/pageByAllTaskWait')).toBe('/task/pageByAllTaskWait');
    expect(workflowProxy.rewrite('/workflow/instance/flowHisTaskList/1001')).toBe('/instance/flowHisTaskList/1001');
  });

  it('应保留 address 代理前缀不变', () => {
    const config = createConfig({ mode: 'test' });
    const addressProxy = config.server.proxy['/address'];

    expect(addressProxy).toBeTruthy();
    expect(addressProxy.rewrite).toBeUndefined();
  });
});
