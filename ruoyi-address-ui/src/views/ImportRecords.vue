<template>
  <div class="import-record-page">
    <el-card shadow="never">
      <div class="filters">
        <el-input v-model="query.batchId" clearable placeholder="批次ID" />
        <el-input v-model="query.fileName" clearable placeholder="导入文件名" />
        <el-select v-model="query.status" clearable placeholder="导入状态">
          <el-option label="成功" value="1" />
          <el-option label="失败" value="2" />
        </el-select>
        <el-input v-model="query.segmName" clearable placeholder="当级名称" />
        <el-input v-model="query.createBy" clearable placeholder="操作人ID" />
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
        <el-table-column prop="batchId" label="批次ID" width="120" />
        <el-table-column prop="batchNo" label="批次号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="fileName" label="导入文件名" min-width="220" show-overflow-tooltip />
        <el-table-column prop="rowNum" label="行号" width="90" />
        <el-table-column prop="parentStandName" label="父级地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="segmName" label="当级名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="addrLevel" label="级别" width="90" />
        <el-table-column prop="failReason" label="失败原因" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createTime" label="操作时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="mini" @click="openBatchDetail(row.batchId)">批次详情</el-button>
            <el-button size="mini" type="warning" plain @click="downloadFailures(row.batchId)">
              导出失败数据
            </el-button>
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

    <el-dialog :visible.sync="batchDialogVisible" title="导入批次详情" width="760px">
      <el-descriptions v-if="batchDetail" :column="2" border>
        <el-descriptions-item label="批次ID">{{ batchDetail.batchId }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ batchDetail.batchNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="导入文件" :span="2">{{ batchDetail.fileName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="总数量">{{ batchDetail.totalCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="成功数量">{{ batchDetail.successCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="失败数量">{{ batchDetail.failCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="导入状态">{{ batchDetail.status || '-' }}</el-descriptions-item>
        <el-descriptions-item label="错误摘要" :span="2">{{ batchDetail.errorMsg || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import {
  exportImportFailDetails,
  getImportBatchDetail,
  getImportRecords
} from '../api/address';

export default {
  data() {
    return {
      query: {
        batchId: '',
        fileName: '',
        status: '',
        segmName: '',
        createBy: ''
      },
      timeRange: [],
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      loading: false,
      batchDialogVisible: false,
      batchDetail: null
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
        const res = await getImportRecords(this.buildQueryParams());
        this.list = res.rows || [];
        this.total = res.total || 0;
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '导入记录查询失败');
      } finally {
        this.loading = false;
      }
    },
    reset() {
      this.query = {
        batchId: '',
        fileName: '',
        status: '',
        segmName: '',
        createBy: ''
      };
      this.timeRange = [];
      this.pageNum = 1;
      this.fetchList();
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchList();
    },
    async openBatchDetail(batchId) {
      try {
        const res = await getImportBatchDetail(batchId);
        this.batchDetail = res.data || res;
        this.batchDialogVisible = true;
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '批次详情查询失败');
      }
    },
    async downloadFailures(batchId) {
      try {
        const blob = await exportImportFailDetails(batchId);
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `standard-address-import-failures-${batchId}.xlsx`;
        link.click();
        window.URL.revokeObjectURL(url);
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '失败数据导出失败');
      }
    }
  }
};
</script>

<style scoped>
.import-record-page {
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
</style>
