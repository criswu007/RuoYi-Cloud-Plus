package org.dromara.address.mapper;

import org.dromara.address.domain.AddressSearchSyncLog;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 地址搜索同步日志 Mapper。
 * <p>
 * 目的：提供地址 ES 双写同步日志的基础持久化能力。
 * 关键约束：仅承担日志表 CRUD，不承载业务补偿编排逻辑。
 * 异常与副作用：执行数据库读写；由调用方决定事务边界。
 * </p>
 */
public interface AddressSearchSyncLogMapper extends BaseMapperPlus<AddressSearchSyncLog, AddressSearchSyncLog> {
}
