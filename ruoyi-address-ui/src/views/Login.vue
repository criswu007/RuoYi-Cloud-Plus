<template>
  <div class="login-page">
    <div class="login-hero">
      <div class="hero-badge">RuoYi Address Studio</div>
      <h1>标准地址管理登录</h1>
      <p>
        前端请求继续统一走 `nginx /prod-api` 转发到 gateway，
        这里只补上标准登录入口，让 token 通过正常认证链路生成。
      </p>
    </div>
    <div class="login-panel">
      <div class="panel-header">
        <div class="panel-title">账号登录</div>
        <div class="panel-subtitle">登录后自动进入标准地址列表</div>
      </div>
      <el-form label-position="top" @submit.native.prevent="submitLogin">
        <el-form-item v-if="tenantEnabled" label="租户">
          <el-select
            v-model="form.tenantId"
            filterable
            placeholder="请选择租户"
            class="full-width"
            :loading="tenantLoading"
          >
            <el-option
              v-for="tenant in tenantOptions"
              :key="tenant.tenantId"
              :label="tenant.companyName || tenant.tenantId"
              :value="tenant.tenantId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input
            v-model.trim="form.username"
            placeholder="请输入用户名"
            autocomplete="username"
            @keyup.enter.native="submitLogin"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            autocomplete="current-password"
            @keyup.enter.native="submitLogin"
          />
        </el-form-item>
        <el-form-item v-if="captchaEnabled" label="验证码">
          <div class="captcha-row">
            <el-input
              v-model.trim="form.code"
              maxlength="8"
              placeholder="请输入验证码"
              @keyup.enter.native="submitLogin"
            />
            <button
              type="button"
              class="captcha-image"
              :disabled="captchaLoading"
              @click="loadCaptcha"
            >
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
              <span v-else>刷新验证码</span>
            </button>
          </div>
        </el-form-item>
        <div class="form-toolbar">
          <el-checkbox v-model="form.rememberMe">记住用户名和租户</el-checkbox>
          <el-button type="text" @click="loadCaptcha">刷新验证码</el-button>
        </div>
        <el-button
          type="primary"
          class="submit-button"
          :loading="submitting"
          @click="submitLogin"
        >
          登录并进入系统
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script>
import { getCaptcha, getTenantList, getUserInfo, loginByPassword } from '../api/auth';
import {
  persistAuthSession,
  readRememberedLogin,
  writeRememberedLogin
} from '../utils/auth-session';

const storage = typeof localStorage === 'undefined'
  ? null
  : localStorage;
const DEFAULT_REDIRECT_PATH = '/standard/list';
const DEFAULT_TENANT_ID = '000000';
const DEFAULT_USERNAME = 'admin';
const DEFAULT_PASSWORD = 'admin123';

function resolveImageSource(base64Image) {
  if (!base64Image) {
    return '';
  }
  return base64Image.startsWith('data:')
    ? base64Image
    : `data:image/png;base64,${base64Image}`;
}

export default {
  name: 'LoginView',
  data() {
    const remembered = readRememberedLogin(storage);
    return {
      submitting: false,
      captchaLoading: false,
      tenantLoading: false,
      tenantEnabled: false,
      tenantOptions: [],
      captchaEnabled: true,
      captchaImage: '',
      form: {
        tenantId: remembered.tenantId || DEFAULT_TENANT_ID,
        username: remembered.username || DEFAULT_USERNAME,
        password: remembered.password || DEFAULT_PASSWORD,
        code: '',
        uuid: '',
        rememberMe: remembered.rememberMe
      }
    };
  },
  created() {
    this.bootstrap();
  },
  methods: {
    async bootstrap() {
      await Promise.all([
        this.loadTenantList(),
        this.loadCaptcha()
      ]);
    },
    resolveRedirect() {
      const redirect = this.$route?.query?.redirect;
      if (typeof redirect === 'string' && redirect.startsWith('/')) {
        return redirect;
      }
      return DEFAULT_REDIRECT_PATH;
    },
    validateForm() {
      if (this.tenantEnabled && !this.form.tenantId) {
        return '请选择租户';
      }
      if (!this.form.username) {
        return '请输入用户名';
      }
      if (!this.form.password) {
        return '请输入密码';
      }
      if (this.captchaEnabled && !this.form.code) {
        return '请输入验证码';
      }
      return '';
    },
    async loadTenantList() {
      this.tenantLoading = true;
      try {
        const response = await getTenantList();
        const payload = response?.data || {};
        this.tenantEnabled = Boolean(payload.tenantEnabled);
        this.tenantOptions = Array.isArray(payload.voList)
          ? payload.voList
          : [];
        if (this.tenantEnabled && this.tenantOptions.length > 0 && !this.form.tenantId) {
          this.form.tenantId = this.tenantOptions[0].tenantId;
        }
        if (!this.tenantEnabled) {
          this.form.tenantId = '';
        }
      } catch (error) {
        this.$message.error(error.friendlyMessage || error.message || '租户信息加载失败');
      } finally {
        this.tenantLoading = false;
      }
    },
    async loadCaptcha() {
      this.captchaLoading = true;
      try {
        const response = await getCaptcha();
        const payload = response?.data || {};
        this.captchaEnabled = payload.captchaEnabled !== false;
        this.form.uuid = payload.uuid || '';
        this.form.code = '';
        this.captchaImage = resolveImageSource(payload.img);
      } catch (error) {
        this.$message.error(error.friendlyMessage || error.message || '验证码加载失败');
      } finally {
        this.captchaLoading = false;
      }
    },
    async submitLogin() {
      const validationMessage = this.validateForm();
      if (validationMessage) {
        this.$message.warning(validationMessage);
        return;
      }
      this.submitting = true;
      try {
        const response = await loginByPassword(this.form);
        const session = response?.data || {};
        persistAuthSession(storage, {
          accessToken: session.access_token || session.accessToken,
          refreshToken: session.refresh_token || session.refreshToken,
          clientId: session.client_id || session.clientId
        });
        await getUserInfo();
        writeRememberedLogin(storage, this.form);
        this.$message.success('登录成功');
        this.$router.replace(this.resolveRedirect());
      } catch (error) {
        if (this.captchaEnabled) {
          await this.loadCaptcha();
        }
        this.$message.error(error.friendlyMessage || error.message || '登录失败');
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 420px);
  background:
    radial-gradient(circle at top left, rgba(121, 196, 154, 0.28), transparent 34%),
    radial-gradient(circle at bottom right, rgba(217, 167, 95, 0.18), transparent 28%),
    linear-gradient(135deg, #f4f7f2 0%, #e8eef1 100%);
}

.login-hero {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 64px 72px;
  color: #102433;
}

.hero-badge {
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(16, 36, 51, 0.08);
  color: #2b5467;
  font-size: 13px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.login-hero h1 {
  margin: 24px 0 16px;
  font-size: 42px;
  line-height: 1.15;
}

.login-hero p {
  max-width: 560px;
  margin: 0;
  font-size: 16px;
  line-height: 1.8;
  color: #4d6271;
}

.login-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 40px 36px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(18px);
  box-shadow: -12px 0 32px rgba(16, 36, 51, 0.08);
}

.panel-header {
  margin-bottom: 24px;
}

.panel-title {
  font-size: 28px;
  font-weight: 700;
  color: #102433;
}

.panel-subtitle {
  margin-top: 8px;
  color: #637888;
  font-size: 14px;
}

.full-width {
  width: 100%;
}

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 12px;
}

.captcha-image {
  border: 1px solid rgba(16, 36, 51, 0.08);
  border-radius: 14px;
  background: #f6faf8;
  cursor: pointer;
  overflow: hidden;
  min-height: 40px;
}

.captcha-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.submit-button {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #2d6a4f 0%, #4a9a70 100%);
  box-shadow: 0 14px 24px rgba(45, 106, 79, 0.18);
}

@media (max-width: 960px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-hero {
    padding: 48px 24px 12px;
  }

  .login-panel {
    padding: 24px;
    background: transparent;
    box-shadow: none;
  }
}

@media (max-width: 640px) {
  .login-hero h1 {
    font-size: 32px;
  }

  .captcha-row {
    grid-template-columns: 1fr;
  }
}
</style>
