package org.dromara.address.mapper;

import org.dromara.address.domain.AddressSearchMaintenanceTask;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 地址搜索运维任务 Mapper。
 * <p>
 * 目的：提供 ES 运维任务表的基础 CRUD 能力，供任务编排服务记录重建任务、进度和结果。
 * 关键约束：仅承担任务表持久化，不在 Mapper 层承载运维流程控制。
 * 异常与副作用：执行数据库读写；事务边界由调用方控制。
 * </p>
 */
public interface AddressSearchMaintenanceTaskMapper extends BaseMapperPlus<AddressSearchMaintenanceTask, AddressSearchMaintenanceTask> {
}
