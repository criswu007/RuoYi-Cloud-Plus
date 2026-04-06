package org.dromara.address.search.support;

/**
 * 地址搜索运维进度监听器。
 * <p>
 * 目的：承接全量重建过程中总量解析、物理索引创建、批量写入和别名切换等阶段回调，供任务编排服务回写进度。
 * 关键约束：监听器只消费进度事件，不参与具体 ES/数据库读写控制。
 * 异常与副作用：监听实现可能触发任务表更新；默认实现无副作用。
 * </p>
 */
public interface AddressSearchMaintenanceProgressListener {

    AddressSearchMaintenanceProgressListener NO_OP = new AddressSearchMaintenanceProgressListener() {
    };

    /**
     * 目的：通知调用方当前重建总量已解析完成。
     * 入参：总记录数。
     * 出参：无。
     * 关键约束：总量应为当前重建范围内的最终统计结果。
     * 异常与副作用：实现方可据此回写任务总量。
     *
     * @param totalCount 总记录数
     */
    default void onTotalResolved(long totalCount) {
    }

    /**
     * 目的：通知调用方新物理索引已准备完成。
     * 入参：新物理索引名。
     * 出参：无。
     * 关键约束：仅在索引真正可写后回调。
     * 异常与副作用：实现方可据此回写当前物理索引名。
     *
     * @param physicalIndexName 物理索引名
     */
    default void onPhysicalIndexPrepared(String physicalIndexName) {
    }

    /**
     * 目的：通知调用方当前累计写入量。
     * 入参：累计已处理数量。
     * 出参：无。
     * 关键约束：应传入累计值，而不是单批增量。
     * 异常与副作用：实现方可据此回写进度百分比。
     *
     * @param processedCount 累计已处理数量
     */
    default void onBatchCompleted(long processedCount) {
    }

    /**
     * 目的：通知调用方别名切换已完成。
     * 入参：无。
     * 出参：无。
     * 关键约束：仅在别名切换真正成功后触发。
     * 异常与副作用：实现方可据此把任务推进到最终阶段。
     */
    default void onAliasSwitched() {
    }
}
