import CryptoJS from 'crypto-js';
import { describe, expect, it } from 'vitest';

import { createLoginEncryptedPayload } from './auth-crypto';

describe('登录请求加密', () => {
  it('应使用 AES-ECB 加密请求体，并使用 RSA 加密 Base64 AES 密钥', () => {
    const aesKey = '1234567890abcdef1234567890abcdef';
    const payload = createLoginEncryptedPayload(
      {
        username: 'admin',
        password: 'P@ssw0rd',
        tenantId: '000000',
        code: 'ABCD',
        uuid: 'captcha-uuid'
      },
      {
        randomKey: () => aesKey,
        rsaEncrypt: value => `rsa:${value}`
      }
    );

    const decrypted = CryptoJS.AES.decrypt(
      payload.body,
      CryptoJS.enc.Utf8.parse(aesKey),
      {
        mode: CryptoJS.mode.ECB,
        padding: CryptoJS.pad.Pkcs7
      }
    ).toString(CryptoJS.enc.Utf8);

    expect(JSON.parse(decrypted)).toEqual({
      username: 'admin',
      password: 'P@ssw0rd',
      tenantId: '000000',
      code: 'ABCD',
      uuid: 'captcha-uuid'
    });
    expect(payload.encryptKey).toBe(`rsa:${Buffer.from(aesKey, 'utf8').toString('base64')}`);
    expect(payload.body).not.toContain('admin');
  });
});
