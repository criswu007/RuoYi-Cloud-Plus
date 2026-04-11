import { describe, expect, it } from 'vitest';

import {
  AUTH_CLIENT_ID_STORAGE_KEY,
  AUTH_REFRESH_TOKEN_STORAGE_KEY,
  LOGIN_REMEMBER_STORAGE_KEY,
  clearAuthSession,
  persistAuthSession,
  readRememberedLogin,
  writeRememberedLogin
} from './auth-session';
import { ADDRESS_TOKEN_STORAGE_KEY } from './token-bridge';

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

describe('认证会话存储', () => {
  it('登录成功后应同时持久化新旧 token 与 clientId', () => {
    const storage = createStorage();

    persistAuthSession(storage, {
      accessToken: 'plain-access-token',
      refreshToken: 'refresh-token',
      clientId: 'client-demo'
    });

    expect(storage.getItem(ADDRESS_TOKEN_STORAGE_KEY)).toBe('plain-access-token');
    expect(storage.getItem('Admin-Token')).toBe('plain-access-token');
    expect(storage.getItem(AUTH_REFRESH_TOKEN_STORAGE_KEY)).toBe('refresh-token');
    expect(storage.getItem(AUTH_CLIENT_ID_STORAGE_KEY)).toBe('client-demo');
  });

  it('退出登录时应清理认证会话缓存', () => {
    const storage = createStorage({
      [ADDRESS_TOKEN_STORAGE_KEY]: 'token',
      'Admin-Token': 'token',
      [AUTH_REFRESH_TOKEN_STORAGE_KEY]: 'refresh',
      [AUTH_CLIENT_ID_STORAGE_KEY]: 'client-demo'
    });

    clearAuthSession(storage);

    expect(storage.getItem(ADDRESS_TOKEN_STORAGE_KEY)).toBeNull();
    expect(storage.getItem('Admin-Token')).toBeNull();
    expect(storage.getItem(AUTH_REFRESH_TOKEN_STORAGE_KEY)).toBeNull();
    expect(storage.getItem(AUTH_CLIENT_ID_STORAGE_KEY)).toBeNull();
  });

  it('勾选记住登录信息时应仅保留租户和用户名', () => {
    const storage = createStorage();

    writeRememberedLogin(storage, {
      tenantId: '000000',
      username: 'admin',
      password: 'admin123',
      rememberMe: true
    });

    expect(JSON.parse(storage.getItem(LOGIN_REMEMBER_STORAGE_KEY))).toEqual({
      tenantId: '000000',
      username: 'admin',
      password: 'admin123',
      rememberMe: true
    });
    expect(readRememberedLogin(storage)).toEqual({
      tenantId: '000000',
      username: 'admin',
      password: 'admin123',
      rememberMe: true
    });
  });

  it('未勾选记住登录信息时应清理历史缓存', () => {
    const storage = createStorage({
      [LOGIN_REMEMBER_STORAGE_KEY]: JSON.stringify({
        tenantId: '000000',
        username: 'admin',
        rememberMe: true
      })
    });

    writeRememberedLogin(storage, {
      tenantId: '000000',
      username: 'admin',
      rememberMe: false
    });

    expect(storage.getItem(LOGIN_REMEMBER_STORAGE_KEY)).toBeNull();
    expect(readRememberedLogin(storage)).toEqual({
      tenantId: '',
      username: '',
      password: '',
      rememberMe: false
    });
  });
});
