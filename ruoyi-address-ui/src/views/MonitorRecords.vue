<template>
  <div class="abnormal-address-page">
    <el-card class="panel-card">
      <el-form :model="searchForm" size="default" label-width="100px" label-suffix="：">
        <el-row :gutter="10">
          <el-col :span="5">
            <el-form-item label="异常地址">
              <el-input
                v-model.trim="searchForm.originalAddress"
                placeholder="请输入异常地址"
                clearable
                @keyup.enter.native="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="异常类型">
              <el-select v-model="searchForm.exceptionType" placeholder="请选择异常类型" clearable class="full-width">
                <el-option label="全部" value="" />
                <el-option
                  v-for="item in exceptionTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="处理状态">
              <el-select v-model="searchForm.processStatus" placeholder="请选择处理状态" clearable class="full-width">
                <el-option label="全部" value="" />
                <el-option
                  v-for="item in processStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
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
        <el-button
          type="warning"
          icon="el-icon-close"
          size="small"
          :disabled="!canBatchIgnore"
          @click="handleBatchIgnore"
        >
          批量忽略
        </el-button>
        <el-button
          type="primary"
          icon="el-icon-document-add"
          size="small"
          :disabled="!canBatchGenerateWorkOrder"
          @click="openWorkOrderDialog()"
        >
          生成工单
        </el-button>
      </div>
    </div>

    <el-card class="table-card">
      <el-table
        v-loading="tableLoading"
        class="list-table"
        :data="list"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" :selectable="isRowSelectable" />
        <el-table-column label="序号" width="80" align="center">
          <template #default="{ $index }">
            {{ indexMethod($index) }}
          </template>
        </el-table-column>
        <el-table-column prop="standardAddressFullName" label="异常地址" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ resolveAddressName(row) }}
          </template>
        </el-table-column>
        <el-table-column label="异常类型" width="140" align="center">
          <template #default="{ row }">
            {{ getExceptionTypeLabel(resolveExceptionTypeByRuleTemplate(row.ruleTemplateSnapshot)) }}
          </template>
        </el-table-column>
        <el-table-column prop="lastDetectedTime" label="监控时间" width="180" align="center">
          <template #default="{ row }">
            {{ row.lastDetectedTime || row.firstDetectedTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="hitCount" label="命中次数" width="100" align="center">
          <template #default="{ row }">
            {{ row.hitCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="处理状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getProcessStatusTagType(row.status)" size="small">
              {{ getProcessStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" align="center" fixed="right">
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button size="small" plain @click="viewDetail(row)">详情</el-button>
              <el-button
                v-if="row.status === '0'"
                size="small"
                type="primary"
                plain
                @click="openWorkOrderDialog(row)"
              >
                生成工单
              </el-button>
              <el-button
                v-if="row.status === '0'"
                size="small"
                plain
                @click="ignoreRow(row)"
              >
                忽略
              </el-button>
              <el-button
                v-if="row.workOrderId"
                size="small"
                type="success"
                plain
                @click="$router.push('/monitor/work-orders')"
              >
                前往工单
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

    <el-dialog :visible.sync="detailVisible" title="异常地址详情" width="860px" append-to-body>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="异常地址">{{ resolveAddressName(detail) }}</el-descriptions-item>
        <el-descriptions-item label="处理状态">{{ getProcessStatusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="异常类型">{{ getExceptionTypeLabel(resolveExceptionTypeByRuleTemplate(detail.ruleTemplateSnapshot)) }}</el-descriptions-item>
        <el-descriptions-item label="命中规则">{{ detail.ruleName || detail.ruleNameSnapshot || '-' }}</el-descriptions-item>
        <el-descriptions-item label="首次发现">{{ detail.firstDetectedTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近发现">{{ detail.lastDetectedTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="命中次数">{{ detail.hitCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="工单号">{{ detail.workOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="命中详情" :span="2">
          <pre class="json-block">{{ detail.hitDetailJson || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="处理备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog
      :visible.sync="workOrderDialogVisible"
      title="生成工单"
      width="620px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form :model="workOrderForm" label-width="100px" label-suffix="：">
        <el-form-item label="选中记录">
          <div class="summary-box">{{ pendingRows.length }} 条待处理异常地址</div>
        </el-form-item>
        <el-form-item label="异常地址">
          <div class="summary-box">{{ pendingRows.map(resolveAddressName).join('；') || '-' }}</div>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input
            v-model.trim="workOrderForm.remark"
            type="textarea"
            :rows="4"
            placeholder="可填写工单处理说明，当前版本仅做页面留存"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeWorkOrderDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitWorkOrder">确认生成</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  createMonitorWorkOrder,
  getMonitorRecordDetail,
  getMonitorRecords,
  ignoreMonitorRecord
} from '../api/address';

const EXCEPTION_TYPE_OPTIONS = [
  { label: '格式错误', value: 'formatError' },
  { label: '层级缺失', value: 'levelMissing' },
  { label: '行政区异常', value: 'adminDivisionError' }
];

const PROCESS_STATUS_OPTIONS = [
  { label: '待处理', value: '0' },
  { label: '已忽略', value: '1' },
  { label: '已生成工单', value: '2' },
  { label: '已修正', value: '3' },
  { label: '已驳回', value: '4' }
];

export default {
  name: 'MonitorRecordsView',
  data() {
    return {
      searchForm: {
        originalAddress: '',
        exceptionType: '',
        processStatus: ''
      },
      exceptionTypeOptions: EXCEPTION_TYPE_OPTIONS,
      processStatusOptions: PROCESS_STATUS_OPTIONS,
      rawList: [],
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      tableLoading: false,
      selectedRows: [],
      detailVisible: false,
      detail: null,
      workOrderDialogVisible: false,
      pendingRows: [],
      workOrderForm: {
        remark: ''
      },
      submitting: false
    };
  },
  computed: {
    canBatchIgnore() {
      return this.selectedRows.some(row => row.status === '0');
    },
    canBatchGenerateWorkOrder() {
      return this.selectedRows.some(row => row.status === '0');
    }
  },
  mounted() {
    this.fetchList();
  },
  methods: {
    indexMethod(index) {
      return (this.pageNum - 1) * this.pageSize + index + 1;
    },
    resolveRuleTemplateByExceptionType(exceptionType) {
      return {
        formatError: 'FORMAT_STANDARD',
        levelMissing: 'ELEMENT_COMPLETENESS',
        adminDivisionError: 'REGION_COMPLIANCE'
      }[exceptionType] || '';
    },
    resolveExceptionTypeByRuleTemplate(ruleTemplate) {
      return {
        FORMAT_STANDARD: 'formatError',
        ELEMENT_COMPLETENESS: 'levelMissing',
        REGION_COMPLIANCE: 'adminDivisionError'
      }[ruleTemplate] || '';
    },
    getExceptionTypeLabel(exceptionType) {
      return this.exceptionTypeOptions.find(item => item.value === exceptionType)?.label || exceptionType || '-';
    },
    getProcessStatusLabel(status) {
      return {
        '0': '待处理',
        '1': '已忽略',
        '2': '已生成工单',
        '3': '已修正',
        '4': '已驳回'
      }[status] || status || '-';
    },
    getProcessStatusTagType(status) {
      return {
        '0': 'danger',
        '1': 'info',
        '2': 'warning',
        '3': 'success',
        '4': 'info'
      }[status] || 'info';
    },
    resolveAddressName(row) {
      return row?.standardAddressFullName || row?.standNameSnapshot || row?.remark || '-';
    },
    buildRemoteParams() {
      return {
        status: this.searchForm.processStatus,
        pageNum: 1,
        pageSize: 500
      };
    },
    filterRecordsBySearch(records) {
      const keyword = this.searchForm.originalAddress.trim().toLowerCase();
      const exceptionType = this.searchForm.exceptionType;
      return (records || [])
        .filter(item => {
          if (!keyword) {
            return true;
          }
          return this.resolveAddressName(item).toLowerCase().includes(keyword);
        })
        .filter(item => {
          if (!exceptionType) {
            return true;
          }
          return this.resolveExceptionTypeByRuleTemplate(item.ruleTemplateSnapshot) === exceptionType;
        })
        .sort((left, right) => {
          const leftTime = new Date(left.lastDetectedTime || left.firstDetectedTime || 0).getTime();
          const rightTime = new Date(right.lastDetectedTime || right.firstDetectedTime || 0).getTime();
          return rightTime - leftTime;
        });
    },
    applyClientPaging(records) {
      this.total = records.length;
      const start = (this.pageNum - 1) * this.pageSize;
      this.list = records.slice(start, start + this.pageSize);
    },
    async fetchList() {
      this.tableLoading = true;
      try {
        const res = await getMonitorRecords(this.buildRemoteParams());
        this.rawList = res.rows || [];
        this.applyClientPaging(this.filterRecordsBySearch(this.rawList));
      } finally {
        this.tableLoading = false;
      }
    },
    handleSearch() {
      this.pageNum = 1;
      this.fetchList();
    },
    handleReset() {
      this.searchForm = {
        originalAddress: '',
        exceptionType: '',
        processStatus: ''
      };
      this.handleSearch();
    },
    handleCurrentChange(page) {
      this.pageNum = page;
      this.applyClientPaging(this.filterRecordsBySearch(this.rawList));
    },
    handleSizeChange(size) {
      this.pageSize = size;
      this.pageNum = 1;
      this.applyClientPaging(this.filterRecordsBySearch(this.rawList));
    },
    handleSelectionChange(rows) {
      this.selectedRows = rows;
    },
    isRowSelectable(row) {
      return row.status === '0';
    },
    async viewDetail(row) {
      const res = await getMonitorRecordDetail(row.id);
      this.detail = res.data || row;
      this.detailVisible = true;
    },
    async ignoreRow(row) {
      await ignoreMonitorRecord(row.id);
      this.$message.success('忽略成功');
      this.fetchList();
    },
    async handleBatchIgnore() {
      const rows = this.selectedRows.filter(row => row.status === '0');
      if (!rows.length) {
        this.$message.warning('请先选择待处理异常地址');
        return;
      }
      await ignoreMonitorRecord(rows.map(row => row.id).join(','));
      this.$message.success('批量忽略成功');
      this.selectedRows = [];
      this.fetchList();
    },
    openWorkOrderDialog(row) {
      const rows = row
        ? [row]
        : this.selectedRows.filter(item => item.status === '0');
      if (!rows.length) {
        this.$message.warning('请先选择待处理异常地址');
        return;
      }
      this.pendingRows = rows;
      this.workOrderForm = {
        remark: ''
      };
      this.workOrderDialogVisible = true;
    },
    closeWorkOrderDialog() {
      this.workOrderDialogVisible = false;
      this.pendingRows = [];
      this.workOrderForm = {
        remark: ''
      };
    },
    async submitWorkOrder() {
      if (!this.pendingRows.length) {
        return;
      }
      this.submitting = true;
      try {
        await createMonitorWorkOrder({
          abnormalWarningIds: this.pendingRows.map(row => row.id)
        });
        this.$message.success('工单生成成功');
        this.closeWorkOrderDialog();
        this.selectedRows = [];
        this.fetchList();
        this.$router.push('/monitor/work-orders');
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>

<style scoped>
.abnormal-address-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.pagination {
  margin-top: 16px;
}

.json-block {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: Menlo, Monaco, Consolas, monospace;
}

.summary-box {
  padding: 12px 14px;
  border-radius: 14px;
  background: linear-gradient(145deg, #f7fbf8 0%, #eef4f7 100%);
  border: 1px solid rgba(16, 36, 51, 0.08);
  color: #24384b;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
