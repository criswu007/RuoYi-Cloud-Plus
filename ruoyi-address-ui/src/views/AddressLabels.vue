<template>
  <div class="label-page">
    <el-card class="panel-card" shadow="never">
      <div class="toolbar">
        <div class="filters">
          <el-input
            v-model="query.name"
            placeholder="标签名称"
            clearable
            @keyup.enter.native="fetchList"
          />
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
        <el-button type="success" @click="openEditor()">新增标签</el-button>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="list" border size="small">
        <el-table-column label="序号" width="80">
          <template #default="{ $index }">
            {{ (pageNum - 1) * pageSize + $index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="name" label="标签名称" min-width="240" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="mini" type="primary" plain @click="openEditor(row)">修改</el-button>
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

    <el-dialog :visible.sync="editorVisible" :title="editor.id ? '修改标签' : '新增标签'" width="520px">
      <el-form ref="editorForm" :model="editor" :rules="rules" label-width="100px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="editor.name" maxlength="100" show-word-limit />
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
        name: ''
      },
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      loading: false,
      editorVisible: false,
      editor: {
        id: null,
        name: ''
      },
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
      this.loading = true;
      try {
        const res = await getTagList({
          ...this.query,
          pageNum: this.pageNum,
          pageSize: this.pageSize
        });
        this.list = res.rows || [];
        this.total = res.total || 0;
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '标签查询失败');
      } finally {
        this.loading = false;
      }
    },
    reset() {
      this.query = { name: '' };
      this.pageNum = 1;
      this.fetchList();
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchList();
    },
    openEditor(row) {
      this.editor = row
        ? { id: row.id, name: row.name }
        : { id: null, name: '' };
      this.editorVisible = true;
      this.$nextTick(() => this.$refs.editorForm && this.$refs.editorForm.clearValidate());
    },
    submitEditor() {
      this.$refs.editorForm.validate(async valid => {
        if (!valid) {
          return;
        }
        try {
          if (this.editor.id) {
            await updateTag({ id: this.editor.id, name: this.editor.name });
            this.$message.success('标签已更新');
          } else {
            await createTag({ name: this.editor.name });
            this.$message.success('标签已创建');
          }
          this.editorVisible = false;
          this.fetchList();
        } catch (err) {
          this.$message.error(err?.friendlyMessage || err?.message || '标签保存失败');
        }
      });
    },
    async removeRow(row) {
      await this.$confirm(`确认删除标签“${row.name}”吗？`, '提示', { type: 'warning' });
      try {
        await deleteTag(row.id);
        this.$message.success('标签已删除');
        this.fetchList();
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '标签删除失败');
      }
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
