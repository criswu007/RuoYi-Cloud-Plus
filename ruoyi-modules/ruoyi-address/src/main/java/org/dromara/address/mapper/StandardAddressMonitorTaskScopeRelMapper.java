package org.dromara.address.mapper;

import org.dromara.address.domain.StandardAddressMonitorTaskScopeRel;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 监控任务范围关系Mapper。
 * 目的：提供任务范围明细表的基础读写能力。
 * 入参/出参：入参为 `StandardAddressMonitorTaskScopeRel`，出参同样使用实体映射。
 * 关键约束：范围类型和值的合法性由服务层负责校验。
 * 异常与副作用：Mapper 本身不直接处理业务异常。
 */
public interface StandardAddressMonitorTaskScopeRelMapper
    extends BaseMapperPlus<StandardAddressMonitorTaskScopeRel, StandardAddressMonitorTaskScopeRel> {
}
