package org.dromara.address.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.SpcRegion;
import org.dromara.address.domain.StandardAddressImportBatch;
import org.dromara.address.domain.StandardAddressImportDetail;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressImportResultVo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.SpcRegionMapper;
import org.dromara.address.mapper.StandardAddressImportBatchMapper;
import org.dromara.address.mapper.StandardAddressImportDetailMapper;
import org.dromara.address.support.StandardAddressOperationLogRecorder;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 标准地址导入服务。
 * 目的：负责标准地址 Excel 导入的批次统计、逐条校验、失败明细记录和成功数据写入。
 * 入参/出参：输入 Excel 解析后的导入行集合、是否允许更新、操作人与文件名，输出导入批次摘要。
 * 关键约束：本轮不提供成功数据回滚能力；失败数据必须按当前批次逐条记录并可导出；一二级地址禁止导入新增或更新。
 * 异常与副作用：会写入导入批次主记录、失败明细，并对成功数据执行新增或修改。
 */
@Service
@RequiredArgsConstructor
public class StandardAddressImportService {

    private static final String IMPORT_SUCCESS = "1";
    private static final String IMPORT_FAIL = "2";
    private static final String OPERATION_TYPE_IMPORT = "IMPORT";
    private static final String DEFAULT_STATUS = "2140900";
    private static final String MANAGE_TYPE_MAINTENANCE = "2017101";
    private static final String MANAGE_TYPE_INSTALL = "2017102";
    private static final String MANAGE_TYPE_BUSINESS = "2017103";
    private static final String KEYWORD_ADDR_IN_TYPE_FTTH = "ADDR_IN_TYPE_FTTH";
    private static final String KEYWORD_FTTH_PON_TYPE = "FTTH_PON_TYPE";
    private static final String KEYWORD_AREA_TYPE = "AREA_TYPE";
    private static final String KEYWORD_ADDR_PLACE_TYPE = "ADDR_PLACE_TYPE";

    private final StandardAddressCommandService commandService;
    private final StandardAddressDictionaryService dictionaryService;
    private final AddrSegmMapper addrSegmMapper;
    private final SpcRegionMapper spcRegionMapper;
    private final StandardAddressImportBatchMapper importBatchMapper;
    private final StandardAddressImportDetailMapper importDetailMapper;
    private final StandardAddressOperationLogRecorder operationLogRecorder;

    /**
     * 目的：执行标准地址导入。
     * 入参：导入行集合、是否允许更新、操作人与文件名。
     * 出参：导入批次摘要。
     * 关键约束：逐条校验并记录失败原因；允许部分成功但不允许回滚成功数据；父级地址解析优先匹配本批次已成功生成的标准地址。
     * 异常与副作用：会新增或修改 `ADDR_SEGM`，并写入导入批次和失败明细。
     */
    @Transactional(rollbackFor = Exception.class)
    public StandardAddressImportResultVo importStandardAddressData(List<StandardAddressImportVo> rows,
                                                                  Boolean updateSupport,
                                                                  String operName,
                                                                  String fileName) {
        boolean allowUpdate = Boolean.TRUE.equals(updateSupport);
        long batchId = IdUtil.getSnowflakeNextId();
        String batchNo = buildBatchNo(batchId);
        int totalCount = rows == null ? 0 : rows.size();
        int successCount = 0;
        int failCount = 0;
        String batchErrorMsg = null;
        Map<String, ParentAddressContext> standNameCache = new HashMap<>();
        if (rows != null) {
            for (int index = 0; index < rows.size(); index++) {
                StandardAddressImportVo row = rows.get(index);
                try {
                    processRow(row, index + 1, allowUpdate, standNameCache, fileName);
                    successCount++;
                } catch (Exception ex) {
                    failCount++;
                    batchErrorMsg = StringUtils.defaultIfBlank(batchErrorMsg, ex.getMessage());
                    importDetailMapper.insert(buildFailDetail(batchId, index + 1, row, fileName, allowUpdate, ex.getMessage()));
                }
            }
        }
        StandardAddressImportBatch record = buildBatchRecord(batchId, batchNo, fileName, totalCount, successCount, failCount, batchErrorMsg, allowUpdate);
        importBatchMapper.insert(record);
        return buildResult(record);
    }

    /**
     * 目的：执行上传阶段单条前置校验。
     * 入参：导入行与是否允许更新标记。
     * 出参：无。
     * 关键约束：只做规则校验，不写正式地址、批次记录或失败明细。
     * 异常与副作用：校验失败时抛出业务异常，无额外写入副作用。
     */
    public void validateImportRow(StandardAddressImportVo row, Boolean updateSupport) {
        prepareRow(row, Boolean.TRUE.equals(updateSupport), new HashMap<>());
    }

    /**
     * 目的：执行审批通过后的单条正式导入。
     * 入参：导入行、Excel 行号、是否允许更新、操作人与文件名。
     * 出参：无。
     * 关键约束：正式执行时仍需复用同一套校验规则，避免审批后数据已变化导致脏写。
     * 异常与副作用：会新增或修改 `ADDR_SEGM` 并记录导入操作日志。
     */
    public void executeApprovedImportRow(StandardAddressImportVo row, Integer rowNum, Boolean updateSupport, String operName, String fileName) {
        ImportExecutionContext context = prepareRow(row, Boolean.TRUE.equals(updateSupport), new HashMap<>());
        writePreparedRow(context, rowNum == null ? 0 : rowNum, fileName);
    }

    /**
     * 目的：构建导入批次主记录。
     * 入参：总数量、是否允许更新和文件名。
     * 出参：待持久化的批次记录实体。
     * 关键约束：上传阶段批次默认状态为进行中，最终状态由行结果聚合刷新。
     * 异常与副作用：仅构建内存对象，不直接写库。
     */
    public StandardAddressImportBatch buildImportBatchRecord(int totalCount, Boolean updateSupport, String fileName) {
        long batchId = IdUtil.getSnowflakeNextId();
        StandardAddressImportBatch record = new StandardAddressImportBatch();
        record.setId(batchId);
        record.setBatchNo(buildBatchNo(batchId));
        record.setFileName(StringUtils.defaultIfBlank(fileName, "standard-address-import.xlsx"));
        record.setStatus(StandardAddressImportBatch.STATUS_PENDING);
        record.setTotalCount(totalCount);
        record.setSuccessCount(0);
        record.setFailCount(0);
        record.setUpdateSupport(Boolean.TRUE.equals(updateSupport));
        record.setDelFlag("0");
        return record;
    }

    /**
     * 目的：构建单条导入行结果实体。
     * 入参：批次ID、行号、原始导入行、文件名、是否允许更新、行状态与失败原因。
     * 出参：待持久化的导入行结果实体。
     * 关键约束：`rawPayload` 必须保留原始导入快照，便于失败导出与审计回溯。
     * 异常与副作用：仅构建内存对象，不直接写库。
     */
    public StandardAddressImportDetail buildImportRowDetail(Long batchId, Integer rowNum, StandardAddressImportVo row,
                                                            String fileName, Boolean updateSupport, String status,
                                                            String failReason) {
        StandardAddressImportDetail detail = new StandardAddressImportDetail();
        detail.setId(IdUtil.getSnowflakeNextId());
        detail.setBatchId(batchId);
        detail.setRowNum(rowNum);
        detail.setFileName(StringUtils.defaultIfBlank(fileName, "standard-address-import.xlsx"));
        detail.setUpdateSupport(Boolean.TRUE.equals(updateSupport));
        detail.setParentStandName(row == null ? null : row.getParentStandName());
        detail.setSegmName(row == null ? null : row.getSegmName());
        String segmType = row == null ? null : dictionaryService.resolveSegmTypeByName(row.getSegmTypeName());
        detail.setSegmType(segmType);
        detail.setAddrLevel(segmType == null ? null : dictionaryService.resolveAddrLevel(segmType));
        detail.setStatus(status);
        detail.setFailReason(StringUtils.trimToNull(failReason));
        detail.setRawPayload(row == null ? "{}" : JSONUtil.toJsonStr(row));
        detail.setDelFlag("0");
        return detail;
    }

    private void processRow(StandardAddressImportVo row, int rowNum, boolean allowUpdate, Map<String, ParentAddressContext> standNameCache, String fileName) {
        ImportExecutionContext context = prepareRow(row, allowUpdate, standNameCache);
        writePreparedRow(context, rowNum, fileName);
    }

    private ImportExecutionContext prepareRow(StandardAddressImportVo row, boolean allowUpdate, Map<String, ParentAddressContext> standNameCache) {
        String parentStandName = normalizeRequiredText(row.getParentStandName(), "导入失败：父级地址不能为空");
        String segmName = normalizeRequiredText(row.getSegmName(), "导入失败：当级名称不能为空");
        String segmTypeName = normalizeRequiredText(row.getSegmTypeName(), "导入失败：分段地址类型不能为空");
        String segmType = dictionaryService.resolveSegmTypeByName(segmTypeName);
        if (StringUtils.isBlank(segmType)) {
            throw new ServiceException("导入失败：标准地址类型不存在");
        }
        Integer addrLevel = dictionaryService.resolveAddrLevel(segmType);
        if (addrLevel == null) {
            throw new ServiceException("导入失败：标准地址类型不存在");
        }
        if (addrLevel == 1 || addrLevel == 2) {
            throw new ServiceException("导入失败：一二级标准地址为只读基础数据");
        }
        ParentAddressContext parent = resolveParent(parentStandName, standNameCache);
        if (parent == null || StringUtils.isBlank(parent.getSegmId())) {
            throw new ServiceException("导入失败：父级地址不存在");
        }
        AddrSegm existing = addrSegmMapper.selectActiveByParentAndSegmName(parent.getSegmId(), segmName);
        StandardAddressBo bo = new StandardAddressBo();
        bo.setParentSegmId(parent.getSegmId());
        bo.setSegmName(segmName);
        bo.setAddrLevel(addrLevel);
        bo.setSegmType(segmType);
        bo.setStatus(DEFAULT_STATUS);
        bo.setIsCity(normalizeYesNoFlag(row.getIsCityLabel()));
        bo.setStationId(resolveStationId(parent.getRegionId(), MANAGE_TYPE_MAINTENANCE, row.getMaintenanceStationName()));
        bo.setInstallStationId(resolveStationId(parent.getRegionId(), MANAGE_TYPE_INSTALL, row.getInstallStationName()));
        bo.setBusStationId(resolveStationId(parent.getRegionId(), MANAGE_TYPE_BUSINESS, row.getBusinessStationName()));
        applyAccessMode(bo, row.getAccessModeName());
        bo.setFtthPonType(parseRestrictionInteger(KEYWORD_FTTH_PON_TYPE, row.getAccessCapabilityName(), "接入能力"));
        bo.setAreaType(parseRestrictionInteger(KEYWORD_AREA_TYPE, row.getAreaTypeName(), "城乡属性"));
        bo.setPlaceType(parseRestrictionInteger(KEYWORD_ADDR_PLACE_TYPE, row.getPlaceTypeName(), "房屋属性"));
        bo.setSupportingFeeCommunityFlag(normalizeYesNoFlag(row.getSupportingFeeCommunityLabel()));
        bo.setCoverNum(parseCoverNum(row.getCoverNumText()));
        bo.setSingleProjectCode(trimToNull(row.getSingleProjectCode()));
        if (existing != null && !allowUpdate) {
            throw new ServiceException("导入失败：标准地址已存在，请开启更新支持后重试");
        }
        return new ImportExecutionContext(bo, existing, parent);
    }

    private void writePreparedRow(ImportExecutionContext context, int rowNum, String fileName) {
        boolean updated = false;
        if (context.existing == null) {
            commandService.addStandardAddressForImport(context.bo);
        } else {
            context.bo.setSegmId(context.existing.getSegmId());
            commandService.updateStandardAddressForImport(context.bo);
            updated = true;
        }
        recordImportSuccess(context.bo, fileName, rowNum, updated);
    }

    private void recordImportSuccess(StandardAddressBo bo, String fileName, int rowNum, boolean updated) {
        if (StringUtils.isBlank(bo.getSegmId())) {
            return;
        }
        String operationObject = StringUtils.defaultIfBlank(bo.getStandName(), bo.getSegmName());
        String detail = (updated ? "导入更新标准地址成功" : "导入新增标准地址成功")
            + "；文件：" + StringUtils.defaultIfBlank(fileName, "standard-address-import.xlsx")
            + "；行号：" + rowNum;
        operationLogRecorder.record(bo.getSegmId(), OPERATION_TYPE_IMPORT, operationObject, detail);
    }

    private ParentAddressContext resolveParent(String parentStandName, Map<String, ParentAddressContext> standNameCache) {
        ParentAddressContext cached = standNameCache.get(parentStandName);
        if (cached != null && StringUtils.isNotBlank(cached.getSegmId())) {
            return cached;
        }
        AddrSegm addressParent = addrSegmMapper.selectActiveByStandName(parentStandName);
        if (addressParent != null) {
            return new ParentAddressContext(addressParent.getSegmId(), addressParent.getRegionId());
        }
        SpcRegion regionParent = spcRegionMapper.selectActiveByRegionName(parentStandName);
        return regionParent == null ? null : new ParentAddressContext(regionParent.getRegionId(), regionParent.getRegionId());
    }

    private StandardAddressImportBatch buildBatchRecord(long batchId, String batchNo, String fileName, int totalCount,
                                                        int successCount, int failCount, String errorMsg,
                                                        boolean updateSupport) {
        StandardAddressImportBatch record = new StandardAddressImportBatch();
        record.setId(batchId);
        record.setBatchNo(batchNo);
        record.setFileName(StringUtils.defaultIfBlank(fileName, "standard-address-import.xlsx"));
        record.setStatus(failCount > 0 ? IMPORT_FAIL : IMPORT_SUCCESS);
        record.setTotalCount(totalCount);
        record.setSuccessCount(successCount);
        record.setFailCount(failCount);
        record.setErrorMsg(errorMsg);
        record.setUpdateSupport(updateSupport);
        record.setDelFlag("0");
        return record;
    }

    private StandardAddressImportDetail buildFailDetail(long batchId, int rowNum, StandardAddressImportVo row,
                                                        String fileName, boolean updateSupport, String failReason) {
        StandardAddressImportDetail detail = new StandardAddressImportDetail();
        detail.setId(IdUtil.getSnowflakeNextId());
        detail.setBatchId(batchId);
        detail.setRowNum(rowNum);
        detail.setFileName(StringUtils.defaultIfBlank(fileName, "standard-address-import.xlsx"));
        detail.setUpdateSupport(updateSupport);
        detail.setParentStandName(row == null ? null : row.getParentStandName());
        detail.setSegmName(row == null ? null : row.getSegmName());
        String segmType = row == null ? null : dictionaryService.resolveSegmTypeByName(row.getSegmTypeName());
        detail.setSegmType(segmType);
        detail.setAddrLevel(segmType == null ? null : dictionaryService.resolveAddrLevel(segmType));
        detail.setStatus(StandardAddressImportDetail.STATUS_VALIDATE_FAILED);
        detail.setFailReason(StringUtils.defaultIfBlank(failReason, "导入失败"));
        detail.setRawPayload(row == null ? "{}" : JSONUtil.toJsonStr(row));
        detail.setDelFlag("0");
        return detail;
    }

    private StandardAddressImportResultVo buildResult(StandardAddressImportBatch record) {
        StandardAddressImportResultVo result = new StandardAddressImportResultVo();
        result.setBatchId(record.getId());
        result.setBatchNo(record.getBatchNo());
        result.setFileName(record.getFileName());
        result.setStatus(record.getStatus());
        result.setTotalCount(record.getTotalCount());
        result.setSuccessCount(record.getSuccessCount());
        result.setFailCount(record.getFailCount());
        result.setPendingCount(0);
        result.setFailureExportable(record.getFailCount() != null && record.getFailCount() > 0);
        return result;
    }

    private String buildBatchNo(long batchId) {
        return "STDADDRIMP" + batchId;
    }

    private String normalizeRequiredText(String value, String message) {
        String normalized = value == null ? null : value.trim();
        if (StringUtils.isBlank(normalized)) {
            throw new ServiceException(message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeYesNoFlag(String label) {
        String normalized = trimToNull(label);
        if (normalized == null) {
            return null;
        }
        if ("是".equals(normalized)) {
            return "Y";
        }
        if ("否".equals(normalized)) {
            return "N";
        }
        throw new ServiceException("导入失败：" + normalized + " 不是有效的是/否值");
    }

    private void applyAccessMode(StandardAddressBo bo, String label) {
        String normalized = trimToNull(label);
        if (normalized == null) {
            return;
        }
        String ftthValue = dictionaryService.resolveRestrictionValue(KEYWORD_ADDR_IN_TYPE_FTTH, normalized);
        if (StringUtils.isNotBlank(ftthValue)) {
            bo.setAddrInTypeFtth(Integer.valueOf(ftthValue));
            bo.setAddrInTypeLan(null);
            return;
        }
        throw new ServiceException("导入失败：接入方式不存在");
    }

    private Integer parseRestrictionInteger(String keyword, String label, String fieldName) {
        String normalized = trimToNull(label);
        if (normalized == null) {
            return null;
        }
        String value = dictionaryService.resolveRestrictionValue(keyword, normalized);
        if (StringUtils.isBlank(value)) {
            throw new ServiceException("导入失败：" + fieldName + "不存在");
        }
        return Integer.valueOf(value);
    }

    private String resolveStationId(String regionId, String manageType, String stationName) {
        String normalized = trimToNull(stationName);
        if (normalized == null) {
            return null;
        }
        return dictionaryService.matchStationId(regionId, manageType, normalized);
    }

    private Integer parseCoverNum(String coverNumText) {
        String normalized = trimToNull(coverNumText);
        if (normalized == null) {
            return 1;
        }
        if (!normalized.matches("\\d+")) {
            return 1;
        }
        try {
            return Math.max(Integer.parseInt(normalized), 1);
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private static final class ParentAddressContext {
        private final String segmId;
        private final String regionId;

        private ParentAddressContext(String segmId, String regionId) {
            this.segmId = segmId;
            this.regionId = regionId;
        }

        private String getSegmId() {
            return segmId;
        }

        private String getRegionId() {
            return regionId;
        }
    }

    private static final class ImportExecutionContext {
        private final StandardAddressBo bo;
        private final AddrSegm existing;
        private final ParentAddressContext parent;

        private ImportExecutionContext(StandardAddressBo bo, AddrSegm existing, ParentAddressContext parent) {
            this.bo = bo;
            this.existing = existing;
            this.parent = parent;
        }
    }
}
