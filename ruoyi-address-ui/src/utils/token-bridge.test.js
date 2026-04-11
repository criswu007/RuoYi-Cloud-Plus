import { describe, expect, it } from 'vitest';

import {
  ADDRESS_TOKEN_STORAGE_KEY,
  normalizeTokenValue,
  resolveInitialToken,
  resolveLegacyRefreshToken
} from './token-bridge';

function createStorage(initial = {}) {
  const storage = new Map(Object.entries(initial));
  return {
    getItem(key) {
      return storage.has(key) ? storage.get(key) : null;
    },
    setItem(key, value) {
      storage.set(key, String(value));
    },
    removeItem(key) {
      storage.delete(key);
    }
  };
}

describe('旧系统 token 桥接', () => {
  it('应优先从旧系统 Admin-Token 读取最新 token', () => {
    const storage = createStorage({
      [ADDRESS_TOKEN_STORAGE_KEY]: 'expired-address-token',
      'Admin-Token': 'latest-admin-token'
    });

    expect(resolveLegacyRefreshToken({ storage })).toBe('latest-admin-token');
  });

  it('旧系统 localStorage 不存在时应回退到同域 cookie 中的 Authorization', () => {
    expect(resolveLegacyRefreshToken({
      storage: createStorage(),
      cookieString: 'theme=dark; Authorization=Bearer%20cookie-token'
    })).toBe('Bearer cookie-token');
  });

  it('初始化时应优先保留当前调试台保存的 AUTH_TOKEN', () => {
    const storage = createStorage({
      [ADDRESS_TOKEN_STORAGE_KEY]: 'saved-address-token',
      'Admin-Token': 'latest-admin-token'
    });

    expect(resolveInitialToken({ storage })).toBe('saved-address-token');
  });

  it('应清理 token 两端空白和包裹引号', () => {
    expect(normalizeTokenValue('  \"Bearer demo-token\"  ')).toBe('Bearer demo-token');
  });
});
