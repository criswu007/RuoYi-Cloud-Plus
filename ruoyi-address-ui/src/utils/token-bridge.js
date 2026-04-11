export const ADDRESS_TOKEN_STORAGE_KEY = 'AUTH_TOKEN';
export const LEGACY_TOKEN_STORAGE_KEYS = ['Admin-Token', 'Authorization', 'access_token'];
export const LEGACY_TOKEN_COOKIE_KEYS = ['Admin-Token', 'Authorization', 'access_token'];

function decodeCookieValue(value) {
  if (!value || typeof value !== 'string') {
    return '';
  }
  try {
    return decodeURIComponent(value);
  } catch (error) {
    return value;
  }
}

export function normalizeTokenValue(token) {
  if (!token || typeof token !== 'string') {
    return '';
  }
  const trimmed = token.trim();
  if (!trimmed) {
    return '';
  }
  if ((trimmed.startsWith('"') && trimmed.endsWith('"'))
    || (trimmed.startsWith('\'') && trimmed.endsWith('\''))) {
    return trimmed.slice(1, -1).trim();
  }
  return trimmed;
}

function resolveStorageToken(storage, key) {
  if (!storage || typeof storage.getItem !== 'function' || !key) {
    return '';
  }
  return normalizeTokenValue(storage.getItem(key));
}

function resolveCookieToken(cookieString, key) {
  if (!cookieString || typeof cookieString !== 'string' || !key) {
    return '';
  }
  const segments = cookieString.split(';');
  for (const segment of segments) {
    const [rawKey, ...rawValueParts] = segment.split('=');
    if (normalizeTokenValue(rawKey) !== key) {
      continue;
    }
    return normalizeTokenValue(decodeCookieValue(rawValueParts.join('=')));
  }
  return '';
}

export function readAddressToken(storage) {
  return resolveStorageToken(storage, ADDRESS_TOKEN_STORAGE_KEY);
}

export function writeAddressToken(storage, token) {
  if (!storage || typeof storage.setItem !== 'function') {
    return;
  }
  storage.setItem(ADDRESS_TOKEN_STORAGE_KEY, normalizeTokenValue(token));
}

export function clearAddressToken(storage) {
  if (!storage || typeof storage.removeItem !== 'function') {
    return;
  }
  storage.removeItem(ADDRESS_TOKEN_STORAGE_KEY);
}

export function resolveLegacyRefreshToken({ storage, cookieString = '' } = {}) {
  for (const key of LEGACY_TOKEN_STORAGE_KEYS) {
    const storageToken = resolveStorageToken(storage, key);
    if (storageToken) {
      return storageToken;
    }
  }
  for (const key of LEGACY_TOKEN_COOKIE_KEYS) {
    const cookieToken = resolveCookieToken(cookieString, key);
    if (cookieToken) {
      return cookieToken;
    }
  }
  return '';
}

export function resolveInitialToken({ storage, cookieString = '' } = {}) {
  return readAddressToken(storage) || resolveLegacyRefreshToken({ storage, cookieString });
}
