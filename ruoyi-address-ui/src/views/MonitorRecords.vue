<template>
  <div>
    <el-card class="panel-card">
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="query.standardAddressId" placeholder="标准地址ID" clearable @keyup.enter.native="fetchList" />
          <el-input v-model="query.ruleId" placeholder="规则ID" clearable @keyup.enter.native="fetchList" />
          <el-select v-model="query.status" placeholder="处理状态" clearable>
            <el-option label="待处理" value="0" />
            <el-option label="已忽略" value="1" />
            <el-option label="已处理" value="2" />
          </el-select>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
        <div class="toolbar-actions">
          <el-button type="success" plain @click="batchUpdateStatus('2')">批量标记处理</el-button>
          <el-button type="warning" plain @click="batchUpdateStatus('1')">批量忽略</el-button>
          <el-button type="warning" plain @click="$router.push('/monitor/task')">前往监控任务</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="list" border stripe size="small" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="standardAddressId" label="标准地址ID" width="130" />
        <el-table-column prop="standardAddressFullName" label="标准地址" min-width="240" show-overflow-tooltip />
        <el-table-column prop="ruleId" label="规则ID" width="110" />
        <el-table-column prop="ruleName" label="命中规则" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTypeMap[row.status]">
              {{ statusLabelMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createTime" label="发现时间" width="180" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="mini" @click="viewRow(row)">详情</el-button>
            <el-button size="mini" type="success" plain :disabled="row.status === '2'" @click="updateStatus(row, '2')">标记处理</el-button>
            <el-button size="mini" type="warning" plain :disabled="row.status === '1'" @click="updateStatus(row, '1')">忽略</el-button>
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

    <el-dialog :visible.sync="detailVisible" title="异常地址详情" width="760px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="记录ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="处理状态">{{ statusLabelMap[detail.status] || detail.status }}</el-descriptions-item>
        <el-descriptions-item label="标准地址ID">{{ detail.standardAddressId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="规则ID">{{ detail.ruleId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="标准地址" :span="2">{{ detail.standardAddressFullName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="命中规则" :span="2">{{ detail.ruleName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发现时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import {
  deleteMonitorRecord,
  getMonitorRecordDetail,
  getMonitorRecords,
  updateMonitorRecord
} from '../api/address';

export default {
  data() {
    return {
      query: {
        standardAddressId: '',
        ruleId: '',
        status: ''
      },
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      selectedRows: [],
      detailVisible: false,
      detail: null,
      statusLabelMap: {
        '0': '待处理',
        '1': '已忽略',
        '2': '已处理'
      },
      statusTypeMap: {
        '0': 'danger',
        '1': 'info',
        '2': 'success'
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
      if (params.standardAddressId) {
        params.standardAddressId = Number(params.standardAddressId);
      }
      if (params.ruleId) {
        params.ruleId = Number(params.ruleId);
      }
      const res = await getMonitorRecords(params);
      this.list = res.rows || [];
      this.total = res.total || 0;
    },
    reset() {
      this.query = { standardAddressId: '', ruleId: '', status: '' };
      this.pageNum = 1;
      this.fetchList();
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchList();
    },
    handleSelectionChange(rows) {
      this.selectedRows = rows;
    },
    async updateStatus(row, status) {
      await updateMonitorRecord({
        id: row.id,
        standardAddressId: row.standardAddressId,
        ruleId: row.ruleId,
        status,
        remark: row.remark
      });
      this.$message.success(status === '2' ? '已标记为处理完成' : '已忽略该异常');
      this.fetchList();
    },
    async batchUpdateStatus(status) {
      if (!this.selectedRows.length) {
        this.$message.warning('请先勾选异常记录');
        return;
      }
      await Promise.all(this.selectedRows.map(row => updateMonitorRecord({
        id: row.id,
        standardAddressId: row.standardAddressId,
        ruleId: row.ruleId,
        status,
        remark: row.remark
      })));
      this.$message.success(status === '2' ? '批量处理完成' : '批量忽略完成');
      this.fetchList();
    },
    async viewRow(row) {
      const res = await getMonitorRecordDetail(row.id);
      this.detail = res.data || row;
      this.detailVisible = true;
    },
    async removeRow(row) {
      try {
        await this.$confirm(`确认删除异常记录 #${row.id} 吗？`, '提示', { type: 'warning' });
        await deleteMonitorRecord(row.id);
        this.$message.success('异常记录已删除');
        this.fetchList();
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') {
          const msg = error?.friendlyMessage || error?.message;
          if (msg) {
            this.$message.warning(msg);
          }
        }
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
.toolbar-actions {
  display: flex;
  gap: 8px;
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
</style>
