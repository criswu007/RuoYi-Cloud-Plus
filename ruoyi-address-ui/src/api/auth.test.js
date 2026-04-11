import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockRequest, mockCreateLoginEncryptedPayload } = vi.hoisted(() => ({
  mockRequest: {
    get: vi.fn(),
    post: vi.fn()
  },
  mockCreateLoginEncryptedPayload: vi.fn(() => ({
    body: 'encrypted-body',
    encryptKey: 'rsa-encrypted-key'
  }))
}));

vi.mock('./request', () => ({
  default: mockRequest
}));

vi.mock('../utils/auth-crypto', () => ({
  AUTH_ENCRYPT_HEADER: 'encrypt-key',
  createLoginEncryptedPayload: mockCreateLoginEncryptedPayload
}));

import {
  DEFAULT_AUTH_CLIENT_ID,
  DEFAULT_AUTH_GRANT_TYPE,
  getCaptcha,
  getUserInfo,
  getTenantList,
  loginByPassword
} from './auth';

describe('认证 API 契约', () => {
  beforeEach(() => {
    Object.values(mockRequest).forEach(fn => fn.mockReset());
    mockCreateLoginEncryptedPayload.mockClear();
  });

  it('验证码接口应关闭 token 注入', () => {
    getCaptcha();

    expect(mockRequest.get).toHaveBeenCalledWith('/auth/code', {
      headers: {
        isToken: false
      }
    });
  });

  it('租户列表接口应关闭 token 注入', () => {
    getTenantList();

    expect(mockRequest.get).toHaveBeenCalledWith('/auth/tenant/list', {
      headers: {
        isToken: false
      }
    });
  });

  it('用户信息接口应走 /system/user/getInfo', () => {
    getUserInfo();

    expect(mockRequest.get).toHaveBeenCalledWith('/system/user/getInfo');
  });

  it('密码登录应补齐 clientId 和 grantType，并走加密请求头', () => {
    loginByPassword({
      tenantId: '000000',
      username: 'admin',
      password: '123456',
      code: 'ABCD',
      uuid: 'captcha-uuid',
      rememberMe: true
    });

    expect(mockCreateLoginEncryptedPayload).toHaveBeenCalledWith({
      tenantId: '000000',
      username: 'admin',
      password: '123456',
      code: 'ABCD',
      uuid: 'captcha-uuid',
      rememberMe: true,
      clientId: DEFAULT_AUTH_CLIENT_ID,
      grantType: DEFAULT_AUTH_GRANT_TYPE
    });
    expect(mockRequest.post).toHaveBeenCalledWith('/auth/login', 'encrypted-body', {
      headers: {
        isToken: false,
        isEncrypt: true,
        repeatSubmit: false,
        'encrypt-key': 'rsa-encrypted-key'
      }
    });
  });
});
