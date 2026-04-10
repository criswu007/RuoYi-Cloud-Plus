import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockRequest } = vi.hoisted(() => ({
  mockRequest: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    postForm: vi.fn(),
    postMultipart: vi.fn(),
    postDownload: vi.fn()
  }
}));

vi.mock('./request', () => ({
  default: mockRequest
}));

import * as addressApi from './address';

describe('标准地址 API 契约', () => {
  beforeEach(() => {
    Object.values(mockRequest).forEach(fn => fn.mockReset());
  });

  it('列表查询应使用 POST form 提交 canonical 查询字段', () => {
    const params = {
      standName: '中央路',
      segmType: '180004',
      pageNum: 1,
      pageSize: 20
    };

    addressApi.getStandardAddressList(params);

    expect(mockRequest.postForm).toHaveBeenCalledWith('/address/standard/list', params);
  });

  it('详情查询应使用 POST segmId 路径', () => {
    addressApi.getStandardAddressDetail('000102010000000011800001');

    expect(mockRequest.post).toHaveBeenCalledWith('/address/standard/000102010000000011800001');
  });

  it('级别选项查询应使用 POST /levelOptions', () => {
    expect(typeof addressApi.getStandardAddressLevelOptions).toBe('function');

    addressApi.getStandardAddressLevelOptions();

    expect(mockRequest.post).toHaveBeenCalledWith('/address/standard/levelOptions');
  });

  it('编辑页聚合字典应使用 POST /formOptions', () => {
    expect(typeof addressApi.getStandardAddressFormOptions).toBe('function');

    addressApi.getStandardAddressFormOptions();

    expect(mockRequest.post).toHaveBeenCalledWith('/address/standard/formOptions');
  });

  it('管理站候选查询应使用 POST /stationOptions', () => {
    expect(typeof addressApi.getStandardAddressStationOptions).toBe('function');

    const payload = {
      regionId: '320100',
      manageType: '2017101',
      keyword: '洪武',
      limit: 20
    };

    addressApi.getStandardAddressStationOptions(payload);

    expect(mockRequest.post).toHaveBeenCalledWith('/address/standard/stationOptions', payload);
  });

  it('更新接口应使用 POST /update', () => {
    const payload = { segmId: '0001', segmName: '101室' };

    addressApi.updateStandardAddress(payload);

    expect(mockRequest.post).toHaveBeenCalledWith('/address/standard/update', payload);
  });

  it('删除接口应使用 POST form 提交 confirm', () => {
    addressApi.deleteStandardAddresses('0001,0002', true);

    expect(mockRequest.postForm).toHaveBeenCalledWith('/address/standard/remove/0001,0002', { confirm: true });
  });

  it('批量预览接口应显式暴露并使用 POST', () => {
    expect(typeof addressApi.previewStandardAddressChildren).toBe('function');

    const payload = {
      parentSegmId: '0001',
      prefix: '第',
      startNum: 1,
      endNum: 3,
      suffix: '单元'
    };

    addressApi.previewStandardAddressChildren(payload);

    expect(mockRequest.post).toHaveBeenCalledWith('/address/standard/batchPreviewChild', payload);
  });

  it('父级候选搜索应使用 POST form，而不是 GET', () => {
    const params = {
      keyword: '金鹰国际花园',
      limit: 200
    };

    addressApi.searchSelectionStandardAddresses(params);

    expect(mockRequest.postForm).toHaveBeenCalledWith('/address/selection/search', params);
  });

  it('合并接口应使用 sourceSegmIds 与 targetSegmId', () => {
    addressApi.mergeStandardAddresses(['0001', '0002'], '0003');

    expect(mockRequest.post).toHaveBeenCalledWith('/address/standard/merge', {
      sourceSegmIds: ['0001', '0002'],
      targetSegmId: '0003'
    });
  });

  it('拆分接口应使用 sourceSegmId 与 splitItems', () => {
    addressApi.splitStandardAddress('0001', [{ segmName: '1单元' }]);

    expect(mockRequest.post).toHaveBeenCalledWith('/address/standard/split', {
      sourceSegmId: '0001',
      splitItems: [{ segmName: '1单元' }]
    });
  });

  it('导入接口应使用 /address/standard/import', () => {
    const file = new File(['demo'], 'standard-address.xlsx');

    addressApi.importStandardAddressData(file, true);

    expect(mockRequest.postMultipart).toHaveBeenCalledTimes(1);
    expect(mockRequest.postMultipart.mock.calls[0][0]).toBe('/address/standard/import');
  });

  it('应暴露导入模板下载与失败明细导出接口', () => {
    addressApi.downloadStandardAddressImportTemplate();
    addressApi.exportImportFailDetails(9001);

    expect(mockRequest.postDownload).toHaveBeenNthCalledWith(1, '/address/standard/import/template');
    expect(mockRequest.postDownload).toHaveBeenNthCalledWith(2, '/address/import/batch/failure/export/9001');
  });

  it('导入记录查询应使用 POST form 与批次详情路径', () => {
    const params = {
      fileName: 'standard-address.xlsx',
      segmName: '紫峰大厦',
      pageNum: 1,
      pageSize: 10
    };

    addressApi.getImportRecords(params);
    addressApi.getImportBatchDetail(9001);

    expect(mockRequest.postForm).toHaveBeenCalledWith('/address/import/batch/list', params);
    expect(mockRequest.post).toHaveBeenCalledWith('/address/import/batch/9001');
  });

  it('操作日志查询与详情应使用 POST', () => {
    const params = {
      operationType: 'MERGE',
      pageNum: 1,
      pageSize: 10
    };

    addressApi.getOperationLogs(params);
    addressApi.getOperationLogDetail(1001);

    expect(mockRequest.postForm).toHaveBeenCalledWith('/address/operation-log/list', params);
    expect(mockRequest.post).toHaveBeenCalledWith('/address/operation-log/1001');
  });

  it('安装地址查询与维护应使用 POST 契约', () => {
    const params = {
      setAddrName: '紫峰大厦1单元',
      pageNum: 1,
      pageSize: 10
    };
    const payload = { setAddrId: '000102010000000011800001', setAddrName: '紫峰大厦2单元' };

    addressApi.getInstallationList(params);
    addressApi.getInstallationDetail('000102010000000011800001');
    addressApi.updateInstallationAddress(payload);
    addressApi.deleteInstallationAddresses('000102010000000011800001');

    expect(mockRequest.postForm).toHaveBeenCalledWith('/address/installation/list', params);
    expect(mockRequest.post).toHaveBeenCalledWith('/address/installation/000102010000000011800001');
    expect(mockRequest.post).toHaveBeenCalledWith('/address/installation/update', payload);
    expect(mockRequest.post).toHaveBeenCalledWith('/address/installation/remove/000102010000000011800001');
  });

  it('标签管理接口应使用 POST 契约', () => {
    const params = { name: '高价值', pageNum: 1, pageSize: 10 };
    const payload = { id: 1, name: '高价值客户' };

    addressApi.getTagList(params);
    addressApi.getTagDetail(1);
    addressApi.updateTag(payload);
    addressApi.deleteTag('1');
    addressApi.getStandardAddressTags('000102010000000011800001');

    expect(mockRequest.postForm).toHaveBeenCalledWith('/address/tag/list', params);
    expect(mockRequest.post).toHaveBeenCalledWith('/address/tag/1');
    expect(mockRequest.post).toHaveBeenCalledWith('/address/tag/update', payload);
    expect(mockRequest.post).toHaveBeenCalledWith('/address/tag/remove/1');
    expect(mockRequest.post).toHaveBeenCalledWith('/address/tag/standardAddress/000102010000000011800001');
  });

  it('管理站管理接口应使用 POST 契约', () => {
    const params = { name: '洪武路管理站', pageNum: 1, pageSize: 10 };
    const createPayload = { name: '新街口管理站' };
    const updatePayload = { id: 101, name: '新街口管理站-更新' };

    addressApi.getManagementStationList(params);
    addressApi.createManagementStation(createPayload);
    addressApi.updateManagementStation(updatePayload);
    addressApi.deleteManagementStation('101');

    expect(mockRequest.postForm).toHaveBeenCalledWith('/address/station/list', params);
    expect(mockRequest.post).toHaveBeenCalledWith('/address/station', createPayload);
    expect(mockRequest.post).toHaveBeenCalledWith('/address/station/update', updatePayload);
    expect(mockRequest.post).toHaveBeenCalledWith('/address/station/remove/101');
  });

  it('ES 运维接口应暴露概览、重建任务与 repair 契约', () => {
    addressApi.getSearchOpsOverview();
    addressApi.createStandardRebuildTask({ confirmationCode: 'REBUILD_STANDARD' });
    addressApi.createInstallationRebuildTask({ confirmationCode: 'REBUILD_INSTALLATION' });
    addressApi.getSearchMaintenanceTasks({ status: 'RUNNING', pageNum: 1, pageSize: 10 });
    addressApi.getSearchMaintenanceTaskDetail(9001);
    addressApi.getSearchRepairTasks({ status: 'FAILED', pageNum: 1, pageSize: 10 });
    addressApi.executeSearchRepairTask(88, { confirmationCode: 'REPLAY_REPAIR' });

    expect(mockRequest.get).toHaveBeenNthCalledWith(1, '/address/search/ops/overview');
    expect(mockRequest.post).toHaveBeenNthCalledWith(1, '/address/search/tasks/rebuild/standard', { confirmationCode: 'REBUILD_STANDARD' });
    expect(mockRequest.post).toHaveBeenNthCalledWith(2, '/address/search/tasks/rebuild/installation', { confirmationCode: 'REBUILD_INSTALLATION' });
    expect(mockRequest.get).toHaveBeenNthCalledWith(2, '/address/search/tasks', { params: { status: 'RUNNING', pageNum: 1, pageSize: 10 } });
    expect(mockRequest.get).toHaveBeenNthCalledWith(3, '/address/search/tasks/9001');
    expect(mockRequest.get).toHaveBeenNthCalledWith(4, '/address/search/repair/tasks', { params: { status: 'FAILED', pageNum: 1, pageSize: 10 } });
    expect(mockRequest.post).toHaveBeenNthCalledWith(3, '/address/search/repair/88/execute', { confirmationCode: 'REPLAY_REPAIR' });
  });

  it('应暴露标准地址审批记录与 workflow 审批接口', () => {
    const pageParams = {
      keyword: '莲花新城南苑',
      pageNum: 1,
      pageSize: 10
    };
    const completePayload = {
      taskId: 9001,
      message: '同意',
      variables: { ignore: true }
    };
    const rejectPayload = {
      taskId: 9002,
      nodeCode: 'stdaddr-approve',
      message: '请补充备注',
      variables: { ignore: true }
    };

    addressApi.getStandardApprovalMyPage(pageParams);
    addressApi.getStandardApprovalHandledPage(pageParams);
    addressApi.exportStandardApprovalHandled(pageParams);
    addressApi.getStandardApprovalDetail(1001);
    addressApi.getStandardApprovalByBusinessId('1001');
    addressApi.getWorkflowAllTaskWait(pageParams);
    addressApi.completeWorkflowTask(completePayload);
    addressApi.backWorkflowTask(rejectPayload);
    addressApi.approveStandardApproval({ taskId: 9001, businessId: '1001', message: '审批通过' });
    addressApi.rejectStandardApproval({ taskId: 9002, businessId: '1001', nodeCode: 'stdaddr-approve', message: '请补充备注' });
    addressApi.getWorkflowHistory('1001');

    expect(mockRequest.postForm).toHaveBeenCalledWith('/address/standard/approval/my/page', pageParams);
    expect(mockRequest.postForm).toHaveBeenCalledWith('/address/standard/approval/handled/page', pageParams);
    expect(mockRequest.postDownload).toHaveBeenCalledWith('/address/standard/approval/handled/export?keyword=%E8%8E%B2%E8%8A%B1%E6%96%B0%E5%9F%8E%E5%8D%97%E8%8B%91&pageNum=1&pageSize=10');
    expect(mockRequest.post).toHaveBeenCalledWith('/address/standard/approval/1001');
    expect(mockRequest.post).toHaveBeenCalledWith('/address/standard/approval/business/1001');
    expect(mockRequest.get).toHaveBeenCalledWith('/workflow/task/pageByAllTaskWait', { params: pageParams });
    expect(mockRequest.post).toHaveBeenCalledWith('/workflow/task/completeTask', completePayload);
    expect(mockRequest.post).toHaveBeenCalledWith('/workflow/task/backProcess', rejectPayload);
    expect(mockRequest.post).toHaveBeenCalledWith('/address/standard/approval/approve', { taskId: 9001, businessId: '1001', message: '审批通过' });
    expect(mockRequest.post).toHaveBeenCalledWith('/address/standard/approval/reject', { taskId: 9002, businessId: '1001', nodeCode: 'stdaddr-approve', message: '请补充备注' });
    expect(mockRequest.get).toHaveBeenCalledWith('/workflow/instance/flowHisTaskList/1001');
  });
});
