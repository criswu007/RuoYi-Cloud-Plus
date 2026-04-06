package org.dromara.address.mapper;

import org.dromara.address.domain.AddressSearchRepairTask;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 地址搜索修复任务 Mapper。
 * <p>
 * 目的：提供 repair 任务的基础持久化能力，供同步服务入队、维护服务领取与回写状态。
 * 关键约束：仅承担 repair 表 CRUD，不在 Mapper 层执行修复动作。
 * 异常与副作用：执行数据库读写；由调用方决定事务边界。
 * </p>
 */
public interface AddressSearchRepairTaskMapper extends BaseMapperPlus<AddressSearchRepairTask, AddressSearchRepairTask> {
}
