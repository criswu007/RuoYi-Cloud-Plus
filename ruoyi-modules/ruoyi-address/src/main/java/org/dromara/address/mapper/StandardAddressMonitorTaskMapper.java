package org.dromara.address.mapper;

import org.dromara.address.domain.StandardAddressMonitorTask;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 标准地址监控任务Mapper。
 * 目的：提供监控任务主表的基础读写能力，供任务管理服务复用。
 * 入参/出参：入参为 `StandardAddressMonitorTask`，出参统一映射到 `StandardAddressAdminVo.MonitorTaskVo`。
 * 关键约束：当前阶段仅覆盖主表查询，范围与规则关联后续通过关系表补齐。
 * 异常与副作用：Mapper 本身不直接处理业务异常，也不引入额外副作用。
 */
public interface StandardAddressMonitorTaskMapper
    extends BaseMapperPlus<StandardAddressMonitorTask, StandardAddressAdminVo.MonitorTaskVo> {
}
