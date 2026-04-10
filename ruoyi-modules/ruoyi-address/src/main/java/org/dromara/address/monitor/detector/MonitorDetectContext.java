package org.dromara.address.monitor.detector;

import lombok.Data;

/**
 * 监控检测上下文。
 * 目的：承载单条地址检测时的任务、地址和规则执行上下文，供 detector 统一读取。
 * 入参/出参：作为 detector 的输入对象在执行链路中传递。
 * 关键约束：当前阶段先保留任务与地址核心标识，后续可继续扩展属性与标签快照。
 * 异常与副作用：数据载体对象本身无异常与副作用。
 */
@Data
public class MonitorDetectContext {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 标准地址ID
     */
    private Long standardAddressId;

    /**
     * 标准地址名称
     */
    private String standName;

    /**
     * 当级名称
     */
    private String segmName;

    /**
     * 区域ID
     */
    private String regionId;
}
