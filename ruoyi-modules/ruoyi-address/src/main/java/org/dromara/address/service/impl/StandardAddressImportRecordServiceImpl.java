package org.dromara.address.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.StandardAddressImportRecord;
import org.dromara.address.domain.bo.StandardAddressImportRecordBo;
import org.dromara.address.domain.vo.StandardAddressImportRecordVo;
import org.dromara.address.mapper.StandardAddressImportRecordMapper;
import org.dromara.address.service.IStandardAddressImportRecordService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    /**
     * {@inheritDoc}
     */
    public StandardAddressImportRecordVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public TableDataInfo<StandardAddressImportRecordVo> queryPageList(StandardAddressImportRecordBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<StandardAddressImportRecord> lqw = buildQueryWrapper(bo);
        Page<StandardAddressImportRecordVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public List<StandardAddressImportRecordVo> queryList(StandardAddressImportRecordBo bo) {
        LambdaQueryWrapper<StandardAddressImportRecord> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
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

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    /**
     * {@inheritDoc}
     */
    public void saveRecord(String fileName, String status, int successCount, int failCount, String errorMsg, String operator) {
        StandardAddressImportRecord record = new StandardAddressImportRecord();
        record.setFileName(StringUtils.blankToDefault(fileName, "unknown"));
        record.setStatus(status);
        record.setSuccessCount(successCount);
        record.setFailCount(failCount);
        record.setErrorMsg(errorMsg);
        baseMapper.insert(record);
    }
}
