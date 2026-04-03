package org.dromara.address.support;

import cn.hutool.core.util.IdUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.StandardAddressOperationLog;
import org.dromara.address.mapper.StandardAddressOperationLogMapper;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 标准地址本地操作日志记录器。
 * 目的：把标准地址新增、修改、删除、合并、拆分、导入等核心写操作同步落到 `address_standard_operation_log`，
 * 避免仅依赖公共远程操作日志导致地址模块审计页无数据可查。
 * 入参/出参：输入标准地址主键、操作类型、操作对象名称与详情文案；无返回值。
 * 关键约束：仅在主业务写操作成功后调用；与主业务共用同一数据源和事务，保证地址数据与审计日志一致提交。
 * 异常与副作用：会写入 `address_standard_operation_log`；落表失败时抛出运行时异常并回滚当前业务事务。
 */
@Component
@DS("address")
@RequiredArgsConstructor
public class StandardAddressOperationLogRecorder {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String DEFAULT_OPERATOR = "系统";
    private static final String NOT_DELETED = "0";
    private static final String OPERATION_OBJECT_PREFIX = "操作对象：";
    private static final String DETAIL_SEPARATOR = "；";

    private final StandardAddressOperationLogMapper operationLogMapper;

    /**
     * 目的：记录一条标准地址本地操作日志。
     * 入参：标准地址主键、操作类型、操作对象名称、详情文案。
     * 出参：无。
     * 关键约束：`standardAddressId` 与 `operationType` 不能为空；详情会统一补齐“操作对象”前缀，便于后续审计展示。
     * 异常与副作用：会写入本地操作日志表；数据库写入异常将向上抛出。
     */
    public void record(String standardAddressId, String operationType, String operationObject, String detailMessage) {
        if (StringUtils.isBlank(standardAddressId) || StringUtils.isBlank(operationType)) {
            throw new IllegalArgumentException("标准地址操作日志缺少必要标识");
        }
        StandardAddressOperationLog log = new StandardAddressOperationLog();
        log.setId(IdUtil.getSnowflakeNextId());
        log.setStandardAddressId(standardAddressId);
        log.setOperationType(operationType);
        log.setOperator(StringUtils.defaultIfBlank(LoginHelper.getUsername(), DEFAULT_OPERATOR));
        log.setOperateTime(new Date());
        log.setTenantId(StringUtils.defaultIfBlank(LoginHelper.getTenantId(), DEFAULT_TENANT_ID));
        log.setDelFlag(NOT_DELETED);
        log.setDetails(buildDetails(operationObject, detailMessage));
        operationLogMapper.insert(log);
    }

    private String buildDetails(String operationObject, String detailMessage) {
        String normalizedDetail = StringUtils.defaultIfBlank(detailMessage, "操作成功");
        if (StringUtils.isBlank(operationObject)) {
            return normalizedDetail;
        }
        return OPERATION_OBJECT_PREFIX + operationObject + DETAIL_SEPARATOR + normalizedDetail;
    }
}
