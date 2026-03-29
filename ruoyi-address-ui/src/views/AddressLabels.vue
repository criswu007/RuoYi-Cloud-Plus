<template>
  <div>
    <el-card class="panel-card">
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="query.name" placeholder="标签名称" clearable @keyup.enter.native="fetchList" />
          <el-input v-model="query.code" placeholder="标签编码" clearable @keyup.enter.native="fetchList" />
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
        <el-button type="success" @click="openEditor()">新增标签</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="list" border stripe size="small">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="name" label="标签名称" min-width="160" />
        <el-table-column prop="code" label="标签编码" min-width="140" />
        <el-table-column label="标签颜色" width="120">
          <template #default="{ row }">
            <div class="color-cell">
              <span class="color-dot" :style="{ background: row.color || '#5b6b7b' }" />
              <span>{{ row.color || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
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

    <el-dialog :visible.sync="editorVisible" :title="editor.id ? '编辑标签' : '新增标签'" width="520px">
      <el-form ref="editorForm" :model="editor" :rules="rules" label-width="100px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="editor.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="标签编码">
          <el-input v-model="editor.code" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="标签颜色">
          <div class="color-picker">
            <el-input v-model="editor.color" placeholder="#79c49a / red / rgb()" />
            <div class="preset-group">
              <button
                v-for="item in presetColors"
                :key="item"
                type="button"
                class="preset-color"
                :style="{ background: item }"
                @click="editor.color = item"
              />
            </div>
          </div>
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
import { createTag, deleteTag, getTagList, updateTag } from '../api/address';

export default {
  data() {
    return {
      query: {
        name: '',
        code: ''
      },
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      editorVisible: false,
      editor: {
        id: null,
        name: '',
        code: '',
        color: '#79c49a',
        remark: ''
      },
      presetColors: ['#79c49a', '#d9a75f', '#4d88ff', '#f56c6c', '#8a6fd1', '#4ca998'],
      rules: {
        name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }]
      }
    };
  },
  mounted() {
    this.fetchList();
  },
  methods: {
    async fetchList() {
      const res = await getTagList({
        ...this.query,
        pageNum: this.pageNum,
        pageSize: this.pageSize
      });
      this.list = res.rows || [];
      this.total = res.total || 0;
    },
    reset() {
      this.query = { name: '', code: '' };
      this.pageNum = 1;
      this.fetchList();
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchList();
    },
    openEditor(row) {
      this.editor = row
        ? { id: row.id, name: row.name, code: row.code, color: row.color, remark: row.remark }
        : { id: null, name: '', code: '', color: '#79c49a', remark: '' };
      this.editorVisible = true;
      this.$nextTick(() => this.$refs.editorForm && this.$refs.editorForm.clearValidate());
    },
    submitEditor() {
      this.$refs.editorForm.validate(async valid => {
        if (!valid) {
          return;
        }
        if (this.editor.id) {
          await updateTag(this.editor);
          this.$message.success('标签已更新');
        } else {
          await createTag(this.editor);
          this.$message.success('标签已创建');
        }
        this.editorVisible = false;
        this.fetchList();
      });
    },
    async removeRow(row) {
      await this.$confirm(`确认删除标签“${row.name}”吗？`, '提示', { type: 'warning' });
      await deleteTag(row.id);
      this.$message.success('标签已删除');
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
.color-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.color-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  box-shadow: 0 0 0 2px rgba(16, 36, 51, 0.08);
}
.color-picker {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.preset-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.preset-color {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  box-shadow: 0 0 0 2px rgba(16, 36, 51, 0.08);
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
