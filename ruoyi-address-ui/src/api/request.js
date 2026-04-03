import axios from 'axios';

const SUCCESS_CODE = 200;

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 15000
});

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
  const token = localStorage.getItem('AUTH_TOKEN');
  if (token) {
    config.headers.Authorization = token.startsWith('Bearer ')
      ? token
      : `Bearer ${token}`;
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
