<template>
  <div class="operation-log-page">
    <el-card shadow="never">
      <div class="filters">
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="操作开始时间"
          end-placeholder="操作结束时间"
          value-format="yyyy-MM-dd HH:mm:ss"
        />
        <el-select v-model="query.operationType" clearable placeholder="操作类型">
          <el-option label="新增" value="INSERT" />
          <el-option label="修改" value="UPDATE" />
          <el-option label="删除" value="DELETE" />
          <el-option label="导入" value="IMPORT" />
          <el-option label="合并" value="MERGE" />
          <el-option label="拆分" value="SPLIT" />
        </el-select>
        <el-button type="primary" @click="fetchList">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="list" border size="small">
        <el-table-column label="序号" width="80">
          <template #default="{ $index }">
            {{ (pageNum - 1) * pageSize + $index + 1 }}
          </template>
        </el-table-column>
        <el-table-column label="操作类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeTagMap[row.operationType] || 'info'" effect="plain">
              {{ row.operationType || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operationObject" label="操作对象" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作结果" width="100">
          <template #default="{ row }">
            <el-tag :type="resultTagType(row.operationResult)" effect="plain">
              {{ row.operationResult || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="operateTime" label="操作时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="mini" @click="viewRow(row)">详情</el-button>
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

    <el-dialog :visible.sync="detailVisible" title="操作日志详情" width="760px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="日志ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ detail.operationType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="标准地址ID">{{ detail.standardAddressId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作对象">{{ detail.operationObject || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作结果">{{ detail.operationResult || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.operator || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ detail.operateTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作详情" :span="2">
          <pre class="detail-pre">{{ detail.details || '-' }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { getOperationLogDetail, getOperationLogs } from '../api/address';

export default {
  data() {
    return {
      query: {
        operationType: ''
      },
      timeRange: [],
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      loading: false,
      detailVisible: false,
      detail: null,
      typeTagMap: {
        INSERT: 'success',
        UPDATE: 'primary',
        DELETE: 'danger',
        IMPORT: 'warning',
        MERGE: 'warning',
        SPLIT: 'info'
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
    resultTagType(result) {
      if (result === '成功') {
        return 'success';
      }
      if (result === '失败') {
        return 'danger';
      }
      return 'info';
    },
    async fetchList() {
      this.loading = true;
      try {
        const res = await getOperationLogs(this.buildQueryParams());
        this.list = res.rows || [];
        this.total = res.total || 0;
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '操作日志查询失败');
      } finally {
        this.loading = false;
      }
    },
    reset() {
      this.query = { operationType: '' };
      this.timeRange = [];
      this.pageNum = 1;
      this.fetchList();
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchList();
    },
    async viewRow(row) {
      try {
        const res = await getOperationLogDetail(row.id);
        this.detail = res.data || row;
        this.detailVisible = true;
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '操作日志详情查询失败');
      }
    }
  }
};
</script>

<style scoped>
.operation-log-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
.pager {
  margin-top: 16px;
  text-align: right;
}
</style>
