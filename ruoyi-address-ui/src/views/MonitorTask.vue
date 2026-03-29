<template>
  <div class="monitor-task">
    <el-row :gutter="16" class="summary-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card">
          <div class="summary-label">规则总数</div>
          <div class="summary-value">{{ summary.totalRuleCount || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card emphasis">
          <div class="summary-label">启用规则</div>
          <div class="summary-value">{{ summary.enabledRuleCount || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card warning">
          <div class="summary-label">待处理异常</div>
          <div class="summary-value">{{ summary.pendingRecordCount || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card success">
          <div class="summary-label">最近新增</div>
          <div class="summary-value">{{ summary.lastCreatedCount || 0 }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card class="task-card">
      <div class="task-header">
        <div>
          <h3>监控任务摘要</h3>
          <p>当前版本已接入摘要统计与手动触发，用于支撑非标地址监控治理的首轮联调。</p>
        </div>
        <div class="task-actions">
          <el-button type="primary" :loading="executing" @click="executeNow">立即执行监控</el-button>
          <el-button plain @click="refresh">刷新摘要</el-button>
        </div>
      </div>

      <el-descriptions :column="2" border class="summary-desc">
        <el-descriptions-item label="已忽略异常">{{ summary.ignoredRecordCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="已处理异常">{{ summary.processedRecordCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="治理入口">
          <el-link type="primary" @click="$router.push('/monitor/records')">查看异常地址列表</el-link>
        </el-descriptions-item>
        <el-descriptions-item label="规则入口">
          <el-link type="primary" @click="$router.push('/monitor/rules')">查看规则配置</el-link>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script>
import { executeMonitorTask, getMonitorTaskSummary } from '../api/address';

export default {
  data() {
    return {
      summary: {},
      executing: false
    };
  },
  mounted() {
    this.refresh();
  },
  methods: {
    async refresh() {
      const res = await getMonitorTaskSummary();
      this.summary = res.data || {};
    },
    async executeNow() {
      this.executing = true;
      try {
        const res = await executeMonitorTask();
        const count = res.data || 0;
        this.$message.success(`监控执行完成，本次新增 ${count} 条异常记录`);
        this.refresh();
      } finally {
        this.executing = false;
      }
    }
  }
};
</script>

<style scoped>
.monitor-task {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.summary-row {
  margin-bottom: 0;
}
.summary-card {
  min-height: 136px;
  padding: 22px;
  border-radius: 22px;
  background: linear-gradient(145deg, #fffef9 0%, #f0f5f1 100%);
  box-shadow: 0 14px 34px rgba(16, 36, 51, 0.08);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.summary-card.emphasis {
  background: linear-gradient(145deg, #f3fbf6 0%, #e2f2e7 100%);
}
.summary-card.warning {
  background: linear-gradient(145deg, #fff6eb 0%, #f9ead6 100%);
}
.summary-card.success {
  background: linear-gradient(145deg, #edf7f1 0%, #dceddf 100%);
}
.summary-label {
  color: #5f6f7f;
  font-size: 14px;
}
.summary-value {
  font-size: 38px;
  font-weight: 700;
  color: #102433;
}
.task-card {
  border-radius: 22px;
}
.task-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  flex-wrap: wrap;
}
.task-header h3 {
  margin: 0 0 6px;
  font-size: 20px;
}
.task-header p {
  margin: 0;
  color: #5f6f7f;
}
.task-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.summary-desc {
  margin-top: 18px;
}
</style>
