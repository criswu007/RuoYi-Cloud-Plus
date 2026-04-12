<template>
  <div class="monitor-task-page">
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
          <div class="summary-label">最近新增异常</div>
          <div class="summary-value">{{ summary.lastCreatedCount || 0 }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card class="panel-card">
      <el-form :model="searchForm" size="default" label-width="100px" label-suffix="：">
        <el-row :gutter="10">
          <el-col :span="5">
            <el-form-item label="任务名称">
              <el-input
                v-model.trim="searchForm.taskName"
                placeholder="请输入任务名称"
                clearable
                @keyup.enter.native="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="任务类型">
              <el-select v-model="searchForm.taskType" placeholder="请选择任务类型" clearable class="full-width">
                <el-option label="全部" value="" />
                <el-option
                  v-for="item in taskTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="任务状态">
              <el-select v-model="searchForm.taskStatus" placeholder="请选择任务状态" clearable class="full-width">
                <el-option label="全部" value="" />
                <el-option label="运行中" value="ENABLED" />
                <el-option label="已暂停" value="PAUSED" />
                <el-option label="已终止" value="TERMINATED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4" class="search-buttons-col">
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" :loading="tableLoading" @click="handleSearch">查询</el-button>
              <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <div class="action-bar">
      <div class="operation-buttons">
        <el-button type="success" icon="el-icon-plus" size="small" @click="openScheduledDialog">新增定时任务</el-button>
        <el-button type="primary" icon="el-icon-video-play" size="small" @click="openManualTriggerDialog">手动触发监控</el-button>
        <el-button
          type="danger"
          icon="el-icon-close"
          size="small"
          :disabled="!canTerminateTask"
          @click="handleTerminateSelected"
        >
          终止任务
        </el-button>
      </div>
    </div>

    <el-card class="table-card">
      <el-table
        v-loading="tableLoading"
        class="list-table"
        :data="taskList"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="序号" width="80" align="center">
          <template #default="{ $index }">
            {{ indexMethod($index) }}
          </template>
        </el-table-column>
        <el-table-column prop="taskName" label="任务名称" min-width="220" show-overflow-tooltip />
        <el-table-column label="任务类型" width="120" align="center">
          <template #default="{ row }">
            {{ getTaskTypeLabel(row.taskType) }}
          </template>
        </el-table-column>
        <el-table-column label="执行规则" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ getExecuteRuleDisplay(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="lastExecuteTime" label="执行时间" width="180" align="center" />
        <el-table-column prop="processedCount" label="处理数据量" width="120" align="center">
          <template #default="{ row }">
            {{ row.processedCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="exceptionCount" label="识别异常数" width="120" align="center">
          <template #default="{ row }">
            <span :class="['exception-count', row.exceptionCount > 0 ? 'danger' : 'success']">
              {{ row.exceptionCount || 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getTaskStatusTagType(row.taskStatus)" size="small">
              {{ getTaskStatusLabel(row.taskStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" align="center" fixed="right">
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button size="small" plain @click="openDetailDialog(row)">详情</el-button>
              <el-button size="small" plain @click="openRunLogDialog(row)">日志</el-button>
              <el-button size="small" type="primary" plain @click="handleRerun(row)">重新执行</el-button>
              <el-button
                v-if="row.taskType === 'REALTIME'"
                size="small"
                :type="row.taskStatus === 'ENABLED' ? 'warning' : 'success'"
                plain
                @click="toggleRealtimeTask(row)"
              >
                {{ row.taskStatus === 'ENABLED' ? '暂停' : '启动' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        :page-sizes="[10, 20, 50, 70, 100]"
        layout="total, sizes, ->, prev, pager, next, jumper"
        :hide-on-single-page="false"
        :page-size="pageSize"
        :current-page="pageNum"
        :total="total"
        background
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      />
    </el-card>

    <el-dialog
      :visible.sync="scheduledTaskDialogVisible"
      title="新增定时任务"
      width="720px"
      :close-on-click-modal="false"
      :before-close="closeScheduledDialog"
      append-to-body
      destroy-on-close
    >
      <el-form ref="scheduledTaskFormRef" :model="scheduledTaskForm" :rules="scheduledTaskRules" label-width="120px" label-suffix="：">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务名称" prop="taskName">
              <el-input v-model.trim="scheduledTaskForm.taskName" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="执行规则" prop="executeRule">
              <el-input v-model.trim="scheduledTaskForm.executeRule" placeholder="例如：0 0/30 * * * ?" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="监控范围" prop="monitorScope">
              <el-select v-model="scheduledTaskForm.monitorScope" class="full-width" placeholder="请选择监控范围">
                <el-option v-for="item in monitorScopeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联规则" prop="relatedRuleIdsText">
              <el-select v-model="scheduledTaskForm.relatedRuleIdsText" multiple filterable class="full-width" placeholder="请选择关联规则">
                <el-option
                  v-for="item in enabledRuleOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="String(item.value)"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="任务描述">
          <el-input v-model.trim="scheduledTaskForm.taskDesc" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeScheduledDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitScheduledTask">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog
      :visible.sync="manualTriggerDialogVisible"
      title="手动触发监控"
      width="720px"
      :close-on-click-modal="false"
      :before-close="closeManualTriggerDialog"
      append-to-body
      destroy-on-close
    >
      <el-form ref="manualTriggerFormRef" :model="manualTriggerForm" :rules="manualTriggerRules" label-width="120px" label-suffix="：">
        <el-form-item label="监控范围" prop="monitorScope">
          <el-select v-model="manualTriggerForm.monitorScope" class="full-width" placeholder="请选择监控范围">
            <el-option v-for="item in monitorScopeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联规则" prop="relatedRuleIdsText">
          <el-select v-model="manualTriggerForm.relatedRuleIdsText" multiple filterable class="full-width" placeholder="请选择关联规则">
            <el-option
              v-for="item in enabledRuleOptions"
              :key="item.value"
              :label="item.label"
              :value="String(item.value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="manualTriggerForm.monitorScope === 'REGION'" label="区域ID">
          <el-input
            v-model.trim="manualTriggerForm.regionIdsText"
            type="textarea"
            :rows="3"
            placeholder="多个区域ID请使用逗号或换行分隔"
          />
        </el-form-item>
        <el-form-item v-if="manualTriggerForm.monitorScope === 'ADDRESS'" label="地址ID">
          <el-input
            v-model.trim="manualTriggerForm.addressIdsText"
            type="textarea"
            :rows="3"
            placeholder="多个地址ID请使用逗号或换行分隔"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeManualTriggerDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitManualTrigger">立即执行</el-button>
      </div>
    </el-dialog>

    <el-dialog :visible.sync="detailVisible" title="任务详情" width="760px" append-to-body>
      <el-descriptions v-if="currentTaskDetail" :column="2" border>
        <el-descriptions-item label="任务名称">{{ currentTaskDetail.taskName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="任务类型">{{ getTaskTypeLabel(currentTaskDetail.taskType) }}</el-descriptions-item>
        <el-descriptions-item label="任务状态">{{ getTaskStatusLabel(currentTaskDetail.taskStatus) }}</el-descriptions-item>
        <el-descriptions-item label="执行规则">{{ getExecuteRuleDisplay(currentTaskDetail) }}</el-descriptions-item>
        <el-descriptions-item label="监控范围">{{ getMonitorScopeLabel(currentTaskDetail.monitorScope) }}</el-descriptions-item>
        <el-descriptions-item label="最近执行">{{ currentTaskDetail.lastExecuteTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近成功">{{ currentTaskDetail.lastSuccessTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近失败原因">{{ currentTaskDetail.lastFailureReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联规则" :span="2">{{ getRelatedRulesDisplay(currentTaskDetail.relatedRuleIds) }}</el-descriptions-item>
        <el-descriptions-item label="任务描述" :span="2">{{ currentTaskDetail.taskDesc || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog :visible.sync="runLogDialogVisible" :title="runLogTitle" width="920px" append-to-body>
      <el-table v-loading="runLogLoading" :data="runLogList" border stripe size="small">
        <el-table-column prop="id" label="日志ID" width="90" />
        <el-table-column prop="triggerMode" label="触发方式" width="100" />
        <el-table-column prop="executeStatus" label="执行状态" width="110" />
        <el-table-column prop="scannedCount" label="扫描数" width="90" />
        <el-table-column prop="hitCount" label="命中数" width="90" />
        <el-table-column prop="createdCount" label="新增异常" width="100" />
        <el-table-column prop="startedTime" label="开始时间" width="170" />
        <el-table-column prop="finishedTime" label="结束时间" width="170" />
        <el-table-column prop="executeMessage" label="执行说明" min-width="220" show-overflow-tooltip />
      </el-table>
      <el-pagination
        class="pagination"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, ->, prev, pager, next"
        :hide-on-single-page="false"
        :page-size="runLogPageSize"
        :current-page="runLogPageNum"
        :total="runLogTotal"
        background
        @current-change="handleRunLogCurrentChange"
        @size-change="handleRunLogSizeChange"
      />
    </el-dialog>
  </div>
</template>

<script>
import {
  createMonitorTask,
  executeMonitorTask,
  getMonitorRules,
  getMonitorTaskDetail,
  getMonitorTaskRunLogs,
  getMonitorTaskSummary,
  getMonitorTasks,
  pauseMonitorTask,
  rerunMonitorTask,
  terminateMonitorTask,
  updateMonitorTask
} from '../api/address';

const TASK_TYPE_OPTIONS = [
  { label: '实时监控', value: 'REALTIME' },
  { label: '定时任务', value: 'SCHEDULE' },
  { label: '手动触发', value: 'MANUAL' }
];

const MONITOR_SCOPE_OPTIONS = [
  { label: '全量地址', value: 'ALL' },
  { label: '按区域', value: 'REGION' },
  { label: '按地址条目', value: 'ADDRESS' }
];

function createEmptyScheduledTaskForm() {
  return {
    taskName: '',
    executeRule: '',
    monitorScope: 'ALL',
    relatedRuleIdsText: [],
    taskDesc: ''
  };
}

function createEmptyManualTriggerForm() {
  return {
    monitorScope: 'ALL',
    relatedRuleIdsText: [],
    regionIdsText: '',
    addressIdsText: ''
  };
}

function parseIdArray(text, mapper = value => value) {
  return String(text || '')
    .split(/[,\n]/)
    .map(item => item.trim())
    .filter(Boolean)
    .map(mapper);
}

export default {
  name: 'MonitorTaskView',
  data() {
    return {
      summary: {},
      searchForm: {
        taskName: '',
        taskType: '',
        taskStatus: ''
      },
      taskList: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      tableLoading: false,
      selectedRows: [],
      taskTypeOptions: TASK_TYPE_OPTIONS,
      monitorScopeOptions: MONITOR_SCOPE_OPTIONS,
      enabledRuleOptions: [],
      scheduledTaskDialogVisible: false,
      scheduledTaskForm: createEmptyScheduledTaskForm(),
      scheduledTaskRules: {
        taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
        executeRule: [{ required: true, message: '请输入执行规则', trigger: 'blur' }],
        monitorScope: [{ required: true, message: '请选择监控范围', trigger: 'change' }],
        relatedRuleIdsText: [{ required: true, message: '请至少选择一个关联规则', trigger: 'change' }]
      },
      manualTriggerDialogVisible: false,
      manualTriggerForm: createEmptyManualTriggerForm(),
      manualTriggerRules: {
        monitorScope: [{ required: true, message: '请选择监控范围', trigger: 'change' }],
        relatedRuleIdsText: [{ required: true, message: '请至少选择一个关联规则', trigger: 'change' }]
      },
      submitting: false,
      detailVisible: false,
      currentTaskDetail: null,
      runLogDialogVisible: false,
      runLogLoading: false,
      runLogList: [],
      runLogTotal: 0,
      runLogPageNum: 1,
      runLogPageSize: 10,
      currentRunLogTask: null
    };
  },
  computed: {
    canTerminateTask() {
      return this.selectedRows.some(row => row.taskStatus !== 'TERMINATED');
    },
    runLogTitle() {
      return this.currentRunLogTask?.taskName
        ? `任务日志 - ${this.currentRunLogTask.taskName}`
        : '任务日志';
    }
  },
  mounted() {
    this.refreshAll();
  },
  methods: {
    indexMethod(index) {
      return (this.pageNum - 1) * this.pageSize + index + 1;
    },
    getTaskTypeLabel(value) {
      return this.taskTypeOptions.find(item => item.value === value)?.label || value || '-';
    },
    getTaskStatusLabel(value) {
      return {
        ENABLED: '运行中',
        PAUSED: '已暂停',
        TERMINATED: '已终止',
        COMPLETED: '已完成',
        FAILED: '执行失败'
      }[value] || value || '-';
    },
    getTaskStatusTagType(value) {
      return {
        ENABLED: 'success',
        PAUSED: 'warning',
        TERMINATED: 'info',
        COMPLETED: 'success',
        FAILED: 'danger'
      }[value] || 'info';
    },
    getExecuteRuleDisplay(row) {
      if (row?.taskType === 'REALTIME') {
        return '实时监控';
      }
      if (row?.taskType === 'MANUAL') {
        return '手动触发';
      }
      if (row?.taskType === 'SCHEDULE') {
        return row?.executeRule || '-';
      }
      return row?.executeRule || '-';
    },
    getMonitorScopeLabel(value) {
      return this.monitorScopeOptions.find(item => item.value === value)?.label || value || '-';
    },
    getRelatedRulesDisplay(ruleIds = []) {
      if (!Array.isArray(ruleIds) || !ruleIds.length) {
        return '-';
      }
      return ruleIds
        .map(ruleId => this.enabledRuleOptions.find(item => Number(item.value) === Number(ruleId))?.label || ruleId)
        .join('、');
    },
    buildQueryParams() {
      return {
        taskName: this.searchForm.taskName,
        taskType: this.searchForm.taskType,
        taskStatus: this.searchForm.taskStatus,
        pageNum: this.pageNum,
        pageSize: this.pageSize
      };
    },
    async refreshAll() {
      await Promise.all([
        this.fetchSummary(),
        this.fetchTasks(),
        this.loadEnabledRules()
      ]);
    },
    async fetchSummary() {
      const res = await getMonitorTaskSummary();
      this.summary = res.data || {};
    },
    async fetchTasks() {
      this.tableLoading = true;
      try {
        const res = await getMonitorTasks(this.buildQueryParams());
        this.taskList = res.rows || [];
        this.total = res.total || 0;
      } finally {
        this.tableLoading = false;
      }
    },
    async loadEnabledRules() {
      const res = await getMonitorRules({
        status: '0',
        pageNum: 1,
        pageSize: 200
      });
      this.enabledRuleOptions = (res.rows || []).map(item => ({
        label: item.name,
        value: item.id
      }));
    },
    handleSearch() {
      this.pageNum = 1;
      this.fetchTasks();
    },
    handleReset() {
      this.searchForm = {
        taskName: '',
        taskType: '',
        taskStatus: ''
      };
      this.handleSearch();
    },
    handleCurrentChange(page) {
      this.pageNum = page;
      this.fetchTasks();
    },
    handleSizeChange(size) {
      this.pageSize = size;
      this.pageNum = 1;
      this.fetchTasks();
    },
    handleSelectionChange(rows) {
      this.selectedRows = rows;
    },
    openScheduledDialog() {
      this.scheduledTaskForm = createEmptyScheduledTaskForm();
      this.scheduledTaskDialogVisible = true;
      this.$nextTick(() => this.$refs.scheduledTaskFormRef && this.$refs.scheduledTaskFormRef.clearValidate());
    },
    closeScheduledDialog() {
      this.scheduledTaskDialogVisible = false;
      this.scheduledTaskForm = createEmptyScheduledTaskForm();
    },
    buildScheduledTaskPayload() {
      return {
        taskName: this.scheduledTaskForm.taskName,
        taskType: 'SCHEDULE',
        executeRule: this.scheduledTaskForm.executeRule,
        monitorScope: this.scheduledTaskForm.monitorScope,
        relatedRuleIds: this.scheduledTaskForm.relatedRuleIdsText.map(value => Number(value)),
        taskStatus: 'ENABLED',
        taskDesc: this.scheduledTaskForm.taskDesc
      };
    },
    submitScheduledTask() {
      this.$refs.scheduledTaskFormRef.validate(async valid => {
        if (!valid) {
          return;
        }
        this.submitting = true;
        try {
          await createMonitorTask(this.buildScheduledTaskPayload());
          this.$message.success('新增定时任务成功');
          this.closeScheduledDialog();
          this.refreshAll();
        } finally {
          this.submitting = false;
        }
      });
    },
    openManualTriggerDialog() {
      this.manualTriggerForm = createEmptyManualTriggerForm();
      this.manualTriggerDialogVisible = true;
      this.$nextTick(() => this.$refs.manualTriggerFormRef && this.$refs.manualTriggerFormRef.clearValidate());
    },
    closeManualTriggerDialog() {
      this.manualTriggerDialogVisible = false;
      this.manualTriggerForm = createEmptyManualTriggerForm();
    },
    buildManualTaskPayload() {
      return {
        taskName: `手动触发监控任务-${Date.now()}`,
        taskType: 'MANUAL',
        executeRule: '',
        monitorScope: this.manualTriggerForm.monitorScope,
        relatedRuleIds: this.manualTriggerForm.relatedRuleIdsText.map(value => Number(value)),
        regionIds: parseIdArray(this.manualTriggerForm.regionIdsText),
        addressIds: parseIdArray(this.manualTriggerForm.addressIdsText, value => Number(value)),
        taskStatus: 'ENABLED',
        taskDesc: '手动触发的非标地址监控任务'
      };
    },
    submitManualTrigger() {
      this.$refs.manualTriggerFormRef.validate(async valid => {
        if (!valid) {
          return;
        }
        this.submitting = true;
        try {
          await createMonitorTask(this.buildManualTaskPayload());
          const res = await executeMonitorTask();
          if (Number(res.data || 0) > 0) {
            this.$message.success('任务已开始执行');
          } else {
            this.$message.warning('后台巡检正在执行中，请稍后再试');
          }
          this.closeManualTriggerDialog();
          this.refreshAll();
        } finally {
          this.submitting = false;
        }
      });
    },
    async handleTerminateSelected() {
      const rows = this.selectedRows.filter(row => row.taskStatus !== 'TERMINATED');
      if (!rows.length) {
        this.$message.warning('请先选择可终止的任务');
        return;
      }
      await this.$confirm(`确定要终止选中的 ${rows.length} 个任务吗？`, '确认终止', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      });
      await Promise.all(rows.map(row => terminateMonitorTask(row.id)));
      this.$message.success('终止任务成功');
      this.selectedRows = [];
      this.fetchTasks();
    },
    async handleRerun(row) {
      await rerunMonitorTask(row.id);
      this.$message.success('任务已重新执行');
      this.fetchTasks();
      if (this.runLogDialogVisible && this.currentRunLogTask?.id === row.id) {
        this.fetchRunLogs();
      }
    },
    async toggleRealtimeTask(row) {
      if (row.taskStatus === 'ENABLED') {
        await pauseMonitorTask(row.id);
        this.$message.success('任务已暂停');
      } else {
        await updateMonitorTask({
          ...row,
          taskStatus: 'ENABLED',
          relatedRuleIds: row.relatedRuleIds || [],
          addressIds: row.addressIds || [],
          regionIds: row.regionIds || []
        });
        this.$message.success('任务已启动');
      }
      this.fetchTasks();
    },
    async openDetailDialog(row) {
      const res = await getMonitorTaskDetail(row.id);
      this.currentTaskDetail = res.data || row;
      this.detailVisible = true;
    },
    openRunLogDialog(row) {
      this.currentRunLogTask = row;
      this.runLogPageNum = 1;
      this.runLogDialogVisible = true;
      this.fetchRunLogs();
    },
    async fetchRunLogs() {
      if (!this.currentRunLogTask?.id) {
        return;
      }
      this.runLogLoading = true;
      try {
        const res = await getMonitorTaskRunLogs(this.currentRunLogTask.id, {
          pageNum: this.runLogPageNum,
          pageSize: this.runLogPageSize
        });
        this.runLogList = res.rows || [];
        this.runLogTotal = res.total || 0;
      } finally {
        this.runLogLoading = false;
      }
    },
    handleRunLogCurrentChange(page) {
      this.runLogPageNum = page;
      this.fetchRunLogs();
    },
    handleRunLogSizeChange(size) {
      this.runLogPageSize = size;
      this.runLogPageNum = 1;
      this.fetchRunLogs();
    }
  }
};
</script>

<style scoped>
.monitor-task-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-row {
  margin-bottom: 0;
}

.summary-card {
  min-height: 132px;
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

.panel-card,
.table-card {
  border-radius: 22px;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.operation-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.full-width {
  width: 100%;
}

.search-buttons-col {
  display: flex;
  align-items: center;
}

.exception-count.danger {
  color: #f56c6c;
}

.exception-count.success {
  color: #67c23a;
}

.pagination {
  margin-top: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
