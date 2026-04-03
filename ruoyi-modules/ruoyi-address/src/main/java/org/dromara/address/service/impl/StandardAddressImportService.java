package org.dromara.address.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.SpcRegion;
import org.dromara.address.domain.StandardAddressImportFailDetail;
import org.dromara.address.domain.StandardAddressImportRecord;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressImportResultVo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.SpcRegionMapper;
import org.dromara.address.mapper.StandardAddressImportFailDetailMapper;
import org.dromara.address.mapper.StandardAddressImportRecordMapper;
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

    private final StandardAddressCommandService commandService;
    private final StandardAddressDictionaryService dictionaryService;
    private final AddrSegmMapper addrSegmMapper;
    private final SpcRegionMapper spcRegionMapper;
    private final StandardAddressImportRecordMapper importRecordMapper;
    private final StandardAddressImportFailDetailMapper importFailDetailMapper;
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
        Map<String, String> standNameCache = new HashMap<>();
        if (rows != null) {
            for (int index = 0; index < rows.size(); index++) {
                StandardAddressImportVo row = rows.get(index);
                try {
                    processRow(row, index + 1, allowUpdate, standNameCache, fileName);
                    successCount++;
                } catch (Exception ex) {
                    failCount++;
                    batchErrorMsg = StringUtils.defaultIfBlank(batchErrorMsg, ex.getMessage());
                    importFailDetailMapper.insert(buildFailDetail(batchId, index + 1, row, ex.getMessage()));
                }
            }
        }
        StandardAddressImportRecord record = buildBatchRecord(batchId, batchNo, fileName, totalCount, successCount, failCount, batchErrorMsg, allowUpdate);
        importRecordMapper.insert(record);
        return buildResult(record);
    }

    private void processRow(StandardAddressImportVo row, int rowNum, boolean allowUpdate, Map<String, String> standNameCache, String fileName) {
        String parentStandName = normalizeRequiredText(row.getParentStandName(), "导入失败：父级地址不能为空");
        String segmName = normalizeRequiredText(row.getSegmName(), "导入失败：当级名称不能为空");
        Integer addrLevel = row.getAddrLevel();
        if (addrLevel == null) {
            throw new ServiceException("导入失败：地址级别不能为空");
        }
        if (addrLevel == 1 || addrLevel == 2) {
            throw new ServiceException("导入失败：一二级标准地址为只读基础数据");
        }
        String segmType = dictionaryService.resolveDefaultSegmTypeByAddrLevel(addrLevel);
        if (StringUtils.isBlank(segmType)) {
            throw new ServiceException("导入失败：地址级别不存在");
        }
        String parentSegmId = resolveParentSegmId(parentStandName, standNameCache);
        if (StringUtils.isBlank(parentSegmId)) {
            throw new ServiceException("导入失败：父级地址不存在");
        }
        AddrSegm existing = addrSegmMapper.selectActiveByParentAndSegmName(parentSegmId, segmName);
        StandardAddressBo bo = new StandardAddressBo();
        bo.setParentSegmId(parentSegmId);
        bo.setSegmName(segmName);
        bo.setAddrLevel(addrLevel);
        bo.setSegmType(segmType);
        bo.setStatus(StringUtils.defaultIfBlank(row.getStatus(), "2140900"));
        bo.setNotes(row.getNotes());
        boolean updated = false;
        if (existing == null) {
            commandService.addStandardAddressForImport(bo);
        } else {
            if (!allowUpdate) {
                throw new ServiceException("导入失败：标准地址已存在，请开启更新支持后重试");
            }
            bo.setSegmId(existing.getSegmId());
            commandService.updateStandardAddressForImport(bo);
            updated = true;
        }
        recordImportSuccess(bo, fileName, rowNum, updated);
        if (StringUtils.isNotBlank(bo.getStandName()) && StringUtils.isNotBlank(bo.getSegmId())) {
            standNameCache.put(bo.getStandName(), bo.getSegmId());
        }
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

    private String resolveParentSegmId(String parentStandName, Map<String, String> standNameCache) {
        String cachedSegmId = standNameCache.get(parentStandName);
        if (StringUtils.isNotBlank(cachedSegmId)) {
            return cachedSegmId;
        }
        AddrSegm addressParent = addrSegmMapper.selectActiveByStandName(parentStandName);
        if (addressParent != null) {
            return addressParent.getSegmId();
        }
        SpcRegion regionParent = spcRegionMapper.selectActiveByRegionName(parentStandName);
        return regionParent == null ? null : regionParent.getRegionId();
    }

    private StandardAddressImportRecord buildBatchRecord(long batchId, String batchNo, String fileName, int totalCount,
                                                         int successCount, int failCount, String errorMsg,
                                                         boolean updateSupport) {
        StandardAddressImportRecord record = new StandardAddressImportRecord();
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

    private StandardAddressImportFailDetail buildFailDetail(long batchId, int rowNum, StandardAddressImportVo row, String failReason) {
        StandardAddressImportFailDetail detail = new StandardAddressImportFailDetail();
        detail.setId(IdUtil.getSnowflakeNextId());
        detail.setBatchId(batchId);
        detail.setRowNum(rowNum);
        detail.setParentStandName(row == null ? null : row.getParentStandName());
        detail.setSegmName(row == null ? null : row.getSegmName());
        detail.setSegmType(row == null || row.getAddrLevel() == null ? null : dictionaryService.resolveDefaultSegmTypeByAddrLevel(row.getAddrLevel()));
        detail.setAddrLevel(row == null ? null : row.getAddrLevel());
        detail.setStatus(IMPORT_FAIL);
        detail.setFailReason(StringUtils.defaultIfBlank(failReason, "导入失败"));
        detail.setRawPayload(row == null ? "{}" : JSONUtil.toJsonStr(row));
        detail.setDelFlag("0");
        return detail;
    }

    private StandardAddressImportResultVo buildResult(StandardAddressImportRecord record) {
        StandardAddressImportResultVo result = new StandardAddressImportResultVo();
        result.setBatchId(record.getId());
        result.setBatchNo(record.getBatchNo());
        result.setFileName(record.getFileName());
        result.setStatus(record.getStatus());
        result.setTotalCount(record.getTotalCount());
        result.setSuccessCount(record.getSuccessCount());
        result.setFailCount(record.getFailCount());
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
}
