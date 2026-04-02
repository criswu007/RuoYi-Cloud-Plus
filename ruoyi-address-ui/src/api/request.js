import axios from 'axios';

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 15000
});

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
  response => response.data,
  error => {
    const msg = error?.response?.data?.msg
      || error?.response?.data?.message
      || error?.message
      || '请求失败';
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
