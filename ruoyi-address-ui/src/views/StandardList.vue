<template>
  <div>
    <el-card class="panel-card">
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="query.name" placeholder="当级名称" clearable @keyup.enter.native="fetchList" />
          <el-input v-model="query.fullName" placeholder="完整地址" clearable @keyup.enter.native="fetchList" />
          <el-input v-model="query.code" placeholder="行政编码" clearable @keyup.enter.native="fetchList" />
          <el-select v-model="query.level" placeholder="层级" clearable>
            <el-option v-for="item in levelOptions" :key="item" :label="`第 ${item} 级`" :value="item" />
          </el-select>
          <el-select v-model="query.status" placeholder="状态" clearable>
            <el-option label="正常" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
        <div class="actions">
          <el-button type="success" @click="openEditor()">新增地址</el-button>
          <el-button type="primary" plain @click="openBatchAdd">批量新增下级</el-button>
          <el-button type="warning" plain @click="openTagDialog('bind')">批量打标签</el-button>
          <el-button type="info" plain @click="openTagDialog('unbind')">批量取消标签</el-button>
          <el-upload
            :show-file-list="false"
            :http-request="handleImport"
            accept=".xlsx,.xls"
          >
            <el-button type="warning">导入</el-button>
          </el-upload>
        </div>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="list" border stripe size="small" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="110" />
        <el-table-column prop="fullName" label="标准地址" min-width="280" show-overflow-tooltip />
        <el-table-column prop="name" label="当级名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column prop="parentId" label="父级ID" width="110" />
        <el-table-column prop="level" label="层级" width="80">
          <template #default="{ row }">
            <span class="level-chip">L{{ row.level }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="220">
          <template #default="{ row }">
            <div v-if="row.tagNames && row.tagNames.length" class="tag-group">
              <el-tag
                v-for="tag in row.tagNames"
                :key="`${row.id}-${tag}`"
                size="mini"
                effect="plain"
              >
                {{ tag }}
              </el-tag>
            </div>
            <span v-else class="empty-text">未打标签</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="mini" @click="viewRow(row)">详情</el-button>
            <el-button size="mini" type="primary" plain :disabled="row.level <= 2" @click="openEditor(row)">编辑</el-button>
            <el-button size="mini" type="danger" plain :disabled="row.level <= 2" @click="removeRow(row)">删除</el-button>
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

    <el-dialog :visible.sync="detailVisible" title="标准地址详情" width="700px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="主键ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="父级ID">{{ detail.parentId }}</el-descriptions-item>
        <el-descriptions-item label="当级名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="完整地址">{{ detail.fullName }}</el-descriptions-item>
        <el-descriptions-item label="地址编码">{{ detail.code || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址层级">L{{ detail.level }}</el-descriptions-item>
        <el-descriptions-item label="省编码">{{ detail.provinceCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="市编码">{{ detail.cityCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="区县编码">{{ detail.districtCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="街道编码">{{ detail.streetCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="村社区编码">{{ detail.villageCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === '0' ? '正常' : '停用' }}</el-descriptions-item>
        <el-descriptions-item label="标签" :span="2">
          <div v-if="detail.tagNames && detail.tagNames.length" class="tag-group">
            <el-tag v-for="tag in detail.tagNames" :key="tag" size="mini">{{ tag }}</el-tag>
          </div>
          <span v-else class="empty-text">未打标签</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog :visible.sync="editorVisible" :title="editor.id ? '编辑标准地址' : '新增标准地址'" width="720px">
      <el-form ref="editorForm" :model="editor" :rules="editorRules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="父级标准地址ID">
              <el-input v-model="editor.parentId" placeholder="顶级可留空或 0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="地址层级">
              <el-input v-model="editor.level" placeholder="为空则按父级自动推导" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="当级名称" prop="name">
              <el-input v-model="editor.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="地址编码">
              <el-input v-model="editor.code" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="省编码">
              <el-input v-model="editor.provinceCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="市编码">
              <el-input v-model="editor.cityCode" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="区县编码">
              <el-input v-model="editor.districtCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="街道编码">
              <el-input v-model="editor.streetCode" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="村社区编码">
              <el-input v-model="editor.villageCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="editor.status">
                <el-radio label="0">正常</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="editor.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-actions">
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEditor">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog :visible.sync="batchAddVisible" title="批量新增下级地址" width="520px">
      <el-form :model="batchAdd" label-width="120px">
        <el-form-item label="父级标准地址ID">
          <el-input v-model="batchAdd.parentId" placeholder="例如 1001" />
        </el-form-item>
        <el-form-item label="前缀">
          <el-input v-model="batchAdd.prefix" placeholder="例如 1单元" />
        </el-form-item>
        <el-form-item label="起始编号">
          <el-input v-model.number="batchAdd.startNum" placeholder="例如 1" />
        </el-form-item>
        <el-form-item label="结束编号">
          <el-input v-model.number="batchAdd.endNum" placeholder="例如 20" />
        </el-form-item>
        <el-form-item label="后缀">
          <el-input v-model="batchAdd.suffix" placeholder="例如 室，可为空" />
        </el-form-item>
      </el-form>
      <div class="dialog-actions">
        <el-button @click="batchAddVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchAdd">提交</el-button>
      </div>
    </el-dialog>

    <el-dialog :visible.sync="tagDialogVisible" :title="tagDialogMode === 'bind' ? '批量打标签' : '批量取消标签'" width="540px">
      <div class="selection-summary">
        已选择 <strong>{{ selectedStandardAddressIds.length }}</strong> 条标准地址
      </div>
      <el-form label-width="90px">
        <el-form-item label="标签选择">
          <el-select v-model="selectedTagIds" multiple filterable style="width: 100%" placeholder="请选择标签">
            <el-option v-for="item in tagOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-actions">
        <el-button @click="tagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTags">
          {{ tagDialogMode === 'bind' ? '确认打标' : '确认取消标签' }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  batchAddStandardAddressChildren,
  bindTagsToStandardAddresses,
  createStandardAddress,
  deleteStandardAddresses,
  getStandardAddressDetail,
  getStandardAddressList,
  getTagList,
  importStandardAddressData,
  unbindTagsFromStandardAddresses,
  updateStandardAddress
} from '../api/address';

export default {
  data() {
    return {
      query: {
        name: '',
        fullName: '',
        code: '',
        level: '',
        status: ''
      },
      levelOptions: Array.from({ length: 14 }, (_, index) => index + 1),
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      selectedStandardAddressIds: [],
      detailVisible: false,
      detail: null,
      editorVisible: false,
      editor: {
        id: null,
        parentId: '',
        name: '',
        code: '',
        level: '',
        provinceCode: '',
        cityCode: '',
        districtCode: '',
        streetCode: '',
        villageCode: '',
        status: '0',
        remark: ''
      },
      editorRules: {
        name: [{ required: true, message: '请输入当级名称', trigger: 'blur' }]
      },
      batchAddVisible: false,
      batchAdd: {
        parentId: '',
        prefix: '',
        startNum: '',
        endNum: '',
        suffix: ''
      },
      tagDialogVisible: false,
      tagDialogMode: 'bind',
      tagOptions: [],
      selectedTagIds: []
    };
  },
  mounted() {
    this.fetchList();
    this.fetchTags();
  },
  methods: {
    createEmptyEditor() {
      return {
        id: null,
        parentId: '',
        name: '',
        code: '',
        level: '',
        provinceCode: '',
        cityCode: '',
        districtCode: '',
        streetCode: '',
        villageCode: '',
        status: '0',
        remark: ''
      };
    },
    async fetchList() {
      const params = {
        ...this.query,
        pageNum: this.pageNum,
        pageSize: this.pageSize
      };
      if (params.level) {
        params.level = Number(params.level);
      }
      const res = await getStandardAddressList(params);
      this.list = res.rows || [];
      this.total = res.total || 0;
    },
    async fetchTags() {
      const res = await getTagList({ pageNum: 1, pageSize: 200 });
      this.tagOptions = res.rows || [];
    },
    reset() {
      this.query = { name: '', fullName: '', code: '', level: '', status: '' };
      this.pageNum = 1;
      this.fetchList();
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchList();
    },
    handleSelectionChange(rows) {
      this.selectedStandardAddressIds = rows.map(item => item.id);
    },
    async viewRow(row) {
      const res = await getStandardAddressDetail(row.id);
      this.detail = res.data || row;
      this.detailVisible = true;
    },
    openEditor(row) {
      this.editor = row
        ? {
          id: row.id,
          parentId: row.parentId,
          name: row.name,
          code: row.code,
          level: row.level,
          provinceCode: row.provinceCode,
          cityCode: row.cityCode,
          districtCode: row.districtCode,
          streetCode: row.streetCode,
          villageCode: row.villageCode,
          status: row.status || '0',
          remark: row.remark
        }
        : this.createEmptyEditor();
      this.editorVisible = true;
      this.$nextTick(() => this.$refs.editorForm && this.$refs.editorForm.clearValidate());
    },
    submitEditor() {
      this.$refs.editorForm.validate(async valid => {
        if (!valid) {
          return;
        }
        const payload = {
          ...this.editor,
          parentId: this.editor.parentId === '' ? undefined : Number(this.editor.parentId),
          level: this.editor.level === '' ? undefined : Number(this.editor.level)
        };
        if (this.editor.id) {
          await updateStandardAddress(payload);
          this.$message.success('标准地址已更新');
        } else {
          await createStandardAddress(payload);
          this.$message.success('标准地址已创建');
        }
        this.editorVisible = false;
        this.fetchList();
      });
    },
    async removeRow(row) {
      try {
        await this.$confirm('确认删除该标准地址？若有关联安装地址需二次确认', '提示');
        try {
          await deleteStandardAddresses(row.id, false);
          this.$message.success('已删除');
          this.fetchList();
        } catch (err) {
          const msg = err?.response?.data?.msg || err?.message || '';
          if (msg.includes('确认') || msg.includes('关联安装地址')) {
            await this.$confirm('地址已关联安装地址，是否确认删除？', '二次确认');
            await deleteStandardAddresses(row.id, true);
            this.$message.success('已删除');
            this.fetchList();
          } else {
            throw err;
          }
        }
      } catch (e) {
        if (e !== 'cancel') {
          const msg = e?.friendlyMessage || e?.response?.data?.msg || e?.message;
          if (msg) {
            this.$message.warning(msg);
          }
        }
      }
    },
    openBatchAdd() {
      this.batchAddVisible = true;
    },
    async submitBatchAdd() {
      if (!this.batchAdd.parentId || !this.batchAdd.prefix || !this.batchAdd.startNum || !this.batchAdd.endNum) {
        this.$message.warning('请填写完整参数');
        return;
      }
      await batchAddStandardAddressChildren({
        parentId: Number(this.batchAdd.parentId),
        prefix: this.batchAdd.prefix,
        startNum: Number(this.batchAdd.startNum),
        endNum: Number(this.batchAdd.endNum),
        suffix: this.batchAdd.suffix || ''
      });
      this.$message.success('批量新增成功');
      this.batchAddVisible = false;
      this.batchAdd = { parentId: '', prefix: '', startNum: '', endNum: '', suffix: '' };
      this.fetchList();
    },
    openTagDialog(mode) {
      if (!this.selectedStandardAddressIds.length) {
        this.$message.warning('请先勾选标准地址');
        return;
      }
      this.tagDialogMode = mode;
      this.selectedTagIds = [];
      this.tagDialogVisible = true;
    },
    async submitTags() {
      if (!this.selectedTagIds.length) {
        this.$message.warning('请选择标签');
        return;
      }
      const payload = {
        standardAddressIds: this.selectedStandardAddressIds,
        tagIds: this.selectedTagIds
      };
      if (this.tagDialogMode === 'bind') {
        await bindTagsToStandardAddresses(payload);
        this.$message.success('标签已绑定');
      } else {
        await unbindTagsFromStandardAddresses(payload);
        this.$message.success('标签已解绑');
      }
      this.tagDialogVisible = false;
      this.fetchList();
    },
    async handleImport(request) {
      const file = request.file;
      if (!file) {
        this.$message.warning('请选择文件');
        return;
      }
      try {
        await importStandardAddressData(file, false);
        this.$message.success('导入完成');
        this.fetchList();
      } catch (err) {
        const msg = err?.friendlyMessage || err?.response?.data?.msg || err?.message || '导入失败';
        this.$message.error(msg);
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
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  flex-wrap: wrap;
}
.filters {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.table-card {
  margin-top: 16px;
}
.pager {
  margin-top: 16px;
  text-align: right;
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
.tag-group {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.empty-text {
  color: #8c9aa8;
}
.level-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  padding: 0 8px;
  height: 26px;
  border-radius: 999px;
  background: rgba(121, 196, 154, 0.15);
  color: #2f6f51;
  font-weight: 600;
}
.selection-summary {
  margin-bottom: 12px;
  color: #5f6f7f;
}
</style>
