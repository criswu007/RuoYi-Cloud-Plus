package org.dromara.address.mapper;

import org.dromara.address.domain.StandardAddressMonitorTaskRunLog;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 监控任务运行日志Mapper。
 * 目的：提供任务运行日志表的基础读写能力。
 * 入参/出参：入参为 `StandardAddressMonitorTaskRunLog`，出参同样使用实体映射。
 * 关键约束：当前阶段仅提供基础持久化能力，统计汇总逻辑由服务层负责。
 * 异常与副作用：Mapper 本身不直接处理业务异常。
 */
public interface StandardAddressMonitorTaskRunLogMapper
    extends BaseMapperPlus<StandardAddressMonitorTaskRunLog, StandardAddressAdminVo.MonitorTaskRunLogVo> {
}
