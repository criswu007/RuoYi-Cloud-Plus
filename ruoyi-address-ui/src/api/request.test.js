import { beforeEach, describe, expect, it, vi } from 'vitest';

const { interceptorStore, mockAxiosCreate } = vi.hoisted(() => {
  const interceptorStore = {
    requestFulfilled: null,
    responseFulfilled: null,
    responseRejected: null
  };
  const mockInstance = {
    interceptors: {
      request: {
        use: vi.fn(handler => {
          interceptorStore.requestFulfilled = handler;
        })
      },
      response: {
        use: vi.fn((fulfilled, rejected) => {
          interceptorStore.responseFulfilled = fulfilled;
          interceptorStore.responseRejected = rejected;
        })
      }
    },
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  };
  return {
    interceptorStore,
    mockAxiosCreate: vi.fn(() => mockInstance)
  };
});

vi.mock('axios', () => ({
  default: {
    create: mockAxiosCreate
  }
}));

import request, { resolveRequestUrl } from './request';

describe('统一请求封装', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    const storage = new Map();
    globalThis.localStorage = {
      getItem(key) {
        return storage.has(key) ? storage.get(key) : null;
      },
      setItem(key, value) {
        storage.set(key, String(value));
      },
      removeItem(key) {
        storage.delete(key);
      },
      clear() {
        storage.clear();
      }
    };
  });

  it('请求拦截器应自动补齐 Bearer Token', () => {
    localStorage.setItem('AUTH_TOKEN', 'demo-token');

    const config = interceptorStore.requestFulfilled({ headers: {} });

    expect(config.headers.Authorization).toBe('Bearer demo-token');
  });

  it('即使未登录也应补齐固定 clientid，和参考前端保持一致', () => {
    const config = interceptorStore.requestFulfilled({ headers: {} });

    expect(config.headers.clientid).toBe('e5cd7e4891bf95d1d19206ce24a7b32e');
  });

  it('显式关闭 token 注入时不应补齐 Authorization', () => {
    localStorage.setItem('AUTH_TOKEN', 'demo-token');

    const config = interceptorStore.requestFulfilled({
      headers: {
        isToken: false
      }
    });

    expect(config.headers.Authorization).toBeUndefined();
  });

  it('请求拦截器应根据 JWT 自动补齐 clientid', () => {
    localStorage.setItem(
      'AUTH_TOKEN',
      'header.eyJjbGllbnRpZCI6ImNsaWVudC1kZW1vIn0.signature'
    );

    const config = interceptorStore.requestFulfilled({ headers: {} });

    expect(config.headers.clientid).toBe('client-demo');
  });

  it('JWT 无法解析时应回退读取登录返回的 clientId 缓存', () => {
    localStorage.setItem('AUTH_TOKEN', 'plain-access-token');
    localStorage.setItem('AUTH_CLIENT_ID', 'client-from-login');

    const config = interceptorStore.requestFulfilled({ headers: {} });

    expect(config.headers.clientid).toBe('client-from-login');
  });

  it('microservice 模式下标准地址主链路应保留单 address 前缀', () => {
    expect(resolveRequestUrl('/address/standard/list', {
      runtimeMode: 'microservice'
    })).toBe('/prod-api/address/standard/list');
  });

  it('microservice 模式下审批链路不应重复补齐 address 前缀', () => {
    expect(resolveRequestUrl('/address/standard/approval/my/page', {
      runtimeMode: 'microservice'
    })).toBe('/prod-api/address/standard/approval/my/page');
  });

  it('microservice 模式下非标准地址主链路应保留单 address 前缀', () => {
    expect(resolveRequestUrl('/address/installation/list', {
      runtimeMode: 'microservice'
    })).toBe('/prod-api/address/installation/list');
  });

  it('microservice 模式下应保留 workflow 原始前缀并走 prod-api', () => {
    expect(resolveRequestUrl('/workflow/task/pageByAllTaskWait', {
      runtimeMode: 'microservice'
    })).toBe('/prod-api/workflow/task/pageByAllTaskWait');
  });

  it('standalone 模式下不应改写原始请求路径', () => {
    expect(resolveRequestUrl('/address/standard/list', {
      runtimeMode: 'standalone'
    })).toBe('/address/standard/list');
  });

  it('已经带 prod-api 前缀的请求不应重复补齐', () => {
    expect(resolveRequestUrl('/prod-api/address/standard/list', {
      runtimeMode: 'microservice'
    })).toBe('/prod-api/address/standard/list');
  });

  it('业务响应码为 500 时应按失败抛出，而不是继续返回成功数据', async () => {
    await expect(Promise.resolve().then(() => interceptorStore.responseFulfilled({
      data: {
        code: 500,
        msg: '保存失败'
      },
      headers: {
        'content-type': 'application/json'
      },
      config: {}
    }))).rejects.toMatchObject({
      friendlyMessage: '保存失败'
    });
  });

  it('下载接口返回 JSON 错误 blob 时也应按失败抛出', async () => {
    const errorBlob = new Blob(
      [JSON.stringify({ code: 500, msg: '导出失败' })],
      { type: 'application/json' }
    );

    await expect(Promise.resolve().then(() => interceptorStore.responseFulfilled({
      data: errorBlob,
      headers: {
        'content-type': 'application/json'
      },
      config: {
        responseType: 'blob'
      }
    }))).rejects.toMatchObject({
      friendlyMessage: '导出失败'
    });
  });

  it('业务响应码为 200 时应继续返回原始数据对象', async () => {
    const payload = {
      code: 200,
      msg: '操作成功',
      data: {
        segmId: '0001'
      }
    };

    await expect(Promise.resolve().then(() => interceptorStore.responseFulfilled({
      data: payload,
      headers: {
        'content-type': 'application/json'
      },
      config: {}
    }))).resolves.toBe(payload);
  });

  it('下载接口返回真实文件 blob 时应继续透传', async () => {
    const fileBlob = new Blob(['demo'], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    });

    await expect(Promise.resolve().then(() => interceptorStore.responseFulfilled({
      data: fileBlob,
      headers: {
        'content-type': fileBlob.type
      },
      config: {
        responseType: 'blob'
      }
    }))).resolves.toBe(fileBlob);
  });
});
