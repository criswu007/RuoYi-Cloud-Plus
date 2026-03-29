package org.dromara.address.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 非标地址监控任务摘要视图对象。
 * 目的：返回规则、异常记录与最近一次手动触发结果的概览数据。
 */
@Data
public class StandardAddressMonitorTaskSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则总数
     */
    private Long totalRuleCount;

    /**
     * 启用规则数
     */
    private Long enabledRuleCount;

    /**
     * 待处理异常数
     */
    private Long pendingRecordCount;

    /**
     * 已忽略异常数
     */
    private Long ignoredRecordCount;

    /**
     * 已处理异常数
     */
    private Long processedRecordCount;

    /**
     * 最近一次手动触发新增异常数
     */
    private Integer lastCreatedCount;
}
