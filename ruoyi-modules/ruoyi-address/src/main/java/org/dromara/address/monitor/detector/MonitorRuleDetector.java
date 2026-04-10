package org.dromara.address.monitor.detector;

import org.dromara.address.domain.StandardAddressMonitorRule;

/**
 * 监控规则 detector 接口。
 * 目的：按规则模板拆分具体检测逻辑，避免任务执行服务中堆积模板分支判断。
 * 入参/出参：入参为规则实体与检测上下文，出参为命中结果。
 * 关键约束：一个 detector 只负责一种规则模板。
 * 异常与副作用：检测过程不得直接写库，命中结果由执行器统一处理。
 */
public interface MonitorRuleDetector {

    /**
     * 目的：返回当前 detector 负责的规则模板编码。
     * 入参：无。
     * 出参：规则模板编码。
     * 关键约束：必须与规则表中的 `ruleTemplate` 存储值一致。
     * 异常与副作用：无写入副作用。
     */
    String getTemplateCode();

    /**
     * 目的：对单条地址执行模板检测。
     * 入参：监控规则实体与检测上下文。
     * 出参：命中结果；未命中时返回 `null`。
     * 关键约束：检测逻辑仅负责计算，不处理异常记录落库。
     * 异常与副作用：不得直接修改数据库或远程状态。
     */
    MonitorHitResult detect(StandardAddressMonitorRule rule, MonitorDetectContext context);
}
