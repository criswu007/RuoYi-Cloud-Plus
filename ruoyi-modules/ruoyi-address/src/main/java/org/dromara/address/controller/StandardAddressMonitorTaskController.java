package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.StandardAddressAdminBo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.domain.vo.StandardAddressMonitorTaskSummaryVo;
import org.dromara.address.service.IStandardAddressMonitorTaskService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotNull;

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
    @PostMapping("/summary")
    public R<StandardAddressMonitorTaskSummaryVo> summaryMonitorTasks() {
        return R.ok(addressMonitorTaskService.querySummary());
    }

    /**
     * 立即执行一次非标地址监控。
     *
     * @return 提交结果，`1` 表示已提交后台执行，`0` 表示已有巡检执行中
     *
     * 目的：为任务管理页提供全局手动巡检入口，并避免请求线程等待整轮扫描完成。
     * 入参/出参：无显式入参，出参为提交通知状态。
     * 关键约束：接口只负责提交，不同步等待真实巡检完成。
     * 异常与副作用：成功后会异步触发监控扫描并新增异常记录。
     */
    @SaCheckPermission("address:monitor:task:execute")
    @Log(title = "监控任务触发", businessType = BusinessType.UPDATE)
    @PostMapping("/execute")
    public R<Integer> executeMonitorTask() {
        return R.ok(addressMonitorTaskService.executeNow());
    }

    /**
     * 分页查询监控任务。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     *
     * 目的：返回监控任务列表，支撑任务管理页与运维视图。
     * 入参/出参：入参为任务筛选条件和分页参数，出参为任务分页视图集合。
     * 关键约束：当前阶段主表字段先行返回，规则与范围明细后续补齐。
     * 异常与副作用：无写入副作用。
     */
    @SaCheckPermission("address:monitor:task:query")
    @PostMapping("/list")
    public TableDataInfo<StandardAddressAdminVo.MonitorTaskVo> listMonitorTasks(StandardAddressAdminBo.MonitorTaskBo bo, PageQuery pageQuery) {
        return addressMonitorTaskService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询监控任务详情。
     *
     * @param id 任务主键
     * @return 任务详情
     *
     * 目的：返回单个监控任务详情，供编辑页和详情抽屉复用。
     * 入参/出参：入参为任务主键，出参为任务详情视图。
     * 关键约束：主键不能为空。
     * 异常与副作用：无写入副作用。
     */
    @SaCheckPermission("address:monitor:task:query")
    @PostMapping("/{id}")
    public R<StandardAddressAdminVo.MonitorTaskVo> getMonitorTaskInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(addressMonitorTaskService.queryById(id));
    }

    /**
     * 查询监控任务运行日志。
     *
     * @param taskId 任务主键
     * @param pageQuery 分页参数
     * @return 运行日志分页结果
     *
     * 目的：返回指定监控任务的运行日志列表，供任务页详情抽屉展示执行历史。
     * 入参/出参：入参为任务主键和分页参数，出参为运行日志分页视图。
     * 关键约束：主键不能为空，返回结果按开始时间倒序。
     * 异常与副作用：无写入副作用。
     */
    @SaCheckPermission("address:monitor:task:query")
    @PostMapping("/runs/{taskId}")
    public TableDataInfo<StandardAddressAdminVo.MonitorTaskRunLogVo> listMonitorTaskRunLogs(
        @NotNull(message = "主键不能为空") @PathVariable Long taskId,
        PageQuery pageQuery) {
        return addressMonitorTaskService.queryRunLogPageList(taskId, pageQuery);
    }

    /**
     * 新增监控任务。
     *
     * @param bo 任务业务对象
     * @return 操作结果
     *
     * 目的：创建新的监控任务定义。
     * 入参/出参：入参为任务业务对象，出参为统一操作结果。
     * 关键约束：执行方式、范围和规则关联需在服务层校验。
     * 异常与副作用：成功后会新增任务主表数据。
     */
    @SaCheckPermission("address:monitor:task:add")
    @Log(title = "监控任务", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> addMonitorTask(@Validated @RequestBody StandardAddressAdminBo.MonitorTaskBo bo) {
        return toAjax(addressMonitorTaskService.insertByBo(bo));
    }

    /**
     * 修改监控任务。
     *
     * @param bo 任务业务对象
     * @return 操作结果
     *
     * 目的：更新已有监控任务定义。
     * 入参/出参：入参为任务业务对象，出参为统一操作结果。
     * 关键约束：修改后需保持任务主键不变。
     * 异常与副作用：成功后会更新任务主表数据。
     */
    @SaCheckPermission("address:monitor:task:edit")
    @Log(title = "监控任务", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    public R<Void> editMonitorTask(@Validated @RequestBody StandardAddressAdminBo.MonitorTaskBo bo) {
        return toAjax(addressMonitorTaskService.updateByBo(bo));
    }

    /**
     * 重跑监控任务。
     *
     * @param id 任务主键
     * @return 操作结果
     *
     * 目的：提供任务重跑入口，当前阶段先回写最近执行信息。
     * 入参/出参：入参为任务主键，出参为统一操作结果。
     * 关键约束：仅对存在的任务生效。
     * 异常与副作用：成功后会更新任务最近执行时间。
     */
    @SaCheckPermission("address:monitor:task:execute")
    @Log(title = "监控任务", businessType = BusinessType.UPDATE)
    @PostMapping("/rerun/{id}")
    public R<Void> rerunMonitorTask(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(addressMonitorTaskService.rerunById(id));
    }

    /**
     * 暂停监控任务。
     *
     * @param id 任务主键
     * @return 操作结果
     *
     * 目的：暂停任务后续调度。
     * 入参/出参：入参为任务主键，出参为统一操作结果。
     * 关键约束：当前阶段仅更新主表状态。
     * 异常与副作用：成功后会更新任务状态。
     */
    @SaCheckPermission("address:monitor:task:edit")
    @Log(title = "监控任务", businessType = BusinessType.UPDATE)
    @PostMapping("/pause/{id}")
    public R<Void> pauseMonitorTask(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(addressMonitorTaskService.pauseById(id));
    }

    /**
     * 终止监控任务。
     *
     * @param id 任务主键
     * @return 操作结果
     *
     * 目的：终止任务并阻止后续继续执行。
     * 入参/出参：入参为任务主键，出参为统一操作结果。
     * 关键约束：当前阶段仅更新主表状态。
     * 异常与副作用：成功后会更新任务状态。
     */
    @SaCheckPermission("address:monitor:task:edit")
    @Log(title = "监控任务", businessType = BusinessType.UPDATE)
    @PostMapping("/terminate/{id}")
    public R<Void> terminateMonitorTask(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(addressMonitorTaskService.terminateById(id));
    }
}
