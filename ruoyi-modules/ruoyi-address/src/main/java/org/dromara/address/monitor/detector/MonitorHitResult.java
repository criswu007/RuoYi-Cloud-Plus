package org.dromara.address.monitor.detector;

import lombok.Data;

/**
 * 监控规则命中结果。
 * 目的：统一表达 detector 对单条地址的检测结果，供执行器落库和统计汇总。
 * 入参/出参：作为 detector 输出对象在执行链路中传递。
 * 关键约束：未命中时由调用方直接使用 `null` 或空对象表达，不额外写库。
 * 异常与副作用：数据载体对象本身无异常与副作用。
 */
@Data
public class MonitorHitResult {

    /**
     * 是否命中
     */
    private boolean hit;

    /**
     * 严重等级
     */
    private String severity;

    /**
     * 命中详情JSON
     */
    private String detailJson;

    /**
     * 去重键
     */
    private String dedupKey;
}
