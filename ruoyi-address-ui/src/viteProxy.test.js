import { describe, expect, it } from 'vitest';

import createConfig from '../vite.config.js';

describe('vite 代理配置', () => {
  it('microservice 模式应统一通过 prod-api 代理到本地 nginx 80 端口', () => {
    process.env.VITE_ADDRESS_RUNTIME_MODE = 'microservice';

    const config = createConfig({ mode: 'test' });
    const prodApiProxy = config.server.proxy['/prod-api'];

    expect(prodApiProxy).toBeTruthy();
    expect(prodApiProxy.target).toBe('http://127.0.0.1:80');
    expect(config.server.proxy['/address']).toBeUndefined();
    expect(config.server.proxy['/workflow']).toBeUndefined();
  });

  it('应将 workflow 代理前缀重写为后端原始路径', () => {
    process.env.VITE_ADDRESS_RUNTIME_MODE = 'standalone';

    const config = createConfig({ mode: 'test' });
    const workflowProxy = config.server.proxy['/workflow'];

    expect(workflowProxy).toBeTruthy();
    expect(workflowProxy.target).toBe('http://127.0.0.1:9205');
    expect(workflowProxy.rewrite('/workflow/task/pageByAllTaskWait')).toBe('/task/pageByAllTaskWait');
    expect(workflowProxy.rewrite('/workflow/instance/flowHisTaskList/1001')).toBe('/instance/flowHisTaskList/1001');
  });

  it('应保留 address 代理前缀不变', () => {
    process.env.VITE_ADDRESS_RUNTIME_MODE = 'standalone';

    const config = createConfig({ mode: 'test' });
    const addressProxy = config.server.proxy['/address'];

    expect(addressProxy).toBeTruthy();
    expect(addressProxy.rewrite).toBeUndefined();
  });

  it('standalone 模式应直连本地地址与 workflow 服务', () => {
    process.env.VITE_ADDRESS_RUNTIME_MODE = 'standalone';
    process.env.VITE_ADDRESS_STANDALONE_BASE = 'http://127.0.0.1:9206';
    process.env.VITE_WORKFLOW_STANDALONE_BASE = 'http://127.0.0.1:9205';
    process.env.VITE_GATEWAY_BASE = 'http://127.0.0.1:8080';

    const config = createConfig({ mode: 'test' });
    const addressProxy = config.server.proxy['/address'];
    const workflowProxy = config.server.proxy['/workflow'];

    expect(addressProxy.target).toBe('http://127.0.0.1:9206');
    expect(workflowProxy.target).toBe('http://127.0.0.1:9205');
    expect(workflowProxy.rewrite('/workflow/task/pageByAllTaskWait')).toBe('/task/pageByAllTaskWait');
  });

  it('microservice 模式应统一走 gateway 且 workflow 不重写路径', () => {
    process.env.VITE_ADDRESS_RUNTIME_MODE = 'microservice';
    process.env.VITE_ADDRESS_STANDALONE_BASE = 'http://127.0.0.1:9206';
    process.env.VITE_WORKFLOW_STANDALONE_BASE = 'http://127.0.0.1:9205';
    process.env.VITE_GATEWAY_BASE = 'http://127.0.0.1:8080';

    const config = createConfig({ mode: 'test' });
    const prodApiProxy = config.server.proxy['/prod-api'];

    expect(prodApiProxy.target).toBe('http://127.0.0.1:80');
    expect(prodApiProxy.rewrite).toBeUndefined();
  });
});
