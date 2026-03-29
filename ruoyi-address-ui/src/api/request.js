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

export default instance;
