package org.dromara.address.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.StandardAddressOperationLog;
import org.dromara.address.domain.bo.StandardAddressOperationLogBo;
import org.dromara.address.domain.vo.StandardAddressOperationLogVo;
import org.dromara.address.mapper.StandardAddressOperationLogMapper;
import org.dromara.address.service.IStandardAddressOperationLogService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 地址操作日志服务实现。
 * 目的：提供操作日志的查询能力，支持审计与追溯。
 */
@RequiredArgsConstructor
@Service
@DS("address")
public class StandardAddressOperationLogServiceImpl implements IStandardAddressOperationLogService {

    private final StandardAddressOperationLogMapper baseMapper;

    @Override
    public StandardAddressOperationLogVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<StandardAddressOperationLogVo> queryPageList(StandardAddressOperationLogBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<StandardAddressOperationLog> lqw = buildQueryWrapper(bo);
        Page<StandardAddressOperationLogVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<StandardAddressOperationLogVo> queryList(StandardAddressOperationLogBo bo) {
        LambdaQueryWrapper<StandardAddressOperationLog> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<StandardAddressOperationLog> buildQueryWrapper(StandardAddressOperationLogBo bo) {
        LambdaQueryWrapper<StandardAddressOperationLog> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getStandardAddressId() != null, StandardAddressOperationLog::getStandardAddressId, bo.getStandardAddressId());
        lqw.eq(StringUtils.isNotBlank(bo.getOperationType()), StandardAddressOperationLog::getOperationType, bo.getOperationType());
        lqw.like(StringUtils.isNotBlank(bo.getOperator()), StandardAddressOperationLog::getOperator, bo.getOperator());

        Map<String, Object> params = bo.getParams();
        if (params != null && params.get("beginTime") != null && params.get("endTime") != null) {
            lqw.between(true, StandardAddressOperationLog::getOperateTime, params.get("beginTime"), params.get("endTime"));
        }
        return lqw;
    }
}
