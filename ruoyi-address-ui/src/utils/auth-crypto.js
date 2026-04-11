import CryptoJS from 'crypto-js';
import JSEncrypt from 'jsencrypt';

export const AUTH_ENCRYPT_HEADER = 'encrypt-key';
export const DEFAULT_LOGIN_PUBLIC_KEY = 'MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKoR8mX0rGKLqzcWmOzbfj64K8ZIgOdHnzkXSOVOZbFu/TJhZ7rFAN+eaGkl3C4buccQd/EjEsj9ir7ijT7h96MCAwEAAQ==';

const AES_KEY_LENGTH = 32;
const RANDOM_SOURCE = 'ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678';

function encodeBase64(value) {
  if (typeof Buffer !== 'undefined') {
    return Buffer.from(value, 'utf8').toString('base64');
  }
  if (typeof btoa === 'function') {
    return btoa(value);
  }
  throw new Error('当前环境不支持 Base64 编码');
}

function toPemPublicKey(publicKey) {
  if (!publicKey) {
    return '';
  }
  if (publicKey.includes('BEGIN PUBLIC KEY')) {
    return publicKey;
  }
  const normalized = publicKey.replace(/\s+/g, '');
  const chunks = normalized.match(/.{1,64}/g) || [];
  return [
    '-----BEGIN PUBLIC KEY-----',
    ...chunks,
    '-----END PUBLIC KEY-----'
  ].join('\n');
}

function createDefaultRsaEncryptor(publicKey) {
  const encryptor = new JSEncrypt();
  encryptor.setPublicKey(toPemPublicKey(publicKey));
  return value => encryptor.encrypt(value);
}

function createRandomKey() {
  let result = '';
  for (let index = 0; index < AES_KEY_LENGTH; index += 1) {
    const randomIndex = Math.floor(Math.random() * RANDOM_SOURCE.length);
    result += RANDOM_SOURCE[randomIndex];
  }
  return result;
}

function encryptByAes(payload, aesKey) {
  return CryptoJS.AES.encrypt(
    JSON.stringify(payload),
    CryptoJS.enc.Utf8.parse(aesKey),
    {
      mode: CryptoJS.mode.ECB,
      padding: CryptoJS.pad.Pkcs7
    }
  ).toString();
}

export function createLoginEncryptedPayload(payload, options = {}) {
  const aesKey = (options.randomKey || createRandomKey)();
  const rsaEncrypt = options.rsaEncrypt
    || createDefaultRsaEncryptor(
      options.publicKey
        || import.meta.env.VITE_AUTH_LOGIN_PUBLIC_KEY
        || DEFAULT_LOGIN_PUBLIC_KEY
    );
  const encryptKey = rsaEncrypt(encodeBase64(aesKey));

  if (!encryptKey) {
    throw new Error('登录请求加密失败，请检查前端公钥配置');
  }

  return {
    body: encryptByAes(payload, aesKey),
    encryptKey
  };
}
