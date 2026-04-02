import request from './request';

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

export const getImportRecords = params =>
  request.postForm('/address/import-record/list', params);

export const getImportBatchDetail = batchId =>
  request.post(`/address/import-record/batch/${batchId}`);

export const exportImportFailDetails = batchId =>
  request.postDownload(`/address/import-record/failure/export/${batchId}`);

export const getOperationLogs = params =>
  request.get('/address/operation-log/list', { params });

export const getOperationLogDetail = id =>
  request.get(`/address/operation-log/${id}`);

export const getInstallationList = params =>
  request.get('/address/installation/list', { params });

export const getInstallationDetail = id =>
  request.get(`/address/installation/${id}`);

export const searchSelectionStandardAddresses = params =>
  request.postForm('/address/selection/search', params);

export const createRoomStandardAddress = data =>
  request.post('/address/selection/room', data);

export const getTagList = params =>
  request.get('/address/tag/list', { params });

export const getTagDetail = id =>
  request.get(`/address/tag/${id}`);

export const createTag = data =>
  request.post('/address/tag', data);

export const updateTag = data =>
  request.put('/address/tag', data);

export const deleteTag = ids =>
  request.delete(`/address/tag/${ids}`);

export const getStandardAddressTags = standardAddressId =>
  request.get(`/address/tag/standard-address/${standardAddressId}`);

export const bindTagsToStandardAddresses = data =>
  request.post('/address/tag/bind', data);

export const unbindTagsFromStandardAddresses = data =>
  request.post('/address/tag/unbind', data);

export const getMonitorRules = params =>
  request.get('/address/monitor/rule/list', { params });

export const createMonitorRule = data =>
  request.post('/address/monitor/rule', data);

export const updateMonitorRule = data =>
  request.put('/address/monitor/rule', data);

export const deleteMonitorRule = ids =>
  request.delete(`/address/monitor/rule/${ids}`);

export const getMonitorRecords = params =>
  request.get('/address/monitor/record/list', { params });

export const getMonitorRecordDetail = id =>
  request.get(`/address/monitor/record/${id}`);

export const updateMonitorRecord = data =>
  request.put('/address/monitor/record', data);

export const deleteMonitorRecord = ids =>
  request.delete(`/address/monitor/record/${ids}`);

export const getMonitorTaskSummary = () =>
  request.get('/address/monitor/task/summary');

export const executeMonitorTask = () =>
  request.post('/address/monitor/task/execute');
