<template>
  <div class="approval-page">
    <el-card shadow="never">
      <div class="page-head">
        <div>
          <div class="page-title">待审批地址管理</div>
          <div class="page-tip">覆盖我提交的、待审批、已审批三类审批视图。</div>
        </div>
        <el-button type="primary" plain @click="$router.push('/standard/list')">返回标准地址列表</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-click="handleTabChange">
        <el-tab-pane label="我提交的" name="mine">
          <div class="filters">
            <el-input
              v-model="mineQuery.keyword"
              clearable
              placeholder="请输入标题或地址摘要"
              @keyup.enter.native="fetchMineList"
            />
            <el-select v-model="mineQuery.operationType" clearable placeholder="操作类型">
              <el-option
                v-for="item in operationTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-select v-model="mineQuery.approvalStatus" clearable placeholder="审批状态">
              <el-option
                v-for="item in approvalStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-button type="primary" @click="fetchMineList">查询</el-button>
            <el-button @click="resetMineQuery">重置</el-button>
          </div>

          <el-table v-loading="mineLoading" :data="mineList" border size="small">
            <el-table-column label="序号" width="70">
              <template #default="{ $index }">
                {{ (minePageNum - 1) * minePageSize + $index + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="applyNo" label="申请单号" min-width="180" show-overflow-tooltip />
            <el-table-column prop="bizTitle" label="业务标题" min-width="220" show-overflow-tooltip />
            <el-table-column label="操作类型" width="110">
              <template #default="{ row }">
                <el-tag effect="plain">{{ operationTypeLabel(row.operationType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审批状态" width="120">
              <template #default="{ row }">
                <el-tag :type="approvalStatusTagType(row.approvalStatus)" effect="plain">
                  {{ approvalStatusLabel(row.approvalStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitDeptName" label="提交部门" width="140" />
            <el-table-column prop="createTime" label="提交时间" width="180" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button size="mini" @click="openDetailByBusinessId(row.id)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              layout="total, prev, pager, next"
              :total="mineTotal"
              :current-page="minePageNum"
              :page-size="minePageSize"
              @current-change="changeMinePage"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="待审批" name="wait">
          <el-alert
            v-if="waitActionPermission.loaded && !waitActionPermission.canApprove"
            :closable="false"
            class="permission-alert"
            show-icon
            type="warning"
            :title="waitActionPermission.message"
          />
          <div class="filters">
            <el-input
              v-model="waitQuery.keyword"
              clearable
              placeholder="请输入业务标题"
              @keyup.enter.native="fetchWaitList"
            />
            <el-button type="primary" @click="fetchWaitList">查询</el-button>
            <el-button @click="resetWaitQuery">重置</el-button>
          </div>

          <el-table v-loading="waitLoading" :data="waitList" border size="small">
            <el-table-column label="序号" width="70">
              <template #default="{ $index }">
                {{ (waitPageNum - 1) * waitPageSize + $index + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="businessCode" label="申请单号" min-width="180" show-overflow-tooltip />
            <el-table-column prop="businessTitle" label="业务标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="nodeName" label="当前节点" width="140" />
            <el-table-column prop="createByName" label="申请人" width="120" />
            <el-table-column prop="createTime" label="发起时间" width="180" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button size="mini" @click="openDetailByBusinessId(row.businessId)">详情</el-button>
                <el-button
                  v-if="waitActionPermission.canApprove"
                  size="mini"
                  type="primary"
                  plain
                  @click="approveTask(row)"
                >通过</el-button>
                <el-button
                  v-if="waitActionPermission.canApprove"
                  size="mini"
                  type="danger"
                  plain
                  @click="rejectTask(row)"
                >驳回</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              layout="total, prev, pager, next"
              :total="waitTotal"
              :current-page="waitPageNum"
              :page-size="waitPageSize"
              @current-change="changeWaitPage"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="已审批" name="finish">
          <div class="filters">
            <el-input
              v-model="finishQuery.keyword"
              clearable
              placeholder="请输入业务标题"
              @keyup.enter.native="fetchFinishList"
            />
            <el-button type="primary" @click="fetchFinishList">查询</el-button>
            <el-button type="success" plain @click="exportFinishList">导出</el-button>
            <el-button @click="resetFinishQuery">重置</el-button>
          </div>

          <el-table v-loading="finishLoading" :data="finishList" border size="small">
            <el-table-column label="序号" width="70">
              <template #default="{ $index }">
                {{ (finishPageNum - 1) * finishPageSize + $index + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="businessCode" label="申请单号" min-width="180" show-overflow-tooltip />
            <el-table-column prop="businessTitle" label="业务标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="approvalResult" label="审批结果" width="140" />
            <el-table-column prop="approveName" label="审批人" width="120" />
            <el-table-column prop="updateTime" label="审批完成时间" width="180" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button size="mini" @click="openDetailByBusinessId(row.businessId)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              layout="total, prev, pager, next"
              :total="finishTotal"
              :current-page="finishPageNum"
              :page-size="finishPageSize"
              @current-change="changeFinishPage"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-drawer :visible.sync="detailVisible" size="52%" title="审批详情">
      <div v-loading="detailLoading" class="detail-wrap">
        <el-descriptions v-if="detail" :column="2" border>
          <el-descriptions-item label="申请单号">{{ detail.applyNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务标题">{{ detail.bizTitle || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作类型">{{ operationTypeLabel(detail.operationType) }}</el-descriptions-item>
          <el-descriptions-item label="审批状态">
            <el-tag :type="approvalStatusTagType(detail.approvalStatus)" effect="plain">
              {{ approvalStatusLabel(detail.approvalStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交人">{{ detail.submitUserName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交部门">{{ detail.submitDeptName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批人">{{ detail.approveUserName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批时间">{{ detail.approveTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="原地址摘要" :span="2">{{ detail.sourceSummary || '-' }}</el-descriptions-item>
          <el-descriptions-item label="新地址摘要" :span="2">{{ detail.targetSummary || '-' }}</el-descriptions-item>
          <el-descriptions-item label="驳回原因" :span="2">{{ detail.rejectReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="执行信息" :span="2">{{ detail.executeMessage || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="snapshot-section">
          <div class="snapshot-card">
            <div class="snapshot-title">原地址快照</div>
            <pre class="snapshot-pre">{{ formatSnapshotText(detail && detail.sourceSnapshot) }}</pre>
          </div>
          <div class="snapshot-card">
            <div class="snapshot-title">新地址快照</div>
            <pre class="snapshot-pre">{{ formatSnapshotText((detail && detail.targetSnapshot) || (detail && detail.importBatchPayload)) }}</pre>
          </div>
        </div>

        <el-card shadow="never">
          <div slot="header">审批记录</div>
          <el-table :data="historyList" border size="small">
            <el-table-column prop="nodeName" label="节点" min-width="120" />
            <el-table-column prop="approveName" label="审批人" width="120" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                {{ historyStatusLabel(row) }}
              </template>
            </el-table-column>
            <el-table-column prop="message" label="意见" min-width="200" show-overflow-tooltip />
            <el-table-column prop="updateTime" label="完成时间" width="180" />
          </el-table>
        </el-card>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {
  approveStandardApproval,
  exportStandardApprovalHandled,
  getStandardApprovalByBusinessId,
  getStandardApprovalActionPermission,
  getStandardApprovalHandledPage,
  getStandardApprovalMyPage,
  getWorkflowAllTaskWait,
  getWorkflowHistory,
  rejectStandardApproval
} from '../api/address';

const FLOW_CODE = 'address_standard_approve_v1';

export default {
  data() {
    return {
      activeTab: 'mine',
      operationTypeOptions: [
        { label: '新增', value: 'ADD' },
        { label: '编辑', value: 'UPDATE' },
        { label: '删除', value: 'DELETE' },
        { label: '导入', value: 'IMPORT' },
        { label: '合并', value: 'MERGE' },
        { label: '拆分', value: 'SPLIT' }
      ],
      approvalStatusOptions: [
        { label: '待审批', value: 'WAITING' },
        { label: '审批通过', value: 'APPROVED' },
        { label: '审批驳回', value: 'REJECTED' },
        { label: '执行中', value: 'EXECUTING' },
        { label: '执行失败', value: 'EXECUTE_FAILED' }
      ],
      mineQuery: {
        keyword: '',
        operationType: '',
        approvalStatus: ''
      },
      waitQuery: {
        keyword: ''
      },
      waitActionPermission: {
        loaded: false,
        canApprove: false,
        message: ''
      },
      finishQuery: {
        keyword: ''
      },
      mineList: [],
      waitList: [],
      finishList: [],
      mineTotal: 0,
      waitTotal: 0,
      finishTotal: 0,
      minePageNum: 1,
      waitPageNum: 1,
      finishPageNum: 1,
      minePageSize: 10,
      waitPageSize: 10,
      finishPageSize: 10,
      mineLoading: false,
      waitLoading: false,
      finishLoading: false,
      detailVisible: false,
      detailLoading: false,
      detail: null,
      historyList: []
    };
  },
  mounted() {
    this.loadTabData();
  },
  methods: {
    operationTypeLabel(value) {
      return this.operationTypeOptions.find(item => item.value === value)?.label || value || '-';
    },
    approvalStatusLabel(value) {
      return this.approvalStatusOptions.find(item => item.value === value)?.label || value || '-';
    },
    approvalStatusTagType(value) {
      const map = {
        WAITING: 'warning',
        APPROVED: 'success',
        REJECTED: 'danger',
        EXECUTING: 'primary',
        EXECUTE_FAILED: 'info'
      };
      return map[value] || 'info';
    },
    historyStatusLabel(row) {
      if (row?.skipType === 'REJECT') {
        return '已驳回';
      }
      if (row?.updateTime) {
        return '已处理';
      }
      return '待处理';
    },
    buildMineQueryParams() {
      const params = {
        pageNum: this.minePageNum,
        pageSize: this.minePageSize
      };
      if (this.mineQuery.keyword) {
        params.keyword = this.mineQuery.keyword.trim();
      }
      if (this.mineQuery.operationType) {
        params.operationType = this.mineQuery.operationType;
      }
      if (this.mineQuery.approvalStatus) {
        params.approvalStatus = this.mineQuery.approvalStatus;
      }
      return params;
    },
    buildWorkflowQueryParams(tabName) {
      const query = this.waitQuery;
      const pageNum = this.waitPageNum;
      const pageSize = this.waitPageSize;
      const params = {
        flowCode: FLOW_CODE,
        pageNum,
        pageSize
      };
      if (query.keyword) {
        params.businessTitle = query.keyword.trim();
      }
      return params;
    },
    buildFinishQueryParams() {
      const params = {
        pageNum: this.finishPageNum,
        pageSize: this.finishPageSize
      };
      if (this.finishQuery.keyword) {
        params.keyword = this.finishQuery.keyword.trim();
      }
      return params;
    },
    buildFinishExportParams() {
      const params = {};
      if (this.finishQuery.keyword) {
        params.keyword = this.finishQuery.keyword.trim();
      }
      return params;
    },
    normalizeWaitActionPermission(permission) {
      const canApprove = Boolean(permission?.canApprove);
      return {
        loaded: true,
        canApprove,
        message: canApprove
          ? ''
          : (permission?.message || '当前用户没有标准地址审批权限，请联系管理员授权“标准地址审批员”角色或使用系统管理员账号办理')
      };
    },
    mapHandledApprovalToFinishRow(row) {
      return {
        businessId: row.id,
        businessCode: row.applyNo,
        businessTitle: row.bizTitle,
        approvalResult: this.approvalStatusLabel(row.approvalStatus),
        approveName: row.approveUserName,
        updateTime: row.approveTime
      };
    },
    formatSnapshotText(payload) {
      if (!payload) {
        return '-';
      }
      if (typeof payload !== 'string') {
        return JSON.stringify(payload, null, 2);
      }
      try {
        return JSON.stringify(JSON.parse(payload), null, 2);
      } catch (err) {
        return payload;
      }
    },
    downloadBlob(blob, filename) {
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(downloadUrl);
    },
    async loadTabData() {
      if (this.activeTab === 'wait') {
        await this.fetchWaitActionPermission();
        await this.fetchWaitList();
        return;
      }
      if (this.activeTab === 'finish') {
        await this.fetchFinishList();
        return;
      }
      await this.fetchMineList();
    },
    async fetchWaitActionPermission() {
      try {
        const res = await getStandardApprovalActionPermission();
        this.waitActionPermission = this.normalizeWaitActionPermission(res.data);
      } catch (err) {
        this.waitActionPermission = {
          loaded: true,
          canApprove: true,
          message: ''
        };
      }
    },
    async handleTabChange() {
      await this.loadTabData();
    },
    async fetchMineList() {
      this.mineLoading = true;
      try {
        const res = await getStandardApprovalMyPage(this.buildMineQueryParams());
        this.mineList = res.rows || [];
        this.mineTotal = res.total || 0;
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '审批记录查询失败');
      } finally {
        this.mineLoading = false;
      }
    },
    async fetchWaitList() {
      this.waitLoading = true;
      try {
        const res = await getWorkflowAllTaskWait(this.buildWorkflowQueryParams());
        this.waitList = res.rows || [];
        this.waitTotal = res.total || 0;
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '待审批任务查询失败');
      } finally {
        this.waitLoading = false;
      }
    },
    async fetchFinishList() {
      this.finishLoading = true;
      try {
        const res = await getStandardApprovalHandledPage(this.buildFinishQueryParams());
        this.finishList = (res.rows || []).map(row => this.mapHandledApprovalToFinishRow(row));
        this.finishTotal = res.total || 0;
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '已审批任务查询失败');
      } finally {
        this.finishLoading = false;
      }
    },
    async exportFinishList() {
      if (!this.finishTotal) {
        this.$message.warning('当前没有可导出的已审批记录');
        return;
      }
      try {
        const blob = await exportStandardApprovalHandled(this.buildFinishExportParams());
        this.downloadBlob(blob, '标准地址已审批记录.xlsx');
        this.$message.success('已审批记录导出成功');
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '已审批记录导出失败');
      }
    },
    resetMineQuery() {
      this.mineQuery = {
        keyword: '',
        operationType: '',
        approvalStatus: ''
      };
      this.minePageNum = 1;
      this.fetchMineList();
    },
    resetWaitQuery() {
      this.waitQuery = { keyword: '' };
      this.waitPageNum = 1;
      this.fetchWaitList();
    },
    resetFinishQuery() {
      this.finishQuery = { keyword: '' };
      this.finishPageNum = 1;
      this.fetchFinishList();
    },
    changeMinePage(page) {
      this.minePageNum = page;
      this.fetchMineList();
    },
    changeWaitPage(page) {
      this.waitPageNum = page;
      this.fetchWaitList();
    },
    changeFinishPage(page) {
      this.finishPageNum = page;
      this.fetchFinishList();
    },
    async openDetailByBusinessId(businessId) {
      if (!businessId) {
        this.$message.warning('业务ID不能为空');
        return;
      }
      this.detailVisible = true;
      this.detailLoading = true;
      try {
        const [approvalRes, historyRes] = await Promise.all([
          getStandardApprovalByBusinessId(businessId),
          getWorkflowHistory(businessId)
        ]);
        this.detail = approvalRes.data || null;
        this.historyList = historyRes.data?.list || [];
      } catch (err) {
        this.detail = null;
        this.historyList = [];
        this.$message.error(err?.friendlyMessage || err?.message || '审批详情查询失败');
      } finally {
        this.detailLoading = false;
      }
    },
    async approveTask(row) {
      if (!this.waitActionPermission.canApprove) {
        this.$message.warning(this.waitActionPermission.message);
        return;
      }
      try {
        await this.$confirm(`确认通过“${row.businessTitle || row.businessCode || row.id}”吗？`, '审批通过');
        await approveStandardApproval({
          taskId: row.id,
          businessId: row.businessId,
          message: '审批通过'
        });
        this.$message.success('审批已通过');
        await this.fetchWaitList();
        if (this.detailVisible && String(this.detail?.id) === String(row.businessId)) {
          await this.openDetailByBusinessId(row.businessId);
        }
      } catch (err) {
        if (err === 'cancel' || err === 'close') {
          return;
        }
        this.$message.error(err?.friendlyMessage || err?.message || '审批通过失败');
      }
    },
    async rejectTask(row) {
      if (!this.waitActionPermission.canApprove) {
        this.$message.warning(this.waitActionPermission.message);
        return;
      }
      try {
        const { value } = await this.$prompt('请输入驳回原因', '审批驳回', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputValidator: input => Boolean(String(input || '').trim()),
          inputErrorMessage: '驳回原因不能为空'
        });
        await rejectStandardApproval({
          taskId: row.id,
          businessId: row.businessId,
          nodeCode: row.nodeCode,
          message: value.trim()
        });
        this.$message.success('审批已驳回');
        await this.fetchWaitList();
        if (this.detailVisible && String(this.detail?.id) === String(row.businessId)) {
          await this.openDetailByBusinessId(row.businessId);
        }
      } catch (err) {
        if (err === 'cancel' || err === 'close') {
          return;
        }
        this.$message.error(err?.friendlyMessage || err?.message || '审批驳回失败');
      }
    }
  }
};
</script>

<style scoped>
.approval-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.page-tip {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.filters {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.permission-alert {
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  text-align: right;
}

.detail-wrap {
  padding: 0 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.snapshot-section {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.snapshot-card {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.snapshot-title {
  padding: 12px 16px;
  font-weight: 600;
  color: #303133;
  background: #f8f9fb;
  border-bottom: 1px solid #ebeef5;
}

.snapshot-pre {
  margin: 0;
  padding: 16px;
  min-height: 200px;
  max-height: 360px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  background: #0f172a;
  color: #e2e8f0;
}

@media (max-width: 1200px) {
  .snapshot-section {
    grid-template-columns: 1fr;
  }
}
</style>
