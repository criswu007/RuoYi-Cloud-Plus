<template>
  <el-container class="layout">
    <el-aside width="252px" class="aside">
      <div class="logo-wrap">
        <div class="logo-mark">A</div>
        <div>
          <div class="logo-title">标准地址管理</div>
          <div class="logo-subtitle">RuoYi Address Studio</div>
        </div>
      </div>
      <el-menu :default-active="$route.path" router class="menu" background-color="transparent" text-color="#d6e1ee" active-text-color="#ffffff">
        <el-submenu index="standard">
          <template slot="title">标准地址管理</template>
          <el-menu-item index="/standard/list">标准地址列表</el-menu-item>
          <el-menu-item index="/standard/approvals">待审批地址管理</el-menu-item>
          <el-menu-item index="/standard/merge">地址合并</el-menu-item>
          <el-menu-item index="/standard/split">地址拆分</el-menu-item>
          <el-menu-item index="/installation/list">安装地址列表</el-menu-item>
          <el-menu-item index="/selection/tools">选址平台</el-menu-item>
          <el-menu-item index="/import/records">导入记录查询</el-menu-item>
          <el-menu-item index="/operation/logs">地址操作日志</el-menu-item>
          <el-menu-item index="/standard/labels">标签库管理</el-menu-item>
          <el-menu-item index="/management/station">管理站管理</el-menu-item>
        </el-submenu>
        <el-submenu index="monitor">
          <template slot="title">质量监控</template>
          <el-menu-item index="/monitor/records">异常地址</el-menu-item>
          <el-menu-item index="/monitor/rules">规则配置</el-menu-item>
          <el-menu-item index="/monitor/task">监控任务</el-menu-item>
        </el-submenu>
        <el-submenu index="ops">
          <template slot="title">运维支持</template>
          <el-menu-item index="/ops/search">ES 运维</el-menu-item>
        </el-submenu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-main">
          <div class="header-title">{{ pageTitle }}</div>
          <div class="env">
            MODE: {{ runtimeMode }} ｜ API_BASE: {{ apiBase }}
          </div>
        </div>
        <div class="token">
          <el-input
            v-model="token"
            size="small"
            placeholder="输入 Token（可选）"
            class="token-input"
            @change="saveToken"
          />
          <el-button size="small" type="primary" @click="saveToken">保存</el-button>
          <el-button size="small" @click="clearToken">清除</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
const storage = typeof localStorage === 'undefined'
  ? null
  : localStorage;

export default {
  data() {
    return {
      token: storage ? storage.getItem('AUTH_TOKEN') || '' : '',
      runtimeMode: import.meta.env.VITE_ADDRESS_RUNTIME_MODE || 'standalone',
      apiBase: import.meta.env.VITE_API_BASE || '代理 /address',
      titleMap: {
        '/standard/list': '标准地址列表',
        '/standard/detail': '标准地址详情',
        '/standard/approvals': '待审批地址管理',
        '/standard/merge': '标准地址合并',
        '/standard/split': '标准地址拆分',
        '/standard/labels': '标签库管理',
        '/import/records': '导入记录查询',
        '/operation/logs': '地址操作日志',
        '/installation/list': '安装地址列表',
        '/selection/tools': '选址平台',
        '/management/station': '管理站管理',
        '/monitor/records': '异常地址治理',
        '/monitor/rules': '非标监控规则',
        '/monitor/task': '监控任务摘要',
        '/ops/search': 'ES 运维'
      }
    };
  },
  computed: {
    pageTitle() {
      if (this.$route.path.startsWith('/standard/detail/')) {
        return this.titleMap['/standard/detail'];
      }
      return this.titleMap[this.$route.path] || '标准地址管理';
    }
  },
  methods: {
    saveToken() {
      if (storage) {
        storage.setItem('AUTH_TOKEN', this.token || '');
      }
      this.$message.success('Token 已保存');
    },
    clearToken() {
      this.token = '';
      if (storage) {
        storage.removeItem('AUTH_TOKEN');
      }
      this.$message.success('Token 已清除');
    }
  }
};
</script>

<style scoped>
.layout {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(191, 231, 205, 0.35), transparent 30%),
    linear-gradient(180deg, #f4f7f2 0%, #edf2f4 100%);
}
.aside {
  background:
    linear-gradient(180deg, rgba(14, 37, 55, 0.98) 0%, rgba(13, 28, 43, 0.98) 100%);
  color: #fff;
  box-shadow: 8px 0 24px rgba(12, 24, 37, 0.18);
}
.logo-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px 18px;
}
.logo-mark {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: linear-gradient(135deg, #79c49a 0%, #d9a75f 100%);
  color: #102433;
  font-size: 22px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.04em;
}
.logo-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(214, 225, 238, 0.72);
}
.menu {
  border-right: none;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 74px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(16, 36, 51, 0.08);
}
.main {
  padding: 20px;
}
.header-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.header-title {
  font-size: 22px;
  font-weight: 700;
  color: #102433;
  letter-spacing: 0.03em;
}
.env {
  color: #5f6f7f;
  font-size: 13px;
}
.token {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.token-input {
  width: 360px;
}
:deep(.el-submenu__title),
:deep(.el-menu-item) {
  height: 46px;
  line-height: 46px;
}
:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(121, 196, 154, 0.4), rgba(217, 167, 95, 0.16)) !important;
  border-right: 3px solid #d9a75f;
}

@media (max-width: 960px) {
  .token-input {
    width: 100%;
  }
  .header {
    align-items: flex-start;
    flex-direction: column;
    padding-top: 12px;
    padding-bottom: 12px;
  }
}
</style>
