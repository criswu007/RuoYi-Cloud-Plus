package org.dromara.address.service;

import org.dromara.address.domain.vo.StandardAddressMonitorTaskSummaryVo;

/**
 * 非标地址监控任务服务接口。
 * 目的：提供监控任务摘要查询与手动触发能力，支撑原型中的监控任务页面。
 */
public interface IStandardAddressMonitorTaskService {

    /**
     * 查询监控任务摘要。
     *
     * @return 任务摘要
     *
     * 关键约束：摘要基于现有规则与异常记录实时统计。
     */
    StandardAddressMonitorTaskSummaryVo querySummary();

    /**
     * 立即执行一次非标地址监控。
     *
     * @return 本次新增的异常记录数
     *
     * 副作用：触发监控扫描并新增异常记录。
     */
    Integer executeNow();
}
