<template>
  <div class="rule-configuration-page">
    <el-card class="panel-card">
      <el-form :model="searchForm" size="default" label-width="100px" label-suffix="：">
        <el-row :gutter="10">
          <el-col :span="5">
            <el-form-item label="规则名称">
              <el-input
                v-model.trim="searchForm.ruleName"
                placeholder="请输入规则名称"
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
            <el-form-item label="规则状态">
              <el-select v-model="searchForm.ruleStatus" placeholder="请选择规则状态" clearable class="full-width">
                <el-option label="全部" value="" />
                <el-option label="启用" value="0" />
                <el-option label="禁用" value="1" />
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
        <el-button type="success" icon="el-icon-plus" size="small" @click="openEditor()">新增规则</el-button>
        <el-button
          type="primary"
          icon="el-icon-check"
          size="small"
          :disabled="selectedRows.length === 0"
          @click="handleBatchEnable"
        >
          批量启用
        </el-button>
        <el-button
          type="info"
          icon="el-icon-close"
          size="small"
          :disabled="selectedRows.length === 0"
          @click="handleBatchDisable"
        >
          批量禁用
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
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="序号" width="80" align="center">
          <template #default="{ $index }">
            {{ indexMethod($index) }}
          </template>
        </el-table-column>
        <el-table-column prop="name" label="规则名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="规则类型" width="140" align="center">
          <template #default="{ row }">
            {{ getRuleTypeLabel(row.ruleTemplate) }}
          </template>
        </el-table-column>
        <el-table-column label="核心配置" min-width="250" show-overflow-tooltip>
          <template #default="{ row }">
            {{ getCoreConfigDisplay(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">
              {{ row.status === '0' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button size="small" plain @click="openEditor(row)">修改</el-button>
              <el-button
                size="small"
                :type="row.status === '0' ? 'warning' : 'success'"
                plain
                @click="toggleStatus(row)"
              >
                {{ row.status === '0' ? '禁用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" plain :disabled="row.status === '0'" @click="removeRow(row)">
                删除
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
      :visible.sync="editorVisible"
      :title="editorTitle"
      width="760px"
      :close-on-click-modal="false"
      :before-close="handleCancelDialog"
      append-to-body
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px" label-suffix="：">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="规则名称" prop="ruleName">
              <el-input v-model.trim="formData.ruleName" maxlength="100" show-word-limit placeholder="请输入规则名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规则类型" prop="ruleTemplate">
              <el-select v-model="formData.ruleTemplate" class="full-width" placeholder="请选择规则类型" @change="handleRuleTemplateChange">
                <el-option
                  v-for="item in ruleTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="规则状态" prop="ruleStatus">
              <el-select v-model="formData.ruleStatus" class="full-width" placeholder="请选择规则状态">
                <el-option label="启用" value="0" />
                <el-option label="禁用" value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="异常类型" prop="exceptionType">
              <el-select v-model="formData.exceptionType" class="full-width" placeholder="请选择异常类型" disabled>
                <el-option
                  v-for="item in exceptionTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-input-number v-model="formData.priority" :min="1" :max="999" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="去重窗口(h)">
              <el-input-number v-model="formData.dedupHours" :min="1" :max="720" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="规则配置" prop="ruleConfig">
          <div class="rule-config-container">
            <div v-if="formData.ruleTemplate === 'ELEMENT_COMPLETENESS'" class="config-section">
              <el-checkbox-group v-model="formData.ruleConfig.requiredLevels">
                <el-checkbox
                  v-for="item in requiredLevelOptions"
                  :key="item"
                  :label="item"
                >
                  {{ item }}
                </el-checkbox>
              </el-checkbox-group>
            </div>

            <div v-else-if="formData.ruleTemplate === 'FORMAT_STANDARD'" class="config-section">
              <el-form-item label="禁止包含字符" style="margin-bottom: 10px;">
                <el-input
                  v-model.trim="formData.ruleConfig.forbiddenChars"
                  placeholder="请输入禁止包含的字符，多个字符用逗号分隔"
                  maxlength="200"
                />
              </el-form-item>
              <el-form-item label="地址长度范围" style="margin-bottom: 0;">
                <el-input-number v-model="formData.ruleConfig.minLength" :min="0" :max="9999" style="width: 150px;" />
                <span class="range-separator">至</span>
                <el-input-number v-model="formData.ruleConfig.maxLength" :min="0" :max="9999" style="width: 150px;" />
              </el-form-item>
            </div>

            <div v-else-if="formData.ruleTemplate === 'REGION_COMPLIANCE'" class="config-section admin-division-config">
              <el-row :gutter="10">
                <el-col :span="8">
                  <el-form-item label="省" label-width="40px">
                    <el-input v-model.trim="formData.ruleConfig.province" placeholder="请输入省" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="市" label-width="40px">
                    <el-input v-model.trim="formData.ruleConfig.city" placeholder="请输入市" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="区/县" label-width="70px">
                    <el-input v-model.trim="formData.ruleConfig.district" placeholder="请输入区/县" />
                  </el-form-item>
                </el-col>
              </el-row>
            </div>

            <div v-else-if="formData.ruleTemplate === 'SMART_SUSPECT'" class="config-section">
              <el-input
                v-model.trim="formData.ruleConfig.expression"
                type="textarea"
                :rows="4"
                placeholder="请输入智能疑似检测表达式或说明"
              />
            </div>

            <div v-else class="config-placeholder">
              请先选择规则类型
            </div>
          </div>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model.trim="formData.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="handleCancelDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEditor">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  createMonitorRule,
  deleteMonitorRule,
  disableMonitorRule,
  enableMonitorRule,
  getMonitorRules,
  updateMonitorRule
} from '../api/address';

const RULE_TYPE_OPTIONS = [
  { label: '格式校验', value: 'FORMAT_STANDARD' },
  { label: '层级校验', value: 'ELEMENT_COMPLETENESS' },
  { label: '行政区校验', value: 'REGION_COMPLIANCE' },
  { label: '智能疑似校验', value: 'SMART_SUSPECT' }
];

const EXCEPTION_TYPE_OPTIONS = [
  { label: '格式错误', value: 'formatError' },
  { label: '层级缺失', value: 'levelMissing' },
  { label: '行政区异常', value: 'adminDivisionError' },
  { label: '智能疑似异常', value: 'smartSuspect' }
];

const REQUIRED_LEVEL_OPTIONS = ['省', '市', '区/县', '街道/乡镇', '社区/村'];

function createEmptyRuleConfig() {
  return {
    requiredLevels: [],
    forbiddenChars: '',
    minLength: null,
    maxLength: null,
    province: '',
    city: '',
    district: '',
    expression: ''
  };
}

function createEmptyForm() {
  return {
    id: null,
    ruleName: '',
    ruleTemplate: '',
    exceptionType: '',
    ruleStatus: '0',
    priority: 10,
    ruleCode: '',
    severity: 'MEDIUM',
    dedupHours: 24,
    ruleConfig: createEmptyRuleConfig(),
    remark: ''
  };
}

function safeParseConfigJson(configJson) {
  if (!configJson) {
    return createEmptyRuleConfig();
  }
  try {
    return {
      ...createEmptyRuleConfig(),
      ...JSON.parse(configJson)
    };
  } catch (error) {
    return createEmptyRuleConfig();
  }
}

export default {
  name: 'MonitorRulesView',
  data() {
    return {
      searchForm: {
        ruleName: '',
        exceptionType: '',
        ruleStatus: ''
      },
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      tableLoading: false,
      selectedRows: [],
      editorVisible: false,
      submitting: false,
      formData: createEmptyForm(),
      ruleTypeOptions: RULE_TYPE_OPTIONS,
      exceptionTypeOptions: EXCEPTION_TYPE_OPTIONS,
      requiredLevelOptions: REQUIRED_LEVEL_OPTIONS,
      formRules: {
        ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
        ruleTemplate: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
        ruleStatus: [{ required: true, message: '请选择规则状态', trigger: 'change' }]
      }
    };
  },
  computed: {
    editorTitle() {
      return this.formData.id ? '编辑规则' : '新增规则';
    }
  },
  mounted() {
    this.fetchList();
  },
  methods: {
    indexMethod(index) {
      return (this.pageNum - 1) * this.pageSize + index + 1;
    },
    getRuleTypeLabel(value) {
      return this.ruleTypeOptions.find(item => item.value === value)?.label || value || '-';
    },
    getExceptionTypeLabel(value) {
      return this.exceptionTypeOptions.find(item => item.value === value)?.label || value || '-';
    },
    resolveRuleTemplateByExceptionType(exceptionType) {
      return {
        formatError: 'FORMAT_STANDARD',
        levelMissing: 'ELEMENT_COMPLETENESS',
        adminDivisionError: 'REGION_COMPLIANCE',
        smartSuspect: 'SMART_SUSPECT'
      }[exceptionType] || '';
    },
    resolveExceptionTypeByRuleTemplate(ruleTemplate) {
      return {
        FORMAT_STANDARD: 'formatError',
        ELEMENT_COMPLETENESS: 'levelMissing',
        REGION_COMPLIANCE: 'adminDivisionError',
        SMART_SUSPECT: 'smartSuspect'
      }[ruleTemplate] || '';
    },
    getCoreConfigDisplay(row) {
      const config = safeParseConfigJson(row?.configJson);
      if (row?.ruleTemplate === 'FORMAT_STANDARD') {
        const parts = [];
        if (config.forbiddenChars) {
          parts.push(`禁止包含：${config.forbiddenChars}`);
        }
        if (config.minLength !== null || config.maxLength !== null) {
          parts.push(`长度范围：${config.minLength ?? 0}-${config.maxLength ?? '∞'}`);
        }
        return parts.length ? parts.join('；') : '未配置';
      }
      if (row?.ruleTemplate === 'ELEMENT_COMPLETENESS') {
        return Array.isArray(config.requiredLevels) && config.requiredLevels.length
          ? `必选层级：${config.requiredLevels.join('、')}`
          : '未配置';
      }
      if (row?.ruleTemplate === 'REGION_COMPLIANCE') {
        const parts = [config.province, config.city, config.district].filter(Boolean);
        return parts.length ? `行政区：${parts.join(' / ')}` : '未配置';
      }
      if (row?.ruleTemplate === 'SMART_SUSPECT') {
        return config.expression || '未配置';
      }
      return '未配置';
    },
    buildQueryParams() {
      return {
        name: this.searchForm.ruleName,
        ruleTemplate: this.resolveRuleTemplateByExceptionType(this.searchForm.exceptionType),
        status: this.searchForm.ruleStatus,
        pageNum: this.pageNum,
        pageSize: this.pageSize
      };
    },
    async fetchList() {
      this.tableLoading = true;
      try {
        const res = await getMonitorRules(this.buildQueryParams());
        this.list = res.rows || [];
        this.total = res.total || 0;
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
        ruleName: '',
        exceptionType: '',
        ruleStatus: ''
      };
      this.handleSearch();
    },
    handleCurrentChange(page) {
      this.pageNum = page;
      this.fetchList();
    },
    handleSizeChange(size) {
      this.pageSize = size;
      this.pageNum = 1;
      this.fetchList();
    },
    handleSelectionChange(rows) {
      this.selectedRows = rows;
    },
    batchUpdateStatus(targetStatus, successMessage) {
      const ids = this.selectedRows.map(row => row.id);
      if (!ids.length) {
        this.$message.warning('请先选择规则');
        return;
      }
      const request = targetStatus === '0'
        ? enableMonitorRule(ids.join(','))
        : disableMonitorRule(ids.join(','));
      request.then(() => {
        this.$message.success(successMessage);
        this.fetchList();
        this.selectedRows = [];
      });
    },
    handleBatchEnable() {
      this.batchUpdateStatus('0', '批量启用成功');
    },
    handleBatchDisable() {
      this.batchUpdateStatus('1', '批量禁用成功');
    },
    openEditor(row) {
      if (!row) {
        this.formData = createEmptyForm();
      } else {
        this.formData = {
          id: row.id,
          ruleName: row.name || '',
          ruleTemplate: row.ruleTemplate || '',
          exceptionType: this.resolveExceptionTypeByRuleTemplate(row.ruleTemplate),
          ruleStatus: row.status || '0',
          priority: row.priority || 10,
          ruleCode: row.ruleCode || '',
          severity: row.severity || 'MEDIUM',
          dedupHours: row.dedupHours || 24,
          ruleConfig: safeParseConfigJson(row.configJson),
          remark: row.remark || ''
        };
      }
      this.editorVisible = true;
      this.$nextTick(() => this.$refs.formRef && this.$refs.formRef.clearValidate());
    },
    handleRuleTemplateChange(value) {
      this.formData.exceptionType = this.resolveExceptionTypeByRuleTemplate(value);
      this.formData.ruleConfig = createEmptyRuleConfig();
    },
    buildRuleCode() {
      const prefix = this.formData.ruleTemplate || 'RULE';
      return `${prefix}_${Date.now()}`;
    },
    buildConfigJson() {
      const config = {
        ...createEmptyRuleConfig(),
        ...this.formData.ruleConfig
      };
      if (this.formData.ruleTemplate === 'ELEMENT_COMPLETENESS') {
        return JSON.stringify({
          requiredLevels: config.requiredLevels
        });
      }
      if (this.formData.ruleTemplate === 'FORMAT_STANDARD') {
        return JSON.stringify({
          forbiddenChars: config.forbiddenChars,
          minLength: config.minLength,
          maxLength: config.maxLength
        });
      }
      if (this.formData.ruleTemplate === 'REGION_COMPLIANCE') {
        return JSON.stringify({
          province: config.province,
          city: config.city,
          district: config.district
        });
      }
      return JSON.stringify({
        expression: config.expression
      });
    },
    buildPayload() {
      return {
        id: this.formData.id,
        name: this.formData.ruleName,
        ruleCode: this.formData.ruleCode || this.buildRuleCode(),
        ruleTemplate: this.formData.ruleTemplate,
        status: this.formData.ruleStatus,
        severity: this.formData.severity || 'MEDIUM',
        priority: this.formData.priority || 10,
        dedupHours: this.formData.dedupHours || 24,
        configJson: this.buildConfigJson(),
        remark: this.formData.remark
      };
    },
    submitEditor() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) {
          return;
        }
        this.submitting = true;
        try {
          const payload = this.buildPayload();
          if (payload.id) {
            await updateMonitorRule(payload);
            this.$message.success('规则已更新');
          } else {
            await createMonitorRule(payload);
            this.$message.success('规则已创建');
          }
          this.editorVisible = false;
          this.fetchList();
        } finally {
          this.submitting = false;
        }
      });
    },
    handleCancelDialog() {
      this.editorVisible = false;
      this.formData = createEmptyForm();
    },
    async toggleStatus(row) {
      if (row.status === '0') {
        await disableMonitorRule(row.id);
        this.$message.success('规则已禁用');
      } else {
        await enableMonitorRule(row.id);
        this.$message.success('规则已启用');
      }
      this.fetchList();
    },
    async removeRow(row) {
      await this.$confirm(`确定要删除规则“${row.name}”吗？`, '确认删除', {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      });
      await deleteMonitorRule(row.id);
      this.$message.success('删除成功');
      this.fetchList();
    }
  }
};
</script>

<style scoped>
.rule-configuration-page {
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

.search-buttons-col {
  display: flex;
  align-items: center;
}

.full-width {
  width: 100%;
}

.pagination {
  margin-top: 16px;
}

.rule-config-container {
  padding: 16px;
  border-radius: 16px;
  background: linear-gradient(145deg, #f7fbf8 0%, #eef4f7 100%);
  border: 1px solid rgba(16, 36, 51, 0.08);
}

.config-placeholder {
  color: #7a8a9a;
}

.range-separator {
  margin: 0 8px;
  color: #6a7b8c;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
