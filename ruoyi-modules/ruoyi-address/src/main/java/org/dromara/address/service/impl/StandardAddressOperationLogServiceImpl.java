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
import org.dromara.address.service.IStandardAddressService;
import org.dromara.address.service.IStandardAddressOperationLogService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final IStandardAddressService standardAddressService;

    @Override
    public StandardAddressOperationLogVo queryById(Long id) {
        StandardAddressOperationLogVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            return null;
        }
        fillDerivedFields(Collections.singletonList(vo));
        return vo;
    }

    @Override
    public TableDataInfo<StandardAddressOperationLogVo> queryPageList(StandardAddressOperationLogBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<StandardAddressOperationLog> lqw = buildQueryWrapper(bo);
        Page<StandardAddressOperationLogVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        fillDerivedFields(result.getRecords());
        return TableDataInfo.build(result);
    }

    @Override
    public List<StandardAddressOperationLogVo> queryList(StandardAddressOperationLogBo bo) {
        LambdaQueryWrapper<StandardAddressOperationLog> lqw = buildQueryWrapper(bo);
        List<StandardAddressOperationLogVo> list = baseMapper.selectVoList(lqw);
        fillDerivedFields(list);
        return list;
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

    /**
     * 批量补齐日志展示衍生字段。
     *
     * @param logs 日志列表
     *
     * 目的：补齐操作对象与操作结果，避免日志列表逐条回查标准地址导致 N+1。
     * 关键约束：主查询不额外 join 大表，统一由服务层批量回填。
     * 异常与副作用：无写入副作用。
     */
    private void fillDerivedFields(Collection<StandardAddressOperationLogVo> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        List<String> segmIds = logs.stream()
            .map(StandardAddressOperationLogVo::getStandardAddressId)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        Map<String, String> standNameMap = segmIds.isEmpty()
            ? Collections.emptyMap()
            : standardAddressService.listStandardAddressStandNameMapBySegmIds(segmIds);
        for (StandardAddressOperationLogVo log : logs) {
            String segmId = log.getStandardAddressId();
            String operationObject = StringUtils.isBlank(segmId) ? null : standNameMap.get(segmId);
            log.setOperationObject(StringUtils.defaultIfBlank(operationObject, segmId));
            log.setOperationResult(resolveOperationResult(log.getDetails()));
        }
    }

    /**
     * 推断日志操作结果。
     *
     * @param details 操作详情文本
     * @return 操作结果文案
     *
     * 目的：兼容历史日志只记录详情文本、未单列结果字段的场景。
     * 关键约束：包含“失败/fail/error/异常”关键词时判定为失败，其余默认成功。
     * 异常与副作用：无写入副作用。
     */
    private String resolveOperationResult(String details) {
        if (StringUtils.isBlank(details)) {
            return "成功";
        }
        String lowered = StringUtils.lowerCase(details);
        if (StringUtils.containsAny(lowered, "失败", "fail", "error", "异常")) {
            return "失败";
        }
        return "成功";
    }
}
