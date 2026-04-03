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

import request from './request';

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
