<template>
  <div class="search-ops-console">
    <el-row :gutter="16" class="summary-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card">
          <div class="summary-label">ES 连通状态</div>
          <div class="summary-value">{{ overview.esReachable ? '健康' : '异常' }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card">
          <div class="summary-label">标准地址索引</div>
          <div class="summary-value text-value">{{ standardPhysicalIndexName }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card">
          <div class="summary-label">安装地址索引</div>
          <div class="summary-value text-value">{{ installationPhysicalIndexName }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card warning">
          <div class="summary-label">待处理 repair</div>
          <div class="summary-value">{{ overview.pendingRepairCount || 0 }}</div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {
  createInstallationRebuildTask,
  createStandardRebuildTask,
  executeSearchRepairTask,
  getSearchMaintenanceTasks,
  getSearchRepairTasks,
  getSearchOpsOverview
} from '../api/address';

export default {
  data() {
    return {
      overview: {},
      taskList: [],
      repairList: [],
      taskLoading: false,
      repairLoading: false,
      actionLoading: false,
      pollingTimer: null,
      taskPageNum: 1,
      taskPageSize: 10,
      repairPageNum: 1,
      repairPageSize: 10,
      taskQuery: {
        taskType: '',
        status: ''
      },
      repairQuery: {
        entityType: '',
        status: ''
      },
      standardConfirmationCode: '',
      installationConfirmationCode: '',
      repairConfirmationCode: ''
    };
  },
  computed: {
    standardPhysicalIndexName() {
      return this.overview.standardIndex && this.overview.standardIndex.physicalIndexName
        ? this.overview.standardIndex.physicalIndexName
        : '-';
    },
    installationPhysicalIndexName() {
      return this.overview.installationIndex && this.overview.installationIndex.physicalIndexName
        ? this.overview.installationIndex.physicalIndexName
        : '-';
    }
  },
  mounted() {
    this.refreshOverviewAndTasks();
    this.refreshRepairTasks();
    this.startPolling();
  },
  beforeDestroy() {
    this.stopPolling();
  },
  methods: {
    buildTaskQueryParams() {
      return {
        ...this.taskQuery,
        pageNum: this.taskPageNum,
        pageSize: this.taskPageSize
      };
    },
    buildRepairQueryParams() {
      return {
        ...this.repairQuery,
        pageNum: this.repairPageNum,
        pageSize: this.repairPageSize
      };
    },
    startPolling() {
      if (typeof this.stopPolling === 'function') {
        this.stopPolling();
      } else if (this.pollingTimer) {
        clearInterval(this.pollingTimer);
        this.pollingTimer = null;
      }
      this.pollingTimer = setInterval(() => {
        this.refreshOverviewAndTasks();
      }, 5000);
    },
    stopPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer);
        this.pollingTimer = null;
      }
    },
    async refreshOverviewAndTasks() {
      const [overviewRes, taskRes] = await Promise.all([
        getSearchOpsOverview(),
        getSearchMaintenanceTasks(this.buildTaskQueryParams())
      ]);
      this.overview = overviewRes.data || {};
      this.taskList = taskRes.rows || [];
    },
    async refreshRepairTasks() {
      const res = await getSearchRepairTasks(this.buildRepairQueryParams());
      this.repairList = res.rows || [];
    },
    async triggerStandardRebuild() {
      await createStandardRebuildTask({ confirmationCode: this.standardConfirmationCode });
      this.standardConfirmationCode = '';
      await this.refreshOverviewAndTasks();
    },
    async triggerInstallationRebuild() {
      await createInstallationRebuildTask({ confirmationCode: this.installationConfirmationCode });
      this.installationConfirmationCode = '';
      await this.refreshOverviewAndTasks();
    },
    async replayRepair(taskId) {
      await executeSearchRepairTask(taskId, { confirmationCode: this.repairConfirmationCode });
      this.repairConfirmationCode = '';
      await this.refreshRepairTasks();
    }
  }
};
</script>

<style scoped>
.search-ops-console {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-row {
  margin-bottom: 4px;
}

.summary-card {
  min-height: 112px;
  padding: 18px 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 30px rgba(16, 36, 51, 0.08);
}

.summary-card.warning {
  background: linear-gradient(135deg, rgba(255, 246, 230, 0.96), rgba(255, 255, 255, 0.92));
}

.summary-label {
  margin-bottom: 14px;
  color: #5f6f7f;
  font-size: 13px;
}

.summary-value {
  color: #102433;
  font-size: 26px;
  font-weight: 700;
}

.text-value {
  font-size: 18px;
  line-height: 1.5;
  word-break: break-all;
}
</style>
