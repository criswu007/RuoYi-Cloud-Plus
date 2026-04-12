import request from './request';

function toQueryString(params = {}) {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }
    searchParams.append(key, value);
  });
  const queryString = searchParams.toString();
  return queryString ? `?${queryString}` : '';
}

export const getStandardAddressList = params =>
  request.postForm('/address/standard/list', params);

export const getStandardAddressDetail = segmId =>
  request.post(`/address/standard/${segmId}`);

export const getStandardAddressLevelOptions = () =>
  request.post('/address/standard/levelOptions');

export const getStandardAddressFormOptions = () =>
  request.post('/address/standard/formOptions');

export const getStandardAddressStationOptions = data =>
  request.post('/address/standard/stationOptions', data);

export const createStandardAddress = data =>
  request.post('/address/standard', data);

export const updateStandardAddress = data =>
  request.post('/address/standard/update', data);

export const deleteStandardAddresses = (segmIds, confirm = false) =>
  request.postForm(`/address/standard/remove/${segmIds}`, { confirm });

export const mergeStandardAddresses = (sourceSegmIds, targetSegmId) =>
  request.post('/address/standard/merge', { sourceSegmIds, targetSegmId });

export const splitStandardAddress = (sourceSegmId, splitItems) =>
  request.post('/address/standard/split', { sourceSegmId, splitItems });

export const previewStandardAddressChildren = data =>
  request.post('/address/standard/batchPreviewChild', data);

export const batchAddStandardAddressChildren = data =>
  request.post('/address/standard/batchAddChild', data);

export const downloadStandardAddressImportTemplate = () =>
  request.postDownload('/address/standard/import/template');

export const importStandardAddressData = (file, updateSupport = false) => {
  const form = new FormData();
  form.append('file', file);
  form.append('updateSupport', updateSupport);
  return request.postMultipart('/address/standard/import', form);
};

export const getStandardApprovalMyPage = params =>
  request.postForm('/address/standard/approval/my/page', params);

export const getStandardApprovalHandledPage = params =>
  request.postForm('/address/standard/approval/handled/page', params);

export const exportStandardApprovalHandled = params =>
  request.postDownload(`/address/standard/approval/handled/export${toQueryString(params)}`);

export const getStandardApprovalDetail = id =>
  request.post(`/address/standard/approval/${id}`);

export const getStandardApprovalByBusinessId = businessId =>
  request.post(`/address/standard/approval/business/${businessId}`);

export const getStandardApprovalActionPermission = () =>
  request.post('/address/standard/approval/action/permission');

export const getWorkflowAllTaskWait = params =>
  request.get('/workflow/task/pageByAllTaskWait', { params });

export const getWorkflowAllTaskFinish = params =>
  request.get('/workflow/task/pageByAllTaskFinish', { params });

export const completeWorkflowTask = data =>
  request.post('/workflow/task/completeTask', data);

export const backWorkflowTask = data =>
  request.post('/workflow/task/backProcess', data);

export const approveStandardApproval = data =>
  request.post('/address/standard/approval/approve', data);

export const rejectStandardApproval = data =>
  request.post('/address/standard/approval/reject', data);

export const getWorkflowHistory = businessId =>
  request.get(`/workflow/instance/flowHisTaskList/${businessId}`);

export const getImportRecords = params =>
  request.postForm('/address/import/batch/list', params);

export const getImportBatchDetail = batchId =>
  request.post(`/address/import/batch/${batchId}`);

export const exportImportFailDetails = batchId =>
  request.postDownload(`/address/import/batch/failure/export/${batchId}`);

export const getOperationLogs = params =>
  request.postForm('/address/operation-log/list', params);

export const getOperationLogDetail = id =>
  request.post(`/address/operation-log/${id}`);

export const getInstallationList = params =>
  request.postForm('/address/installation/list', params);

export const getInstallationDetail = setAddrId =>
  request.post(`/address/installation/${setAddrId}`);

export const updateInstallationAddress = data =>
  request.post('/address/installation/update', data);

export const deleteInstallationAddresses = setAddrIds =>
  request.post(`/address/installation/remove/${setAddrIds}`);

export const searchSelectionStandardAddresses = params =>
  request.postForm('/address/selection/search', params);

export const createRoomStandardAddress = data =>
  request.post('/address/selection/room', data);

export const getTagList = params =>
  request.postForm('/address/tag/list', params);

export const getTagDetail = id =>
  request.post(`/address/tag/${id}`);

export const createTag = data =>
  request.post('/address/tag', data);

export const updateTag = data =>
  request.post('/address/tag/update', data);

export const deleteTag = ids =>
  request.post(`/address/tag/remove/${ids}`);

export const getStandardAddressTags = segmId =>
  request.post(`/address/tag/standardAddress/${segmId}`);

export const bindTagsToStandardAddresses = data =>
  request.post('/address/tag/bind', data);

export const unbindTagsFromStandardAddresses = data =>
  request.post('/address/tag/unbind', data);

export const getManagementStationList = params =>
  request.postForm('/address/station/list', params);

export const createManagementStation = data =>
  request.post('/address/station', data);

export const updateManagementStation = data =>
  request.post('/address/station/update', data);

export const deleteManagementStation = ids =>
  request.post(`/address/station/remove/${ids}`);

export const getMonitorRules = params =>
  request.postForm('/address/monitor/rule/list', params);

export const getMonitorRuleDetail = id =>
  request.post(`/address/monitor/rule/${id}`);

export const createMonitorRule = data =>
  request.post('/address/monitor/rule', data);

export const updateMonitorRule = data =>
  request.post('/address/monitor/rule/update', data);

export const deleteMonitorRule = ids =>
  request.post(`/address/monitor/rule/remove/${ids}`);

export const enableMonitorRule = ids =>
  request.post(`/address/monitor/rule/enable/${ids}`);

export const disableMonitorRule = ids =>
  request.post(`/address/monitor/rule/disable/${ids}`);

export const getMonitorRecords = params =>
  request.postForm('/address/monitor/record/list', params);

export const getMonitorRecordDetail = id =>
  request.post(`/address/monitor/record/${id}`);

export const updateMonitorRecord = data =>
  request.post('/address/monitor/record/update', data);

export const deleteMonitorRecord = ids =>
  request.post(`/address/monitor/record/remove/${ids}`);

export const ignoreMonitorRecord = ids =>
  request.post(`/address/monitor/record/ignore/${ids}`);

export const getMonitorWorkOrders = params =>
  request.postForm('/address/work-order/list', params);

export const getMonitorWorkOrderDetail = id =>
  request.post(`/address/work-order/${id}`);

export const createMonitorWorkOrder = data =>
  request.post('/address/work-order', data);

export const correctMonitorWorkOrder = (id, data) =>
  request.post(`/address/work-order/correct/${id}`, data);

export const rejectMonitorWorkOrder = (id, data) =>
  request.post(`/address/work-order/reject/${id}`, data);

export const getMonitorTaskSummary = () =>
  request.post('/address/monitor/task/summary');

export const executeMonitorTask = () =>
  request.post('/address/monitor/task/execute');

export const getMonitorTasks = params =>
  request.postForm('/address/monitor/task/list', params);

export const getMonitorTaskDetail = id =>
  request.post(`/address/monitor/task/${id}`);

export const getMonitorTaskRunLogs = (taskId, params) =>
  request.postForm(`/address/monitor/task/runs/${taskId}`, params);

export const createMonitorTask = data =>
  request.post('/address/monitor/task', data);

export const updateMonitorTask = data =>
  request.post('/address/monitor/task/update', data);

export const rerunMonitorTask = id =>
  request.post(`/address/monitor/task/rerun/${id}`);

export const pauseMonitorTask = id =>
  request.post(`/address/monitor/task/pause/${id}`);

export const terminateMonitorTask = id =>
  request.post(`/address/monitor/task/terminate/${id}`);

export const getSearchOpsOverview = () =>
  request.get('/address/search/ops/overview');

export const createStandardRebuildTask = data =>
  request.post('/address/search/tasks/rebuild/standard', data);

export const createInstallationRebuildTask = data =>
  request.post('/address/search/tasks/rebuild/installation', data);

export const getSearchMaintenanceTasks = params =>
  request.get('/address/search/tasks', { params });

export const getSearchMaintenanceTaskDetail = taskId =>
  request.get(`/address/search/tasks/${taskId}`);

export const getSearchRepairTasks = params =>
  request.get('/address/search/repair/tasks', { params });

export const executeSearchRepairTask = (taskId, data) =>
  request.post(`/address/search/repair/${taskId}/execute`, data);
