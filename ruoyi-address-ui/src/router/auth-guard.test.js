import { describe, expect, it } from 'vitest';

import { resolveAuthRedirect } from './auth-guard';

function createStorage(initial = {}) {
  const storage = new Map(Object.entries(initial));
  return {
    getItem(key) {
      return storage.has(key) ? storage.get(key) : null;
    }
  };
}

describe('登录路由守卫', () => {
  it('未登录访问业务页面时应跳转到登录页并保留 redirect', () => {
    expect(resolveAuthRedirect({
      path: '/standard/list',
      fullPath: '/standard/list?pageNum=2',
      matched: [{ meta: {} }]
    }, createStorage())).toEqual({
      path: '/login',
      query: {
        redirect: '/standard/list?pageNum=2'
      }
    });
  });

  it('公开页面不应被守卫重定向', () => {
    expect(resolveAuthRedirect({
      path: '/login',
      fullPath: '/login',
      matched: [{ meta: { public: true } }]
    }, createStorage())).toBeNull();
  });

  it('已登录用户访问登录页时应回到标准地址列表', () => {
    expect(resolveAuthRedirect({
      path: '/login',
      fullPath: '/login',
      matched: [{ meta: { public: true } }]
    }, createStorage({
      AUTH_TOKEN: 'active-token'
    }))).toEqual({
      path: '/standard/list'
    });
  });

  it('已登录用户访问业务页面时应直接放行', () => {
    expect(resolveAuthRedirect({
      path: '/standard/list',
      fullPath: '/standard/list',
      matched: [{ meta: {} }]
    }, createStorage({
      AUTH_TOKEN: 'active-token'
    }))).toBeNull();
  });
});
