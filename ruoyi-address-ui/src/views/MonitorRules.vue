<template>
  <div>
    <el-card class="panel-card">
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="query.name" placeholder="规则名称" clearable @keyup.enter.native="fetchList" />
          <el-select v-model="query.ruleTemplate" placeholder="规则模板" clearable>
            <el-option v-for="item in templateOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="query.severity" placeholder="严重等级" clearable>
            <el-option v-for="item in severityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="query.status" placeholder="状态" clearable>
            <el-option label="启用" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
        <el-button type="success" @click="openEditor()">新增规则</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="list" border stripe size="small">
        <el-table-column prop="name" label="规则名称" min-width="160" />
        <el-table-column prop="ruleCode" label="规则编码" min-width="150" show-overflow-tooltip />
        <el-table-column label="规则模板" width="160">
          <template #default="{ row }">
            {{ templateLabel(row.ruleTemplate) }}
          </template>
        </el-table-column>
        <el-table-column label="严重等级" width="110">
          <template #default="{ row }">
            <el-tag :type="severityTypeMap[row.severity] || 'info'">
              {{ severityLabel(row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90" />
        <el-table-column prop="dedupHours" label="去重窗口" width="100">
          <template #default="{ row }">
            {{ row.dedupHours ? `${row.dedupHours}h` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">
              {{ row.status === '0' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="configJson" label="模板配置" min-width="240" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="mini" type="primary" plain @click="openEditor(row)">编辑</el-button>
            <el-button
              size="mini"
              :type="row.status === '0' ? 'warning' : 'success'"
              plain
              @click="toggleStatus(row)"
            >
              {{ row.status === '0' ? '停用' : '启用' }}
            </el-button>
            <el-button size="mini" type="danger" plain @click="removeRow(row)">删除</el-button>
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

    <el-dialog :visible.sync="editorVisible" :title="editor.id ? '编辑规则' : '新增规则'" width="700px">
      <el-form ref="editorForm" :model="editor" :rules="rules" label-width="110px">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="editor.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="规则编码" prop="ruleCode">
          <el-input v-model="editor.ruleCode" maxlength="64" show-word-limit placeholder="例如：FORMAT_STANDARD_001" />
        </el-form-item>
        <el-form-item label="规则模板" prop="ruleTemplate">
          <el-select v-model="editor.ruleTemplate" style="width: 100%">
            <el-option v-for="item in templateOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重等级" prop="severity">
          <el-radio-group v-model="editor.severity">
            <el-radio v-for="item in severityOptions" :key="item.value" :label="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="editor.priority" :min="1" :max="999" style="width: 180px" />
        </el-form-item>
        <el-form-item label="去重窗口" prop="dedupHours">
          <el-input-number v-model="editor.dedupHours" :min="1" :max="720" style="width: 180px" />
          <span class="inline-hint">小时</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editor.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="模板配置" prop="configJson">
          <el-input v-model="editor.configJson" type="textarea" :rows="6" placeholder='例如：{"maxNameLength":64,"allowPureNumber":false}' />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editor.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-actions">
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEditor">保存</el-button>
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

function createEmptyEditor() {
  return {
    id: null,
    name: '',
    ruleCode: '',
    ruleTemplate: 'FORMAT_STANDARD',
    severity: 'MEDIUM',
    priority: 10,
    dedupHours: 24,
    status: '0',
    configJson: '',
    remark: ''
  };
}

export default {
  data() {
    return {
      query: {
        name: '',
        ruleTemplate: '',
        severity: '',
        status: ''
      },
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      editorVisible: false,
      editor: createEmptyEditor(),
      templateOptions: [
        { label: '格式规范性检测', value: 'FORMAT_STANDARD' },
        { label: '行政区划合规性检测', value: 'REGION_COMPLIANCE' },
        { label: '地址要素完整性检测', value: 'ELEMENT_COMPLETENESS' },
        { label: '智能疑似异常检测', value: 'SMART_SUSPECT' }
      ],
      severityOptions: [
        { label: '高', value: 'HIGH' },
        { label: '中', value: 'MEDIUM' },
        { label: '低', value: 'LOW' }
      ],
      severityTypeMap: {
        HIGH: 'danger',
        MEDIUM: 'warning',
        LOW: 'info'
      },
      rules: {
        name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
        ruleCode: [{ required: true, message: '请输入规则编码', trigger: 'blur' }],
        ruleTemplate: [{ required: true, message: '请选择规则模板', trigger: 'change' }],
        severity: [{ required: true, message: '请选择严重等级', trigger: 'change' }],
        priority: [{ required: true, message: '请输入优先级', trigger: 'change' }],
        dedupHours: [{ required: true, message: '请输入去重窗口', trigger: 'change' }],
        configJson: [{ required: true, message: '请输入模板配置', trigger: 'blur' }]
      }
    };
  },
  mounted() {
    this.fetchList();
  },
  methods: {
    templateLabel(value) {
      return this.templateOptions.find(item => item.value === value)?.label || value || '-';
    },
    severityLabel(value) {
      return this.severityOptions.find(item => item.value === value)?.label || value || '-';
    },
    async fetchList() {
      const res = await getMonitorRules({
        ...this.query,
        pageNum: this.pageNum,
        pageSize: this.pageSize
      });
      this.list = res.rows || [];
      this.total = res.total || 0;
    },
    reset() {
      this.query = { name: '', ruleTemplate: '', severity: '', status: '' };
      this.pageNum = 1;
      this.fetchList();
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchList();
    },
    openEditor(row) {
      this.editor = row ? { ...createEmptyEditor(), ...row } : createEmptyEditor();
      this.editorVisible = true;
      this.$nextTick(() => this.$refs.editorForm && this.$refs.editorForm.clearValidate());
    },
    submitEditor() {
      this.$refs.editorForm.validate(async valid => {
        if (!valid) {
          return;
        }
        if (this.editor.id) {
          await updateMonitorRule(this.editor);
          this.$message.success('规则已更新');
        } else {
          await createMonitorRule(this.editor);
          this.$message.success('规则已创建');
        }
        this.editorVisible = false;
        this.fetchList();
      });
    },
    async toggleStatus(row) {
      if (row.status === '0') {
        await disableMonitorRule(row.id);
        this.$message.success('规则已停用');
      } else {
        await enableMonitorRule(row.id);
        this.$message.success('规则已启用');
      }
      this.fetchList();
    },
    async removeRow(row) {
      await this.$confirm(`确认删除规则“${row.name}”吗？`, '提示', { type: 'warning' });
      await deleteMonitorRule(row.id);
      this.$message.success('规则已删除');
      this.fetchList();
    }
  }
};
</script>

<style scoped>
.panel-card,
.table-card {
  border-radius: 18px;
}
.table-card {
  margin-top: 16px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}
.filters,
.dialog-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.pager {
  margin-top: 16px;
  text-align: right;
}
.inline-hint {
  margin-left: 8px;
  color: #909399;
}
</style>
