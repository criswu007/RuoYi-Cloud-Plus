import { describe, expect, it } from 'vitest';
import StandardApprovalRecords from './StandardApprovalRecords.vue';

describe('标准地址审批记录页面状态模型', () => {
  it('默认应展示我提交的审批页签与独立分页状态', () => {
    const state = StandardApprovalRecords.data();

    expect(state.activeTab).toBe('mine');
    expect(state.mineQuery).toEqual({
      keyword: '',
      operationType: '',
      approvalStatus: ''
    });
    expect(state.waitQuery).toEqual({
      keyword: ''
    });
    expect(state.waitActionPermission).toEqual({
      loaded: false,
      canApprove: false,
      message: ''
    });
    expect(state.finishQuery).toEqual({
      keyword: ''
    });
  });

  it('待审批权限结果应归一化为页面可直接消费的状态', () => {
    const result = StandardApprovalRecords.methods.normalizeWaitActionPermission.call({}, {
      canApprove: false,
      message: '当前用户没有标准地址审批权限'
    });

    expect(result).toEqual({
      loaded: true,
      canApprove: false,
      message: '当前用户没有标准地址审批权限'
    });
  });

  it('待审批权限为空时应回退默认提示文案', () => {
    const result = StandardApprovalRecords.methods.normalizeWaitActionPermission.call({}, {});

    expect(result).toEqual({
      loaded: true,
      canApprove: false,
      message: '当前用户没有标准地址审批权限，请联系管理员授权“标准地址审批员”角色或使用系统管理员账号办理'
    });
  });

  it('待审批查询参数应固定携带流程编码', () => {
    const params = StandardApprovalRecords.methods.buildWorkflowQueryParams.call({
      waitQuery: {
        keyword: '莲花新城南苑'
      },
      waitPageNum: 2,
      waitPageSize: 10
    }, 'wait');

    expect(params).toEqual({
      businessTitle: '莲花新城南苑',
      flowCode: 'address_standard_approve_v1',
      pageNum: 2,
      pageSize: 10
    });
  });

  it('已审批查询参数应仅包含关键字与分页', () => {
    const params = StandardApprovalRecords.methods.buildFinishQueryParams.call({
      finishQuery: {
        keyword: '莲花新城南苑'
      },
      finishPageNum: 3,
      finishPageSize: 20
    });

    expect(params).toEqual({
      keyword: '莲花新城南苑',
      pageNum: 3,
      pageSize: 20
    });
  });

  it('已审批导出参数应仅保留当前筛选条件', () => {
    const params = StandardApprovalRecords.methods.buildFinishExportParams.call({
      finishQuery: {
        keyword: '莲花新城南苑'
      }
    });

    expect(params).toEqual({
      keyword: '莲花新城南苑'
    });
  });

  it('已审批列表应映射为审批结果展示模型', () => {
    const result = StandardApprovalRecords.methods.mapHandledApprovalToFinishRow.call({
      approvalStatusLabel: value => ({
        APPROVED: '审批通过'
      }[value] || value)
    }, {
      id: 1001,
      applyNo: 'STDADDRAPP1001',
      bizTitle: '标准地址编辑审批-莲花新城南苑',
      approvalStatus: 'APPROVED',
      approveUserName: 'approver',
      approveTime: '2026-04-07 13:00:00'
    });

    expect(result).toEqual({
      businessId: 1001,
      businessCode: 'STDADDRAPP1001',
      businessTitle: '标准地址编辑审批-莲花新城南苑',
      approvalResult: '审批通过',
      approveName: 'approver',
      updateTime: '2026-04-07 13:00:00'
    });
  });

  it('详情快照应优先把 JSON 字符串格式化为可读文本', () => {
    const text = StandardApprovalRecords.methods.formatSnapshotText.call({}, '[{\"standName\":\"莲花新城南苑\"}]');

    expect(text).toContain('莲花新城南苑');
    expect(text).toContain('\n');
  });
});
