package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.vo.StandardAddressMonitorTaskSummaryVo;
import org.dromara.address.service.IStandardAddressMonitorTaskService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 非标地址监控任务对外接口。
 * 目的：提供监控任务摘要查询与手动触发入口，衔接原型中的监控任务页面。
 * 关键约束：当前版本仅提供摘要与立即执行，不维护持久化任务编排。
 * 副作用：手动触发会执行一次完整监控扫描并新增异常记录。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/address/monitor/task")
public class StandardAddressMonitorTaskController extends BaseController {

    private final IStandardAddressMonitorTaskService addressMonitorTaskService;

    /**
     * 查询监控任务摘要。
     *
     * @return 摘要信息
     */
    @SaCheckPermission("address:monitor:task:query")
    @GetMapping("/summary")
    public R<StandardAddressMonitorTaskSummaryVo> summary() {
        return R.ok(addressMonitorTaskService.querySummary());
    }

    /**
     * 立即执行一次非标地址监控。
     *
     * @return 本次新增异常记录数
     *
     * 副作用：执行监控扫描并新增异常记录。
     */
    @SaCheckPermission("address:monitor:task:execute")
    @Log(title = "监控任务触发", businessType = BusinessType.UPDATE)
    @PostMapping("/execute")
    public R<Integer> execute() {
        return R.ok(addressMonitorTaskService.executeNow());
    }
}
