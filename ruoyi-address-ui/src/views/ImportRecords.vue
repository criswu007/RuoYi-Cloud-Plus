<template>
  <div>
    <el-card>
      <div class="filters">
        <el-input v-model="query.fileName" placeholder="导入文件名" clearable />
        <el-select v-model="query.status" placeholder="导入状态" clearable>
          <el-option label="进行中" value="0" />
          <el-option label="成功" value="1" />
          <el-option label="失败" value="2" />
        </el-select>
        <el-input v-model="query.createBy" placeholder="操作人ID" clearable />
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

    <el-card class="table-card">
      <el-table :data="list" border size="small">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="fileName" label="导入文件名" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.status === '0'">进行中</el-tag>
            <el-tag v-else-if="row.status === '1'" type="success">成功</el-tag>
            <el-tag v-else type="danger">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="successCount" label="成功数" width="90" />
        <el-table-column prop="failCount" label="失败数" width="90" />
        <el-table-column prop="errorMsg" label="错误信息" show-overflow-tooltip />
        <el-table-column prop="createBy" label="操作人ID" width="110" />
        <el-table-column prop="createTime" label="操作时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="mini" @click="viewRow(row)">详情</el-button>
            <el-button
              v-if="row.status === '2' && row.errorMsg"
              size="mini"
              type="warning"
              plain
              @click="downloadErrorSummary(row)"
            >
              下载错误摘要
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

    <el-dialog :visible.sync="detailVisible" title="导入记录详情" width="760px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="记录ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="导入状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="导入文件" :span="2">{{ detail.fileName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="成功数量">{{ detail.successCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="失败数量">{{ detail.failCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="操作人ID">{{ detail.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="错误摘要" :span="2">
          <pre class="detail-pre">{{ detail.errorMsg || '无错误信息' }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <div class="detail-tip">
        当前版本数据库仅保存导入汇总与错误摘要，尚未持久化逐条失败明细。
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getImportRecordDetail, getImportRecords } from '../api/address';

export default {
  data() {
    return {
      query: {
        fileName: '',
        status: '',
        createBy: ''
      },
      timeRange: [],
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      detailVisible: false,
      detail: null
    };
  },
  mounted() {
    this.fetchList();
  },
  methods: {
    async fetchList() {
      const params = {
        ...this.query,
        pageNum: this.pageNum,
        pageSize: this.pageSize
      };
      if (this.timeRange && this.timeRange.length === 2) {
        params.params = {
          beginTime: this.timeRange[0],
          endTime: this.timeRange[1]
        };
      }
      const res = await getImportRecords(params);
      this.list = res.rows || [];
      this.total = res.total || 0;
    },
    reset() {
      this.query = { fileName: '', status: '', createBy: '' };
      this.timeRange = [];
      this.pageNum = 1;
      this.fetchList();
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchList();
    },
    statusLabel(status) {
      return {
        '0': '进行中',
        '1': '成功',
        '2': '失败'
      }[status] || status || '-';
    },
    async viewRow(row) {
      const res = await getImportRecordDetail(row.id);
      this.detail = res.data || row;
      this.detailVisible = true;
    },
    downloadErrorSummary(row) {
      const content = [
        `导入文件：${row.fileName || '-'}`,
        `记录ID：${row.id || '-'}`,
        `失败数量：${row.failCount || 0}`,
        '',
        row.errorMsg || '无错误摘要'
      ].join('\n');
      const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${row.fileName || `import-record-${row.id}`}-错误摘要.txt`;
      link.click();
      URL.revokeObjectURL(url);
    }
  }
};
</script>

<style scoped>
.detail-tip {
  margin-top: 12px;
  color: #8c9aa8;
  font-size: 13px;
}
.detail-pre {
  margin: 0;
  max-height: 280px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
.filters {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.table-card {
  margin-top: 12px;
}
.pager {
  margin-top: 12px;
  text-align: right;
}
</style>
