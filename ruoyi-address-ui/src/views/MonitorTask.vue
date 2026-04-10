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
          <h3>监控任务管理</h3>
          <p>围绕规则模板、范围和执行状态管理非标地址巡检任务。</p>
        </div>
        <div class="task-actions">
          <el-button type="primary" :loading="executing" @click="executeNow">立即执行监控</el-button>
          <el-button type="success" @click="openEditor()">新增任务</el-button>
          <el-button plain @click="refreshAll">刷新</el-button>
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

    <el-card class="panel-card">
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="query.taskName" placeholder="任务名称" clearable @keyup.enter.native="fetchTasks" />
          <el-select v-model="query.taskType" placeholder="任务类型" clearable>
            <el-option label="手动任务" value="MANUAL" />
            <el-option label="定时任务" value="SCHEDULE" />
          </el-select>
          <el-select v-model="query.monitorScope" placeholder="监控范围" clearable>
            <el-option label="全量" value="ALL" />
            <el-option label="按区域" value="REGION" />
            <el-option label="按地址" value="ADDRESS" />
          </el-select>
          <el-select v-model="query.taskStatus" placeholder="任务状态" clearable>
            <el-option label="启用" value="ENABLED" />
            <el-option label="暂停" value="PAUSED" />
            <el-option label="终止" value="TERMINATED" />
          </el-select>
          <el-button type="primary" @click="fetchTasks">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="list" border stripe size="small">
        <el-table-column prop="taskName" label="任务名称" min-width="180" />
        <el-table-column prop="taskType" label="任务类型" width="110">
          <template #default="{ row }">
            {{ taskTypeLabelMap[row.taskType] || row.taskType || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="monitorScope" label="监控范围" width="110">
          <template #default="{ row }">
            {{ scopeLabelMap[row.monitorScope] || row.monitorScope || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="任务状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTypeMap[row.taskStatus] || 'info'">
              {{ statusLabelMap[row.taskStatus] || row.taskStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="executeRule" label="执行规则" min-width="160" show-overflow-tooltip />
        <el-table-column prop="snailJobTaskId" label="SnailJob ID" width="130" />
        <el-table-column prop="lastExecuteTime" label="最近执行" width="180" />
        <el-table-column prop="lastSuccessTime" label="最近成功" width="180" />
        <el-table-column prop="lastFailureReason" label="最近失败原因" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="390" fixed="right">
          <template #default="{ row }">
            <el-button size="mini" type="primary" plain @click="openEditor(row)">编辑</el-button>
            <el-button size="mini" plain @click="openRunLogs(row)">运行日志</el-button>
            <el-button size="mini" type="success" plain @click="rerun(row)">重跑</el-button>
            <el-button size="mini" type="warning" plain :disabled="row.taskStatus === 'PAUSED'" @click="pause(row)">暂停</el-button>
            <el-button size="mini" type="danger" plain :disabled="row.taskStatus === 'TERMINATED'" @click="terminate(row)">终止</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          :current-page="pageNum"
          :page-size="pageSize"
          @current-change="changePage"
        />
      </div>
    </el-card>

    <el-dialog :visible.sync="editorVisible" :title="editor.id ? '编辑任务' : '新增任务'" width="720px">
      <el-form ref="editorForm" :model="editor" :rules="rules" label-width="110px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="editor.taskName" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="任务类型" prop="taskType">
          <el-radio-group v-model="editor.taskType">
            <el-radio label="MANUAL">手动</el-radio>
            <el-radio label="SCHEDULE">定时</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="监控范围" prop="monitorScope">
          <el-radio-group v-model="editor.monitorScope">
            <el-radio label="ALL">全量</el-radio>
            <el-radio label="REGION">区域</el-radio>
            <el-radio label="ADDRESS">地址</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="editor.taskType === 'SCHEDULE'" label="执行规则" prop="executeRule">
          <el-input v-model="editor.executeRule" placeholder="例如：0 0/30 * * * ?" />
        </el-form-item>
        <el-form-item v-if="editor.monitorScope === 'REGION'" label="区域ID">
          <el-input v-model="editor.regionIdsText" type="textarea" :rows="3" placeholder="多个区域ID请用逗号分隔" />
        </el-form-item>
        <el-form-item v-if="editor.monitorScope === 'ADDRESS'" label="地址ID">
          <el-input v-model="editor.addressIdsText" type="textarea" :rows="3" placeholder="多个地址ID请用逗号分隔" />
        </el-form-item>
        <el-form-item label="规则ID">
          <el-input v-model="editor.relatedRuleIdsText" type="textarea" :rows="3" placeholder="多个规则ID请用逗号分隔" />
        </el-form-item>
        <el-form-item label="任务状态" prop="taskStatus">
          <el-radio-group v-model="editor.taskStatus">
            <el-radio label="ENABLED">启用</el-radio>
            <el-radio label="PAUSED">暂停</el-radio>
            <el-radio label="TERMINATED">终止</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="任务说明">
          <el-input v-model="editor.taskDesc" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-actions">
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEditor">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog :visible.sync="runLogVisible" :title="runLogTitle" width="920px">
      <el-table v-loading="runLogLoading" :data="runLogList" border stripe size="small">
        <el-table-column prop="id" label="日志ID" width="90" />
        <el-table-column prop="triggerMode" label="触发方式" width="100">
          <template #default="{ row }">
            {{ triggerModeLabelMap[row.triggerMode] || row.triggerMode || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="executeStatus" label="执行状态" width="110">
          <template #default="{ row }">
            <el-tag :type="runStatusTypeMap[row.executeStatus] || 'info'">
              {{ runStatusLabelMap[row.executeStatus] || row.executeStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scannedCount" label="扫描数" width="90" />
        <el-table-column prop="hitCount" label="命中数" width="90" />
        <el-table-column prop="createdCount" label="新增异常" width="100" />
        <el-table-column prop="startedTime" label="开始时间" width="170" />
        <el-table-column prop="finishedTime" label="结束时间" width="170" />
        <el-table-column prop="executeMessage" label="执行说明" min-width="220" show-overflow-tooltip />
      </el-table>
      <div class="pager">
        <el-pagination
          layout="total, prev, pager, next"
          :total="runLogTotal"
          :current-page="runLogPageNum"
          :page-size="runLogPageSize"
          @current-change="changeRunLogPage"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  createMonitorTask,
  executeMonitorTask,
  getMonitorTaskSummary,
  getMonitorTasks,
  getMonitorTaskRunLogs,
  pauseMonitorTask,
  rerunMonitorTask,
  terminateMonitorTask,
  updateMonitorTask
} from '../api/address';

function createEmptyEditor() {
  return {
    id: null,
    taskName: '',
    taskType: 'MANUAL',
    executeRule: '',
    monitorScope: 'ALL',
    taskStatus: 'ENABLED',
    taskDesc: '',
    relatedRuleIdsText: '',
    regionIdsText: '',
    addressIdsText: ''
  };
}

function parseIdArray(text, parser = value => value) {
  return String(text || '')
    .split(/[,\n]/)
    .map(item => item.trim())
    .filter(Boolean)
    .map(parser);
}

export default {
  data() {
    return {
      summary: {},
      executing: false,
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      query: {
        taskName: '',
        taskType: '',
        monitorScope: '',
        taskStatus: ''
      },
      editorVisible: false,
      editor: createEmptyEditor(),
      runLogVisible: false,
      runLogLoading: false,
      runLogTaskId: null,
      runLogTaskName: '',
      runLogList: [],
      runLogTotal: 0,
      runLogPageNum: 1,
      runLogPageSize: 10,
      rules: {
        taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
        taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
        monitorScope: [{ required: true, message: '请选择监控范围', trigger: 'change' }],
        taskStatus: [{ required: true, message: '请选择任务状态', trigger: 'change' }],
        executeRule: [{
          validator: (rule, value, callback) => {
            if (this.editor.taskType === 'SCHEDULE' && !value) {
              callback(new Error('定时任务需要填写执行规则'));
              return;
            }
            callback();
          },
          trigger: 'blur'
        }]
      },
      taskTypeLabelMap: {
        MANUAL: '手动任务',
        SCHEDULE: '定时任务'
      },
      scopeLabelMap: {
        ALL: '全量',
        REGION: '按区域',
        ADDRESS: '按地址'
      },
      statusLabelMap: {
        ENABLED: '启用',
        PAUSED: '暂停',
        TERMINATED: '终止'
      },
      statusTypeMap: {
        ENABLED: 'success',
        PAUSED: 'warning',
        TERMINATED: 'info'
      },
      triggerModeLabelMap: {
        MANUAL: '手动',
        SCHEDULE: '定时'
      },
      runStatusLabelMap: {
        RUNNING: '执行中',
        SUCCESS: '成功',
        SKIPPED: '跳过',
        FAILED: '失败',
        SUBMITTED: '已提交'
      },
      runStatusTypeMap: {
        RUNNING: 'warning',
        SUCCESS: 'success',
        SKIPPED: 'info',
        FAILED: 'danger',
        SUBMITTED: 'info'
      }
    };
  },
  computed: {
    runLogTitle() {
      return this.runLogTaskName ? `运行日志 - ${this.runLogTaskName}` : '运行日志';
    }
  },
  mounted() {
    this.refreshAll();
  },
  methods: {
    async refreshAll() {
      await Promise.all([this.refreshSummary(), this.fetchTasks()]);
    },
    async refreshSummary() {
      const res = await getMonitorTaskSummary();
      this.summary = res.data || {};
    },
    async fetchTasks() {
      const res = await getMonitorTasks({
        ...this.query,
        pageNum: this.pageNum,
        pageSize: this.pageSize
      });
      this.list = res.rows || [];
      this.total = res.total || 0;
    },
    resetQuery() {
      this.query = {
        taskName: '',
        taskType: '',
        monitorScope: '',
        taskStatus: ''
      };
      this.pageNum = 1;
      this.fetchTasks();
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchTasks();
    },
    async executeNow() {
      this.executing = true;
      try {
        const res = await executeMonitorTask();
        if (Number(res.data || 0) > 0) {
          this.$message.success('已提交后台执行，请稍后刷新结果');
        } else {
          this.$message.warning('后台巡检正在执行中，请勿重复提交');
        }
        this.refreshAll();
      } finally {
        this.executing = false;
      }
    },
    openEditor(row) {
      if (!row) {
        this.editor = createEmptyEditor();
      } else {
        this.editor = {
          ...createEmptyEditor(),
          ...row,
          relatedRuleIdsText: (row.relatedRuleIds || []).join(','),
          regionIdsText: (row.regionIds || []).join(','),
          addressIdsText: (row.addressIds || []).join(',')
        };
      }
      this.editorVisible = true;
      this.$nextTick(() => this.$refs.editorForm && this.$refs.editorForm.clearValidate());
    },
    buildPayload() {
      return {
        ...this.editor,
        relatedRuleIds: parseIdArray(this.editor.relatedRuleIdsText, value => Number(value)),
        regionIds: parseIdArray(this.editor.regionIdsText),
        addressIds: parseIdArray(this.editor.addressIdsText, value => Number(value))
      };
    },
    submitEditor() {
      this.$refs.editorForm.validate(async valid => {
        if (!valid) {
          return;
        }
        const payload = this.buildPayload();
        if (payload.id) {
          await updateMonitorTask(payload);
          this.$message.success('任务已更新');
        } else {
          await createMonitorTask(payload);
          this.$message.success('任务已创建');
        }
        this.editorVisible = false;
        this.refreshAll();
      });
    },
    async rerun(row) {
      await rerunMonitorTask(row.id);
      this.$message.success('任务已提交重跑');
      this.refreshAll();
      if (this.runLogVisible && this.runLogTaskId === row.id) {
        this.fetchRunLogs();
      }
    },
    async pause(row) {
      await pauseMonitorTask(row.id);
      this.$message.success('任务已暂停');
      this.fetchTasks();
    },
    async terminate(row) {
      await this.$confirm(`确认终止任务“${row.taskName}”吗？`, '提示', { type: 'warning' });
      await terminateMonitorTask(row.id);
      this.$message.success('任务已终止');
      this.fetchTasks();
    },
    openRunLogs(row) {
      this.runLogTaskId = row.id;
      this.runLogTaskName = row.taskName || '';
      this.runLogPageNum = 1;
      this.runLogVisible = true;
      this.fetchRunLogs();
    },
    async fetchRunLogs() {
      if (!this.runLogTaskId) {
        return;
      }
      this.runLogLoading = true;
      try {
        const res = await getMonitorTaskRunLogs(this.runLogTaskId, {
          pageNum: this.runLogPageNum,
          pageSize: this.runLogPageSize
        });
        this.runLogList = res.rows || [];
        this.runLogTotal = res.total || 0;
      } finally {
        this.runLogLoading = false;
      }
    },
    changeRunLogPage(page) {
      this.runLogPageNum = page;
      this.fetchRunLogs();
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
.task-card,
.panel-card,
.table-card {
  border-radius: 22px;
}
.task-header,
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
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
.task-actions,
.filters,
.dialog-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.summary-desc,
.table-card {
  margin-top: 16px;
}
.pager {
  margin-top: 16px;
  text-align: right;
}
</style>
