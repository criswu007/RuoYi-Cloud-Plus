function toNumber(value) {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
}

export function normalizeLevelOptions(options = []) {
  return [...options]
    .filter(item => item && item.addrTypeId !== null && item.addrTypeId !== undefined)
    .map(item => ({
      addrTypeId: String(item.addrTypeId),
      name: item.name || '',
      addrLevel: toNumber(item.addrLevel),
      levelId: toNumber(item.levelId)
    }))
    .sort((left, right) => {
      const levelCompare = (left.addrLevel ?? Number.MAX_SAFE_INTEGER) - (right.addrLevel ?? Number.MAX_SAFE_INTEGER);
      if (levelCompare !== 0) {
        return levelCompare;
      }
      const dbLevelCompare = (left.levelId ?? Number.MAX_SAFE_INTEGER) - (right.levelId ?? Number.MAX_SAFE_INTEGER);
      if (dbLevelCompare !== 0) {
        return dbLevelCompare;
      }
      return left.addrTypeId.localeCompare(right.addrTypeId);
    });
}

export function buildLevelMaps(levelOptions = []) {
  const levelNameMap = {};
  const typeNameMap = {};

  levelOptions.forEach(item => {
    if (item.addrLevel !== null && item.addrLevel !== undefined && item.name && !levelNameMap[item.addrLevel]) {
      levelNameMap[item.addrLevel] = item.name;
    }
    if (item.addrTypeId && item.name) {
      typeNameMap[item.addrTypeId] = item.name;
    }
  });

  return { levelNameMap, typeNameMap };
}

export function resolveLevelLabel(levelMaps = {}, addrLevel, segmType) {
  const { levelNameMap = {}, typeNameMap = {} } = levelMaps;
  const segmTypeKey = segmType === null || segmType === undefined || segmType === '' ? '' : String(segmType);
  if (segmTypeKey && typeNameMap[segmTypeKey]) {
    return typeNameMap[segmTypeKey];
  }

  const normalizedAddrLevel = toNumber(addrLevel);
  if (normalizedAddrLevel !== null && levelNameMap[normalizedAddrLevel]) {
    return levelNameMap[normalizedAddrLevel];
  }

  return normalizedAddrLevel !== null ? `第 ${normalizedAddrLevel} 级` : '-';
}
