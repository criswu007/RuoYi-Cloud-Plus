<template>
  <div>
    <el-card>
      <div class="filters">
        <el-input v-model="query.standardAddressId" placeholder="标准地址ID" clearable />
        <el-select v-model="query.operationType" placeholder="操作类型" clearable>
          <el-option label="MERGE" value="MERGE" />
          <el-option label="SPLIT" value="SPLIT" />
          <el-option label="DELETE" value="DELETE" />
          <el-option label="IMPORT" value="IMPORT" />
          <el-option label="UPDATE" value="UPDATE" />
          <el-option label="INSERT" value="INSERT" />
        </el-select>
        <el-input v-model="query.operator" placeholder="操作人" clearable />
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
        <el-table-column prop="standardAddressId" label="标准地址ID" width="130" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeTagMap[row.operationType] || 'info'" effect="plain">
              {{ row.operationType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="operateTime" label="操作时间" width="170" />
        <el-table-column prop="details" label="详情" show-overflow-tooltip />
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
        standardAddressId: '',
        operationType: '',
        operator: ''
      },
      timeRange: [],
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
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
      if (params.standardAddressId) {
        params.standardAddressId = Number(params.standardAddressId);
      }
      const res = await getOperationLogs(params);
      this.list = res.rows || [];
      this.total = res.total || 0;
    },
    reset() {
      this.query = { standardAddressId: '', operationType: '', operator: '' };
      this.timeRange = [];
      this.pageNum = 1;
      this.fetchList();
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchList();
    },
    async viewRow(row) {
      const res = await getOperationLogDetail(row.id);
      this.detail = res.data || row;
      this.detailVisible = true;
    }
  }
};
</script>

<style scoped>
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
