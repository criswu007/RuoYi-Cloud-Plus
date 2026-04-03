export const FORM_OPTION_FIELDS = {
  status: 'statusOptions',
  addrInTypeFtth: 'addrInTypeFtthOptions',
  ftthPonType: 'ftthPonTypeOptions',
  addrInTypeLan: 'addrInTypeLanOptions',
  areaType: 'areaTypeOptions',
  placeType: 'placeTypeOptions'
};

export const STATION_MANAGE_TYPE_MAP = {
  stationId: '2017101',
  installStationId: '2017102',
  busStationId: '2017103'
};

export function createEmptyFormOptions() {
  return {
    statusOptions: [],
    addrInTypeFtthOptions: [],
    ftthPonTypeOptions: [],
    addrInTypeLanOptions: [],
    areaTypeOptions: [],
    placeTypeOptions: []
  };
}

export function createEmptyFormOptionLabelMaps() {
  return {
    status: {},
    addrInTypeFtth: {},
    ftthPonType: {},
    addrInTypeLan: {},
    areaType: {},
    placeType: {}
  };
}

export function createEmptyStationOptions() {
  return {
    stationId: [],
    installStationId: [],
    busStationId: []
  };
}

export function createEmptyStationOptionLoading() {
  return {
    stationId: false,
    installStationId: false,
    busStationId: false
  };
}

export function normalizeRestrictionOptions(options = []) {
  return (Array.isArray(options) ? options : [])
    .map(item => ({
      value: item?.value === null || item?.value === undefined ? '' : String(item.value),
      label: item?.label || (item?.value === null || item?.value === undefined ? '' : String(item.value))
    }))
    .filter(item => item.value !== '');
}

export function buildFormOptionState(payload = {}) {
  const formOptions = createEmptyFormOptions();
  const formOptionLabelMaps = createEmptyFormOptionLabelMaps();

  Object.entries(FORM_OPTION_FIELDS).forEach(([field, optionKey]) => {
    const normalizedOptions = normalizeRestrictionOptions(payload?.[optionKey]);
    formOptions[optionKey] = normalizedOptions;
    formOptionLabelMaps[field] = Object.fromEntries(
      normalizedOptions.map(item => [item.value, item.label])
    );
  });

  return {
    formOptions,
    formOptionLabelMaps
  };
}

export function getFormOptionLabel(formOptionLabelMaps, field, value) {
  if (value === '' || value === null || value === undefined) {
    return '-';
  }
  const optionValue = String(value);
  return formOptionLabelMaps?.[field]?.[optionValue] || optionValue;
}

export function normalizeStationOptions(options = []) {
  return (Array.isArray(options) ? options : [])
    .filter(item => item?.stationId)
    .map(item => ({
      stationId: String(item.stationId),
      stationName: item.stationName || String(item.stationId),
      regionId: item.regionId || '',
      manageType: item.manageType || ''
    }));
}

export function mergeStationOptions(existingOptions = [], incomingOptions = [], currentValue = '') {
  const optionMap = new Map();

  normalizeStationOptions(existingOptions).forEach(item => {
    optionMap.set(item.stationId, item);
  });
  normalizeStationOptions(incomingOptions).forEach(item => {
    optionMap.set(item.stationId, item);
  });

  const normalizedCurrentValue = currentValue === null || currentValue === undefined || currentValue === ''
    ? ''
    : String(currentValue);
  if (normalizedCurrentValue && !optionMap.has(normalizedCurrentValue)) {
    optionMap.set(normalizedCurrentValue, {
      stationId: normalizedCurrentValue,
      stationName: normalizedCurrentValue,
      regionId: '',
      manageType: ''
    });
  }

  return [...optionMap.values()];
}

export function getStationOptionLabel(option) {
  return option?.stationName || option?.stationId || '-';
}
