<template>
  <div class="installation-page">
    <el-card shadow="never">
      <div class="filters">
        <el-input v-model="query.deviceId" clearable placeholder="设备ID" />
        <el-input v-model="query.setAddrName" clearable placeholder="地址名称" />
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="yyyy-MM-dd HH:mm:ss"
        />
        <el-button type="primary" @click="fetchList">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border size="small">
        <el-table-column label="序号" width="80">
          <template #default="{ $index }">
            {{ (pageNum - 1) * pageSize + $index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="setAddrName" label="安装地址名称" min-width="280" show-overflow-tooltip />
        <el-table-column label="是否关联标准地址" width="140">
          <template #default="{ row }">
            <span>{{ row.associationStatus === 'BOUND' ? '已关联' : '未关联' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="standName" label="关联标准地址" min-width="260" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="mini" @click="openDetail(row)">详情</el-button>
            <el-button size="mini" @click="openEdit(row)">修改</el-button>
            <el-button size="mini" type="danger" plain @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :current-page="pageNum"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          @size-change="changeSize"
          @current-change="changePage"
        />
      </div>
    </el-card>

    <el-dialog :visible.sync="detailVisible" title="安装地址详情" width="760px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="安装地址ID">{{ detail.setAddrId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="安装地址编号">{{ detail.setAddrNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="安装地址名称" :span="2">{{ detail.setAddrName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址类型">{{ detail.setType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备ID">{{ detail.deviceId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="标准地址ID">{{ detail.segmId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联标准地址" :span="2">{{ detail.standName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="组织ID">{{ detail.orgId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.bossOp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ detail.createDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.notes || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog :visible.sync="editVisible" title="修改安装地址" width="680px">
      <el-form ref="editForm" :model="editForm" :rules="rules" label-width="120px">
        <el-form-item label="安装地址名称" prop="setAddrName">
          <el-input v-model="editForm.setAddrName" maxlength="400" show-word-limit />
        </el-form-item>
        <el-form-item label="安装地址编号">
          <el-input v-model="editForm.setAddrNo" maxlength="120" />
        </el-form-item>
        <el-form-item label="地址类型">
          <el-input v-model="editForm.setType" maxlength="24" />
        </el-form-item>
        <el-form-item label="标准地址ID">
          <el-input v-model="editForm.segmId" maxlength="24" />
        </el-form-item>
        <el-form-item label="设备ID">
          <el-input v-model="editForm.deviceId" maxlength="64" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.notes" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-actions">
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  deleteInstallationAddresses,
  getInstallationDetail,
  getInstallationList,
  updateInstallationAddress
} from '../api/address';

export default {
  data() {
    return {
      query: {
        deviceId: '',
        setAddrName: ''
      },
      timeRange: [],
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      loading: false,
      detailVisible: false,
      detail: null,
      editVisible: false,
      editForm: {
        setAddrId: '',
        setAddrName: '',
        setAddrNo: '',
        setType: '',
        segmId: '',
        deviceId: '',
        notes: ''
      },
      rules: {
        setAddrName: [{ required: true, message: '请输入安装地址名称', trigger: 'blur' }]
      }
    };
  },
  mounted() {
    this.fetchList();
  },
  methods: {
    buildQueryParams() {
      const params = {
        ...this.query,
        pageNum: this.pageNum,
        pageSize: this.pageSize
      };
      if (this.timeRange?.length === 2) {
        params.params = {
          beginTime: this.timeRange[0],
          endTime: this.timeRange[1]
        };
      }
      return params;
    },
    async fetchList() {
      this.loading = true;
      try {
        const res = await getInstallationList(this.buildQueryParams());
        this.list = res.rows || [];
        this.total = res.total || 0;
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '安装地址查询失败');
      } finally {
        this.loading = false;
      }
    },
    reset() {
      this.query = { deviceId: '', setAddrName: '' };
      this.timeRange = [];
      this.pageNum = 1;
      this.fetchList();
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchList();
    },
    changeSize(size) {
      this.pageSize = size;
      this.pageNum = 1;
      this.fetchList();
    },
    async openDetail(row) {
      try {
        const res = await getInstallationDetail(row.setAddrId);
        this.detail = res.data || row;
        this.detailVisible = true;
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '安装地址详情查询失败');
      }
    },
    openEdit(row) {
      this.editForm = {
        setAddrId: row.setAddrId,
        setAddrName: row.setAddrName || '',
        setAddrNo: row.setAddrNo || '',
        setType: row.setType || '',
        segmId: row.segmId || '',
        deviceId: row.deviceId || '',
        notes: row.notes || ''
      };
      this.editVisible = true;
      this.$nextTick(() => this.$refs.editForm && this.$refs.editForm.clearValidate());
    },
    submitEdit() {
      this.$refs.editForm.validate(async valid => {
        if (!valid) {
          return;
        }
        try {
          await updateInstallationAddress(this.editForm);
          this.$message.success('安装地址已更新');
          this.editVisible = false;
          this.fetchList();
        } catch (err) {
          this.$message.error(err?.friendlyMessage || err?.message || '安装地址更新失败');
        }
      });
    },
    async removeRow(row) {
      await this.$confirm(`确认删除安装地址“${row.setAddrName}”吗？`, '提示', { type: 'warning' });
      try {
        await deleteInstallationAddresses(row.setAddrId);
        this.$message.success('安装地址已删除');
        this.fetchList();
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '安装地址删除失败');
      }
    }
  }
};
</script>

<style scoped>
.installation-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
