<template>
  <div class="merge-page">
    <el-card shadow="never">
      <el-form :inline="true" :model="query" @submit.native.prevent>
        <el-form-item label="标准地址关键词">
          <el-input v-model="query.standName" clearable placeholder="请输入标准地址关键词" @keyup.enter.native="fetchList" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="query.segmType" clearable placeholder="请选择级别">
            <el-option
              v-for="item in levelOptions"
              :key="item.addrTypeId"
              :label="item.name"
              :value="item.addrTypeId"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" :disabled="!selectedRows.length" @click="appendPendingMerge(selectedRows)">
          添加到待合并列表
        </el-button>
        <el-button type="warning" :disabled="!selectedTargetRow" @click="setTargetAddress(selectedTargetRow)">
          设置为目标地址
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        border
        size="small"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="standName" label="地址名称" min-width="280" show-overflow-tooltip />
        <el-table-column prop="parentStandName" label="父级地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="segmName" label="当级名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="等级" width="110">
          <template #default="{ row }">
            {{ levelLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column label="选择" width="90">
          <template #default="{ row }">
            <el-radio v-model="selectedTargetSegmId" :label="row.segmId" @change="selectTargetRow(row)">选择</el-radio>
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

    <div class="panel-grid">
      <el-card shadow="never">
        <div class="panel-head">
          <span>待合并地址列表</span>
          <el-button type="text" @click="clearPending">清空列表</el-button>
        </div>
        <div class="card-list">
          <div v-for="item in pendingMergeList" :key="item.segmId" class="merge-card">
            <div class="merge-card__title">{{ item.standName || item.segmName || item.segmId }}</div>
            <div class="merge-card__meta">{{ levelLabel(item) }}</div>
            <el-button type="text" @click="removePending(item.segmId)">移除</el-button>
          </div>
          <div v-if="!pendingMergeList.length" class="empty-tip">请先从查询结果中添加待合并地址</div>
        </div>
      </el-card>

      <el-card shadow="never">
        <div class="panel-head">
          <span>合并目标地址列表</span>
        </div>
        <div class="card-list">
          <div v-if="targetAddress" class="merge-card target-card">
            <div class="merge-card__title">{{ targetAddress.standName || targetAddress.segmName || targetAddress.segmId }}</div>
            <div class="merge-card__meta">{{ levelLabel(targetAddress) }}</div>
            <el-button type="text" @click="clearTarget">移除</el-button>
          </div>
          <div v-else class="empty-tip">请先从查询结果中设置目标地址</div>
        </div>
      </el-card>
    </div>

    <div class="submit-bar">
      <el-button type="primary" :disabled="!canSubmitMerge()" :loading="submitLoading" @click="submitMerge">合并</el-button>
    </div>
  </div>
</template>

<script>
import {
  getStandardAddressLevelOptions,
  getStandardAddressList,
  mergeStandardAddresses
} from '../api/address';

function dedupeBySegmId(rows = []) {
  const rowMap = new Map();
  rows.forEach(item => {
    if (item?.segmId && !rowMap.has(item.segmId)) {
      rowMap.set(item.segmId, item);
    }
  });
  return [...rowMap.values()];
}

export default {
  data() {
    return {
      query: {
        standName: '',
        segmType: ''
      },
      levelOptions: [],
      levelMap: {},
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      loading: false,
      selectedRows: [],
      selectedTargetSegmId: '',
      selectedTargetRow: null,
      pendingMergeList: [],
      targetAddress: null,
      submitLoading: false
    };
  },
  mounted() {
    this.initializePage();
  },
  methods: {
    async initializePage() {
      await this.loadLevelOptions();
      await this.fetchList();
    },
    async loadLevelOptions() {
      const res = await getStandardAddressLevelOptions();
      this.levelOptions = res.data || [];
      this.levelMap = (res.data || []).reduce((accumulator, item) => {
        accumulator[item.addrTypeId] = item.name;
        return accumulator;
      }, {});
    },
    async fetchList() {
      this.loading = true;
      try {
        const res = await getStandardAddressList({
          ...this.query,
          pageNum: this.pageNum,
          pageSize: this.pageSize
        });
        this.list = res.rows || [];
        this.total = res.total || 0;
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '标准地址查询失败');
      } finally {
        this.loading = false;
      }
    },
    reset() {
      this.query = { standName: '', segmType: '' };
      this.pageNum = 1;
      this.fetchList();
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchList();
    },
    handleSelectionChange(rows) {
      this.selectedRows = rows || [];
    },
    selectTargetRow(row) {
      this.selectedTargetRow = row;
    },
    appendPendingMerge(rows) {
      this.pendingMergeList = dedupeBySegmId([...(this.pendingMergeList || []), ...(rows || [])]);
    },
    removePending(segmId) {
      this.pendingMergeList = (this.pendingMergeList || []).filter(item => item.segmId !== segmId);
    },
    clearPending() {
      this.pendingMergeList = [];
    },
    setTargetAddress(row) {
      this.targetAddress = row;
    },
    clearTarget() {
      this.targetAddress = null;
      this.selectedTargetSegmId = '';
      this.selectedTargetRow = null;
    },
    buildSubmitPayload() {
      return {
        sourceSegmIds: (this.pendingMergeList || []).map(item => item.segmId),
        targetSegmId: this.targetAddress?.segmId || ''
      };
    },
    canSubmitMerge() {
      return !!(this.pendingMergeList.length && this.targetAddress?.segmId);
    },
    levelLabel(row) {
      if (!row) {
        return '-';
      }
      return this.levelMap[row.segmType] || (row.addrLevel ? `${row.addrLevel}级` : '-');
    },
    async submitMerge() {
      if (!this.canSubmitMerge()) {
        this.$message.warning('请先补齐待合并地址和目标地址');
        return;
      }
      this.submitLoading = true;
      try {
        const payload = this.buildSubmitPayload();
        await mergeStandardAddresses(payload.sourceSegmIds, payload.targetSegmId);
        this.$message.success('合并完成');
        this.clearPending();
        this.clearTarget();
        await this.fetchList();
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '合并失败');
      } finally {
        this.submitLoading = false;
      }
    }
  }
};
</script>

<style scoped>
.merge-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar,
.panel-head,
.submit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pager {
  margin-top: 16px;
  text-align: right;
}

.panel-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.merge-card {
  padding: 14px;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  background: #fafbfd;
}

.target-card {
  background: #fff7e6;
  border-color: #f6c66f;
}

.merge-card__title {
  font-weight: 600;
  color: #303133;
}

.merge-card__meta,
.empty-tip {
  color: #909399;
  font-size: 13px;
}

@media (max-width: 900px) {
  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
