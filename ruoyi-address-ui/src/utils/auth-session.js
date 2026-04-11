import {
  ADDRESS_TOKEN_STORAGE_KEY,
  clearAddressToken,
  normalizeTokenValue,
  writeAddressToken
} from './token-bridge';

export const AUTH_CLIENT_ID_STORAGE_KEY = 'AUTH_CLIENT_ID';
export const AUTH_REFRESH_TOKEN_STORAGE_KEY = 'AUTH_REFRESH_TOKEN';
export const LOGIN_REMEMBER_STORAGE_KEY = 'ADDRESS_LOGIN_REMEMBER';
export const LEGACY_COMPAT_TOKEN_STORAGE_KEY = 'Admin-Token';

const DEFAULT_REMEMBERED_LOGIN = Object.freeze({
  tenantId: '',
  username: '',
  password: '',
  rememberMe: false
});

function removeStorageItem(storage, key) {
  if (!storage || typeof storage.removeItem !== 'function') {
    return;
  }
  storage.removeItem(key);
}

export function persistAuthSession(storage, session = {}) {
  if (!storage || typeof storage.setItem !== 'function') {
    return;
  }

  const accessToken = normalizeTokenValue(session.accessToken);
  const refreshToken = normalizeTokenValue(session.refreshToken);
  const clientId = normalizeTokenValue(session.clientId);

  if (accessToken) {
    writeAddressToken(storage, accessToken);
    storage.setItem(LEGACY_COMPAT_TOKEN_STORAGE_KEY, accessToken);
  } else {
    clearAddressToken(storage);
    removeStorageItem(storage, LEGACY_COMPAT_TOKEN_STORAGE_KEY);
  }

  if (refreshToken) {
    storage.setItem(AUTH_REFRESH_TOKEN_STORAGE_KEY, refreshToken);
  } else {
    removeStorageItem(storage, AUTH_REFRESH_TOKEN_STORAGE_KEY);
  }

  if (clientId) {
    storage.setItem(AUTH_CLIENT_ID_STORAGE_KEY, clientId);
  } else {
    removeStorageItem(storage, AUTH_CLIENT_ID_STORAGE_KEY);
  }
}

export function clearAuthSession(storage) {
  if (!storage) {
    return;
  }
  clearAddressToken(storage);
  removeStorageItem(storage, LEGACY_COMPAT_TOKEN_STORAGE_KEY);
  removeStorageItem(storage, AUTH_CLIENT_ID_STORAGE_KEY);
  removeStorageItem(storage, AUTH_REFRESH_TOKEN_STORAGE_KEY);
}

export function readRememberedLogin(storage) {
  if (!storage || typeof storage.getItem !== 'function') {
    return {
      ...DEFAULT_REMEMBERED_LOGIN
    };
  }
  const rawValue = storage.getItem(LOGIN_REMEMBER_STORAGE_KEY);
  if (!rawValue) {
    return {
      ...DEFAULT_REMEMBERED_LOGIN
    };
  }
  try {
    const remembered = JSON.parse(rawValue);
    return {
      tenantId: normalizeTokenValue(remembered.tenantId),
      username: normalizeTokenValue(remembered.username),
      password: normalizeTokenValue(remembered.password),
      rememberMe: Boolean(remembered.rememberMe)
    };
  } catch (error) {
    return {
      ...DEFAULT_REMEMBERED_LOGIN
    };
  }
}

export function writeRememberedLogin(storage, form = {}) {
  if (!storage || typeof storage.setItem !== 'function') {
    return;
  }
  if (!form.rememberMe) {
    removeStorageItem(storage, LOGIN_REMEMBER_STORAGE_KEY);
    return;
  }
  storage.setItem(LOGIN_REMEMBER_STORAGE_KEY, JSON.stringify({
    tenantId: normalizeTokenValue(form.tenantId),
    username: normalizeTokenValue(form.username),
    password: normalizeTokenValue(form.password),
    rememberMe: true
  }));
}

export { ADDRESS_TOKEN_STORAGE_KEY };
