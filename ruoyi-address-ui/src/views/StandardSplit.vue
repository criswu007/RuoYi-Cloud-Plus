<template>
  <div class="split-page">
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
      <el-table
        v-loading="loading"
        :data="list"
        border
        highlight-current-row
        size="small"
        @row-click="selectSource"
      >
        <el-table-column prop="standName" label="地址名称" min-width="280" show-overflow-tooltip />
        <el-table-column prop="parentStandName" label="父级地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="segmName" label="当级名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="等级" width="110">
          <template #default="{ row }">
            {{ levelLabel(row) }}
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

    <el-card shadow="never">
      <div class="source-bar">
        <div>待拆分地址：{{ selectedSource?.standName || '-' }}</div>
        <el-button v-if="selectedSource" type="text" @click="cancelSelectedSource">取消选择</el-button>
      </div>

      <div class="config-head">
        <div class="title">拆分后地址配置</div>
        <el-button type="primary" plain :disabled="!selectedSource" @click="addSplitItem">新增拆分地址项</el-button>
      </div>

      <div v-if="splitItems.length" class="split-items">
        <div v-for="(item, index) in splitItems" :key="index" class="split-item">
          <div class="split-item__title">第 {{ index + 1 }} 项</div>
          <el-input v-model="item.segmName" placeholder="拆分后当级名称" />
          <el-input :value="selectedSource?.parentStandName || '-'" disabled />
          <el-button type="text" @click="removeSplitItem(index)">删除</el-button>
        </div>
      </div>
      <div v-else class="empty-tip">请先选择待拆分地址，并新增拆分地址项</div>
    </el-card>

    <div class="submit-bar">
      <el-button type="primary" :disabled="!canSubmit()" :loading="submitLoading" @click="submitSplit">确认拆分</el-button>
    </div>
  </div>
</template>

<script>
import {
  getStandardAddressLevelOptions,
  getStandardAddressList,
  splitStandardAddress
} from '../api/address';

const READONLY_REGION_ADDR_TYPES = ['180000', '180001'];
const APPROVAL_SUCCESS_MESSAGE = '已提交审批，待审批通过后生效，审批期间原地址可继续使用。';

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
      selectedSource: null,
      splitItems: [],
      submitLoading: false
    };
  },
  mounted() {
    this.initializePage();
  },
  methods: {
    async initializePage() {
      await this.loadLevelOptions();
    },
    async loadLevelOptions() {
      try {
        const res = await getStandardAddressLevelOptions();
        this.levelOptions = (res.data || []).filter(item => !this.isReadonlyRegionLevelOption(item));
        this.levelMap = this.levelOptions.reduce((accumulator, item) => {
          accumulator[item.addrTypeId] = item.name;
          return accumulator;
        }, {});
      } catch (err) {
        this.levelOptions = [];
        this.levelMap = {};
        this.$message.error(err?.friendlyMessage || err?.message || '级别字典加载失败');
      }
    },
    isReadonlyRegionLevelOption(option) {
      if (!option) {
        return false;
      }
      const segmType = String(option.addrTypeId || '').trim();
      const addrLevel = Number(option.addrLevel);
      return READONLY_REGION_ADDR_TYPES.includes(segmType) || addrLevel === 1 || addrLevel === 2;
    },
    isReadonlyRegionSegmType(segmType) {
      return READONLY_REGION_ADDR_TYPES.includes(String(segmType || '').trim());
    },
    hasQueryCondition() {
      return !!(this.query?.standName?.trim() || this.query?.segmType);
    },
    clearListResult() {
      this.list = [];
      this.total = 0;
    },
    async fetchList() {
      if (!this.hasQueryCondition()) {
        this.clearListResult();
        this.$message.warning('请输入标准地址关键词或选择级别后再查询');
        return;
      }
      if (this.isReadonlyRegionSegmType(this.query?.segmType)) {
        this.clearListResult();
        this.$message.warning('一二级标准地址不支持拆分，请选择三级及以下标准地址');
        return;
      }
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
      this.clearListResult();
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchList();
    },
    selectSource(row) {
      this.selectedSource = row;
      if (!this.splitItems.length) {
        this.addSplitItem();
      }
    },
    cancelSelectedSource() {
      this.selectedSource = null;
      this.splitItems = [];
    },
    addSplitItem() {
      this.splitItems.push({ segmName: '' });
    },
    removeSplitItem(index) {
      this.splitItems.splice(index, 1);
    },
    buildSubmitPayload() {
      return {
        sourceSegmId: this.selectedSource?.segmId || '',
        splitItems: (this.splitItems || [])
          .map(item => ({ segmName: (item.segmName || '').trim() }))
          .filter(item => item.segmName)
      };
    },
    canSubmit() {
      const payload = this.buildSubmitPayload();
      return !!(payload.sourceSegmId && payload.splitItems.length);
    },
    levelLabel(row) {
      if (!row) {
        return '-';
      }
      return this.levelMap[row.segmType] || (row.addrLevel ? `${row.addrLevel}级` : '-');
    },
    async submitSplit() {
      const payload = this.buildSubmitPayload();
      if (!payload.sourceSegmId) {
        this.$message.warning('请先选择待拆分地址');
        return;
      }
      if (!payload.splitItems.length) {
        this.$message.warning('请至少填写一条拆分地址项');
        return;
      }
      this.submitLoading = true;
      try {
        await splitStandardAddress(payload.sourceSegmId, payload.splitItems);
        this.$message.success(APPROVAL_SUCCESS_MESSAGE);
        this.cancelSelectedSource();
        await this.fetchList();
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '拆分失败');
      } finally {
        this.submitLoading = false;
      }
    }
  }
};
</script>

<style scoped>
.split-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pager {
  margin-top: 16px;
  text-align: right;
}

.source-bar,
.config-head,
.submit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.split-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.split-item {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) minmax(0, 1fr) 72px;
  gap: 12px;
  align-items: center;
  padding: 14px;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  background: #fafbfd;
}

.split-item__title,
.empty-tip {
  color: #909399;
  font-size: 13px;
}

@media (max-width: 900px) {
  .split-item {
    grid-template-columns: 1fr;
  }
}
</style>
