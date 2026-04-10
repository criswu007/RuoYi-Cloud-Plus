package org.dromara.address.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.StandardAddressImportBatch;
import org.dromara.address.domain.StandardAddressImportDetail;
import org.dromara.address.domain.bo.StandardAddressImportDetailBo;
import org.dromara.address.domain.vo.StandardAddressImportBatchVo;
import org.dromara.address.domain.vo.StandardAddressImportDetailVo;
import org.dromara.address.mapper.StandardAddressImportBatchMapper;
import org.dromara.address.mapper.StandardAddressImportDetailMapper;
import org.dromara.address.service.IStandardAddressImportBatchService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 地址导入记录服务实现。
 * 目的：提供导入记录的查询与持久化能力。
 * 关键约束：保存导入记录使用独立事务，避免主业务回滚影响审计。
 */
@RequiredArgsConstructor
@Service
@DS("address")
public class StandardAddressImportBatchServiceImpl implements IStandardAddressImportBatchService {

    private final StandardAddressImportBatchMapper baseMapper;
    private final StandardAddressImportDetailMapper failDetailMapper;

    @Override
    public StandardAddressImportBatchVo queryBatchById(Long batchId) {
        StandardAddressImportBatch record = baseMapper.selectById(batchId);
        if (record == null) {
            return null;
        }
        BatchSummary summary = buildBatchSummary(batchId, record);
        StandardAddressImportBatchVo batchVo = new StandardAddressImportBatchVo();
        batchVo.setBatchId(record.getId());
        batchVo.setBatchNo(record.getBatchNo());
        batchVo.setFileName(record.getFileName());
        batchVo.setStatus(summary.status());
        batchVo.setTotalCount(summary.totalCount());
        batchVo.setSuccessCount(summary.successCount());
        batchVo.setFailCount(summary.failCount());
        batchVo.setPendingCount(summary.pendingCount());
        batchVo.setUpdateSupport(record.getUpdateSupport());
        batchVo.setErrorMsg(record.getErrorMsg());
        batchVo.setCreateBy(record.getCreateBy());
        batchVo.setCreateTime(record.getCreateTime());
        return batchVo;
    }

    @Override
    public TableDataInfo<StandardAddressImportDetailVo> queryPageList(StandardAddressImportDetailBo bo, PageQuery pageQuery) {
        Page<StandardAddressImportDetailVo> result = failDetailMapper.selectFailDetailPage(pageQuery.build(), bo);
        result.getRecords().forEach(this::applyCompatibleStatus);
        return TableDataInfo.build(result);
    }

    @Override
    public List<StandardAddressImportDetailVo> listFailDetailsByBatchId(Long batchId) {
        List<StandardAddressImportDetailVo> records = failDetailMapper.selectFailDetailListByBatchId(batchId);
        records.forEach(this::applyCompatibleStatus);
        return records;
    }

    /**
     * 目的：刷新批次聚合统计。
     * 入参：批次ID。
     * 出参：无。
     * 关键约束：统计必须以导入行结果表聚合为准，不能依赖批次表历史快照。
     * 异常与副作用：会更新 `address_standard_import_batch` 的状态和数量字段。
     */
    @Override
    public void refreshBatchSummary(Long batchId) {
        StandardAddressImportBatch record = baseMapper.selectById(batchId);
        if (record == null) {
            return;
        }
        BatchSummary summary = buildBatchSummary(batchId, record);
        StandardAddressImportBatch update = new StandardAddressImportBatch();
        update.setId(batchId);
        update.setStatus(summary.status());
        update.setTotalCount(summary.totalCount());
        update.setSuccessCount(summary.successCount());
        update.setFailCount(summary.failCount());
        baseMapper.updateById(update);
    }

    private BatchSummary buildBatchSummary(Long batchId, StandardAddressImportBatch record) {
        Map<String, Integer> statusCountMap = summarizeStatusCount(batchId);
        if (statusCountMap.isEmpty()) {
            return new BatchSummary(
                record.getStatus(),
                valueOrZero(record.getTotalCount()),
                valueOrZero(record.getSuccessCount()),
                valueOrZero(record.getFailCount()),
                0
            );
        }
        int pendingCount = statusCountMap.getOrDefault(StandardAddressImportDetail.STATUS_WAITING_APPROVAL, 0);
        int successCount = statusCountMap.getOrDefault(StandardAddressImportDetail.STATUS_APPROVED_SUCCESS, 0);
        int failCount = statusCountMap.getOrDefault(StandardAddressImportDetail.STATUS_VALIDATE_FAILED, 0)
            + statusCountMap.getOrDefault(StandardAddressImportDetail.STATUS_REJECTED_FAILED, 0)
            + statusCountMap.getOrDefault(StandardAddressImportDetail.STATUS_EXECUTE_FAILED, 0);
        int totalCount = statusCountMap.values().stream().mapToInt(Integer::intValue).sum();
        return new BatchSummary(resolveBatchStatus(totalCount, pendingCount, failCount, record.getStatus()),
            totalCount, successCount, failCount, pendingCount);
    }

    private Map<String, Integer> summarizeStatusCount(Long batchId) {
        List<Map<String, Object>> rows = failDetailMapper.selectStatusSummaryByBatchId(batchId);
        Map<String, Integer> result = new HashMap<>(rows.size());
        for (Map<String, Object> row : rows) {
            Object status = row.get("status");
            Object count = row.get("count");
            if (status != null && count instanceof Number number) {
                result.put(String.valueOf(status), number.intValue());
            }
        }
        return result;
    }

    private void applyCompatibleStatus(StandardAddressImportDetailVo vo) {
        vo.setStatus(resolveCompatibleRowStatus(vo.getRowStatus()));
    }

    private String resolveBatchStatus(int totalCount, int pendingCount, int failCount, String fallbackStatus) {
        if (pendingCount > 0) {
            return StandardAddressImportBatch.STATUS_PENDING;
        }
        if (totalCount == 0) {
            return fallbackStatus;
        }
        return failCount > 0 ? StandardAddressImportBatch.STATUS_FAIL : StandardAddressImportBatch.STATUS_SUCCESS;
    }

    private String resolveCompatibleRowStatus(String rowStatus) {
        if (StandardAddressImportDetail.STATUS_WAITING_APPROVAL.equals(rowStatus)) {
            return StandardAddressImportBatch.STATUS_PENDING;
        }
        if (StandardAddressImportDetail.STATUS_APPROVED_SUCCESS.equals(rowStatus)) {
            return StandardAddressImportBatch.STATUS_SUCCESS;
        }
        return StandardAddressImportBatch.STATUS_FAIL;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private record BatchSummary(String status, int totalCount, int successCount, int failCount, int pendingCount) {
    }
}
