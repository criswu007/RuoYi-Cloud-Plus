<template>
  <div>
    <el-card class="panel-card">
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="query.name" placeholder="规则名称" clearable @keyup.enter.native="fetchList" />
          <el-select v-model="query.ruleType" placeholder="规则类型" clearable>
            <el-option label="REGEX" value="REGEX" />
            <el-option label="DICT" value="DICT" />
            <el-option label="CUSTOM" value="CUSTOM" />
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
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="name" label="规则名称" min-width="170" />
        <el-table-column prop="ruleType" label="规则类型" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">
              {{ row.status === '0' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ruleContent" label="规则内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="mini" type="primary" plain @click="openEditor(row)">编辑</el-button>
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

    <el-dialog :visible.sync="editorVisible" :title="editor.id ? '编辑规则' : '新增规则'" width="640px">
      <el-form ref="editorForm" :model="editor" :rules="rules" label-width="100px">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="editor.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="editor.ruleType" style="width: 100%">
            <el-option label="REGEX" value="REGEX" />
            <el-option label="DICT" value="DICT" />
            <el-option label="CUSTOM" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editor.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="规则内容" prop="ruleContent">
          <el-input v-model="editor.ruleContent" type="textarea" :rows="5" placeholder="例如：^江苏省.+" />
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
import { createMonitorRule, deleteMonitorRule, getMonitorRules, updateMonitorRule } from '../api/address';

export default {
  data() {
    return {
      query: {
        name: '',
        ruleType: '',
        status: ''
      },
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      editorVisible: false,
      editor: {
        id: null,
        name: '',
        ruleType: 'REGEX',
        ruleContent: '',
        status: '0',
        remark: ''
      },
      rules: {
        name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
        ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }],
        ruleContent: [{ required: true, message: '请输入规则内容', trigger: 'blur' }]
      }
    };
  },
  mounted() {
    this.fetchList();
  },
  methods: {
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
      this.query = { name: '', ruleType: '', status: '' };
      this.pageNum = 1;
      this.fetchList();
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchList();
    },
    openEditor(row) {
      this.editor = row
        ? { ...row }
        : { id: null, name: '', ruleType: 'REGEX', ruleContent: '', status: '0', remark: '' };
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
.filters {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.pager {
  margin-top: 16px;
  text-align: right;
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
