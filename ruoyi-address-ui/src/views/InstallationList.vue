<template>
  <div>
    <el-card>
      <div class="filters">
        <el-input v-model="query.resourceId" placeholder="资源ID" clearable />
        <el-input v-model="query.resourceType" placeholder="资源类型" clearable />
        <el-input v-model="query.installName" placeholder="安装地址名称" clearable />
        <el-input v-model="query.standardAddressId" placeholder="标准地址ID" clearable />
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
        <el-table-column prop="resourceId" label="资源ID" width="120" />
        <el-table-column prop="resourceType" label="资源类型" width="120" />
        <el-table-column prop="installName" label="安装地址" />
        <el-table-column prop="hasStandardAddress" label="是否关联" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.hasStandardAddress" type="success">是</el-tag>
            <el-tag v-else type="info">否</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="standardAddressFullName" label="关联标准地址" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
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

    <el-dialog :visible.sync="detailVisible" title="安装地址详情" width="760px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="主键ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="标准地址ID">{{ detail.standardAddressId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="安装地址" :span="2">{{ detail.installName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="资源ID">{{ detail.resourceId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="资源类型">{{ detail.resourceType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="是否关联">{{ detail.hasStandardAddress ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联标准地址" :span="2">{{ detail.standardAddressFullName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { getInstallationDetail, getInstallationList } from '../api/address';

export default {
  data() {
    return {
      query: {
        resourceId: '',
        resourceType: '',
        installName: '',
        standardAddressId: ''
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
      if (params.standardAddressId) {
        params.standardAddressId = Number(params.standardAddressId);
      }
      if (this.timeRange && this.timeRange.length === 2) {
        params.params = {
          beginTime: this.timeRange[0],
          endTime: this.timeRange[1]
        };
      }
      const res = await getInstallationList(params);
      this.list = res.rows || [];
      this.total = res.total || 0;
    },
    reset() {
      this.query = { resourceId: '', resourceType: '', installName: '', standardAddressId: '' };
      this.timeRange = [];
      this.pageNum = 1;
      this.fetchList();
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchList();
    },
    async viewRow(row) {
      const res = await getInstallationDetail(row.id);
      this.detail = res.data || row;
      this.detailVisible = true;
    }
  }
};
</script>

<style scoped>
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
