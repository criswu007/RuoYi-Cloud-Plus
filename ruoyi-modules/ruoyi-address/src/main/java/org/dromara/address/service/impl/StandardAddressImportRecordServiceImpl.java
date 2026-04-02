package org.dromara.address.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.StandardAddressImportRecord;
import org.dromara.address.domain.bo.StandardAddressImportRecordBo;
import org.dromara.address.domain.vo.StandardAddressImportBatchVo;
import org.dromara.address.domain.vo.StandardAddressImportRecordVo;
import org.dromara.address.mapper.StandardAddressImportFailDetailMapper;
import org.dromara.address.mapper.StandardAddressImportRecordMapper;
import org.dromara.address.service.IStandardAddressImportRecordService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 地址导入记录服务实现。
 * 目的：提供导入记录的查询与持久化能力。
 * 关键约束：保存导入记录使用独立事务，避免主业务回滚影响审计。
 */
@RequiredArgsConstructor
@Service
@DS("address")
public class StandardAddressImportRecordServiceImpl implements IStandardAddressImportRecordService {

    private final StandardAddressImportRecordMapper baseMapper;
    private final StandardAddressImportFailDetailMapper failDetailMapper;

    @Override
    /**
     * {@inheritDoc}
     */
    public StandardAddressImportBatchVo queryBatchById(Long batchId) {
        StandardAddressImportRecord record = baseMapper.selectById(batchId);
        if (record == null) {
            return null;
        }
        StandardAddressImportBatchVo batchVo = new StandardAddressImportBatchVo();
        batchVo.setBatchId(record.getId());
        batchVo.setBatchNo(record.getBatchNo());
        batchVo.setFileName(record.getFileName());
        batchVo.setStatus(record.getStatus());
        batchVo.setTotalCount(record.getTotalCount());
        batchVo.setSuccessCount(record.getSuccessCount());
        batchVo.setFailCount(record.getFailCount());
        batchVo.setUpdateSupport(record.getUpdateSupport());
        batchVo.setErrorMsg(record.getErrorMsg());
        batchVo.setCreateBy(record.getCreateBy());
        batchVo.setCreateTime(record.getCreateTime());
        return batchVo;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public TableDataInfo<StandardAddressImportRecordVo> queryPageList(StandardAddressImportRecordBo bo, PageQuery pageQuery) {
        Page<StandardAddressImportRecordVo> result = failDetailMapper.selectFailDetailPage(pageQuery.build(), bo);
        return TableDataInfo.build(result);
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public List<StandardAddressImportRecordVo> listFailDetailsByBatchId(Long batchId) {
        return failDetailMapper.selectFailDetailListByBatchId(batchId);
    }

    private LambdaQueryWrapper<StandardAddressImportRecord> buildQueryWrapper(StandardAddressImportRecordBo bo) {
        LambdaQueryWrapper<StandardAddressImportRecord> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getFileName()), StandardAddressImportRecord::getFileName, bo.getFileName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), StandardAddressImportRecord::getStatus, bo.getStatus());
        lqw.eq(bo.getCreateBy() != null, StandardAddressImportRecord::getCreateBy, bo.getCreateBy());

        if (bo.getParams() != null && bo.getParams().get("beginTime") != null && bo.getParams().get("endTime") != null) {
            lqw.between(true, StandardAddressImportRecord::getCreateTime, bo.getParams().get("beginTime"), bo.getParams().get("endTime"));
        }
        return lqw;
    }
}
