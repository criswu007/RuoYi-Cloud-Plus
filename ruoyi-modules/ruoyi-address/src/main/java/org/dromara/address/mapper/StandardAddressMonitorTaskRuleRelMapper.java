package org.dromara.address.mapper;

import org.dromara.address.domain.StandardAddressMonitorTaskRuleRel;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 监控任务规则关系Mapper。
 * 目的：提供任务与规则关系表的基础读写能力。
 * 入参/出参：入参为 `StandardAddressMonitorTaskRuleRel`，出参同样使用实体映射。
 * 关键约束：当前阶段仅承载简单关系读写，不下沉业务校验。
 * 异常与副作用：Mapper 本身不直接处理业务异常。
 */
public interface StandardAddressMonitorTaskRuleRelMapper
    extends BaseMapperPlus<StandardAddressMonitorTaskRuleRel, StandardAddressMonitorTaskRuleRel> {
}
