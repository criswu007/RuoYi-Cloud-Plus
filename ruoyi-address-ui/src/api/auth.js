import request from './request';
import { AUTH_ENCRYPT_HEADER, createLoginEncryptedPayload } from '../utils/auth-crypto';

export const DEFAULT_AUTH_CLIENT_ID = 'e5cd7e4891bf95d1d19206ce24a7b32e';
export const DEFAULT_AUTH_GRANT_TYPE = 'password';

export const getCaptcha = () =>
  request.get('/auth/code', {
    headers: {
      isToken: false
    }
  });

export const getTenantList = () =>
  request.get('/auth/tenant/list', {
    headers: {
      isToken: false
    }
  });

export function loginByPassword(loginForm) {
  const encryptedPayload = createLoginEncryptedPayload({
    ...loginForm,
    clientId: DEFAULT_AUTH_CLIENT_ID,
    grantType: DEFAULT_AUTH_GRANT_TYPE
  });

  return request.post('/auth/login', encryptedPayload.body, {
    headers: {
      isToken: false,
      isEncrypt: true,
      repeatSubmit: false,
      [AUTH_ENCRYPT_HEADER]: encryptedPayload.encryptKey
    }
  });
}

export const getUserInfo = () => request.get('/system/user/getInfo');
