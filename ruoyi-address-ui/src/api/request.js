import axios from 'axios';
import { AUTH_CLIENT_ID_STORAGE_KEY } from '../utils/auth-session';

const SUCCESS_CODE = 200;
const DEFAULT_MICROSERVICE_PROXY_BASE = '/prod-api';
const DEFAULT_CLIENT_ID = import.meta.env.VITE_APP_CLIENT_ID || 'e5cd7e4891bf95d1d19206ce24a7b32e';
const ABSOLUTE_URL_PATTERN = /^[a-z][a-z\d+.-]*:\/\//i;
const ABSOLUTE_PROTOCOL_RELATIVE_PATTERN = /^\/\//;
const rawApiBase = import.meta.env.VITE_API_BASE || '';

const instance = axios.create({
  baseURL: ABSOLUTE_URL_PATTERN.test(rawApiBase) ? rawApiBase : '',
  timeout: 15000
});

function normalizeProxyBase(proxyBase) {
  if (!proxyBase || typeof proxyBase !== 'string') {
    return DEFAULT_MICROSERVICE_PROXY_BASE;
  }
  return proxyBase.endsWith('/') ? proxyBase.slice(0, -1) : proxyBase;
}

function isAbsoluteUrl(url) {
  return ABSOLUTE_URL_PATTERN.test(url) || ABSOLUTE_PROTOCOL_RELATIVE_PATTERN.test(url);
}

/**
 * 统一解析前端请求路径，确保微服务模式下所有浏览器请求先进入 nginx 的 prod-api，
 * 再由 nginx 转发到 gateway。
 *
 * @param {string} url 原始请求路径
 * @param {{ runtimeMode?: string, proxyBase?: string }} [options] 可选的运行态覆盖，供测试使用
 * @returns {string} 实际发起的请求路径
 */
export function resolveRequestUrl(url, options = {}) {
  if (!url || typeof url !== 'string' || isAbsoluteUrl(url)) {
    return url;
  }
  const normalizedUrl = url.startsWith('/') ? url : `/${url}`;
  const runtimeMode = options.runtimeMode
    || import.meta.env.VITE_ADDRESS_RUNTIME_MODE
    || 'standalone';
  if (runtimeMode !== 'microservice') {
    return normalizedUrl;
  }
  const proxyBase = normalizeProxyBase(
    options.proxyBase
      || import.meta.env.VITE_MICROSERVICE_PROXY_BASE
      || import.meta.env.VITE_API_BASE
      || DEFAULT_MICROSERVICE_PROXY_BASE
  );
  if (normalizedUrl === proxyBase || normalizedUrl.startsWith(`${proxyBase}/`)) {
    return normalizedUrl;
  }
  return `${proxyBase}${normalizedUrl}`;
}

function resolveFriendlyMessage(payload, fallback = '请求失败') {
  return payload?.msg
    || payload?.message
    || payload?.errorMsg
    || payload?.error
    || fallback;
}

function resolveBusinessCode(payload) {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    return null;
  }
  if (!Object.prototype.hasOwnProperty.call(payload, 'code')) {
    return null;
  }
  const numericCode = Number(payload.code);
  return Number.isNaN(numericCode) ? payload.code : numericCode;
}

function createBusinessError(payload, fallback = '请求失败') {
  const error = new Error(resolveFriendlyMessage(payload, fallback));
  error.friendlyMessage = resolveFriendlyMessage(payload, fallback);
  error.responseData = payload;
  return error;
}

function decodeBase64Url(value) {
  if (!value || typeof value !== 'string') {
    return '';
  }
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const paddingLength = (4 - (normalized.length % 4 || 4)) % 4;
  const padded = normalized + '='.repeat(paddingLength);
  if (typeof atob === 'function') {
    return atob(padded);
  }
  if (typeof Buffer !== 'undefined') {
    return Buffer.from(padded, 'base64').toString('utf8');
  }
  return '';
}

function resolveClientIdFromToken(token) {
  if (!token || typeof token !== 'string') {
    return '';
  }
  const normalizedToken = token.startsWith('Bearer ')
    ? token.slice('Bearer '.length).trim()
    : token.trim();
  const segments = normalizedToken.split('.');
  if (segments.length < 2) {
    return '';
  }
  try {
    const payload = JSON.parse(decodeBase64Url(segments[1]));
    return payload?.clientid || payload?.client_id || '';
  } catch (error) {
    return '';
  }
}

function readStorageValue(key) {
  if (typeof localStorage === 'undefined') {
    return '';
  }
  const value = localStorage.getItem(key);
  return typeof value === 'string'
    ? value.trim()
    : '';
}

async function unwrapResponseData(response) {
  if (response?.config?.responseType !== 'blob') {
    return response?.data;
  }
  const blob = response?.data;
  if (!(blob instanceof Blob)) {
    return blob;
  }
  const contentType = String(response?.headers?.['content-type'] || blob.type || '').toLowerCase();
  if (!contentType.includes('json') && !contentType.includes('text/plain')) {
    return blob;
  }
  const text = await blob.text();
  if (!text) {
    return blob;
  }
  try {
    return JSON.parse(text);
  } catch (err) {
    return blob;
  }
}

function ensureBusinessSuccess(data) {
  const code = resolveBusinessCode(data);
  if (code === null || code === SUCCESS_CODE) {
    return data;
  }
  throw createBusinessError(data);
}

instance.interceptors.request.use(config => {
  config.url = resolveRequestUrl(config.url);
  const headers = config.headers || {};
  config.headers = headers;
  headers.clientid = headers.clientid
    || readStorageValue(AUTH_CLIENT_ID_STORAGE_KEY)
    || DEFAULT_CLIENT_ID;
  if (headers.isToken === false) {
    return config;
  }
  const token = readStorageValue('AUTH_TOKEN');
  if (token) {
    headers.Authorization = token.startsWith('Bearer ')
      ? token
      : `Bearer ${token}`;
    const clientId = resolveClientIdFromToken(token) || readStorageValue(AUTH_CLIENT_ID_STORAGE_KEY);
    if (clientId) {
      headers.clientid = clientId;
    }
  }
  return config;
});

instance.interceptors.response.use(
  async response => {
    const data = await unwrapResponseData(response);
    return ensureBusinessSuccess(data);
  },
  error => {
    const msg = resolveFriendlyMessage(error?.response?.data, error?.message || '请求失败');
    error.friendlyMessage = msg;
    return Promise.reject(error);
  }
);

function toFormBody(data = {}) {
  const body = new URLSearchParams();
  Object.entries(data).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }
    if (Array.isArray(value)) {
      value.forEach(item => {
        if (item !== undefined && item !== null && item !== '') {
          body.append(key, item);
        }
      });
      return;
    }
    body.append(key, value);
  });
  return body;
}

const request = {
  get(url, config) {
    return instance.get(url, config);
  },
  post(url, data, config) {
    return instance.post(url, data, config);
  },
  put(url, data, config) {
    return instance.put(url, data, config);
  },
  delete(url, config) {
    return instance.delete(url, config);
  },
  postForm(url, data = {}, config = {}) {
    return instance.post(url, toFormBody(data), {
      ...config,
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        ...(config.headers || {})
      }
    });
  },
  postMultipart(url, data, config = {}) {
    return instance.post(url, data, {
      ...config,
      headers: {
        'Content-Type': 'multipart/form-data',
        ...(config.headers || {})
      }
    });
  },
  postDownload(url, data = {}, config = {}) {
    return instance.post(url, data, {
      ...config,
      responseType: 'blob'
    });
  }
};

export default request;
