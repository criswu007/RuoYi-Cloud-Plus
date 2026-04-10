package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressAdminBo;

/**
 * 监控任务调度服务接口。
 * 目的：封装监控任务与 `snailjob` 的注册、暂停和移除动作，避免任务服务直接耦合调度实现。
 * 入参/出参：入参为任务业务对象或任务主键，出参为调度任务ID或统一布尔结果。
 * 关键约束：首版先固定围绕 `snailjob`，ES 或其他外部组件不可用时仅通过返回结果表达失败。
 * 异常与副作用：成功时会对调度中心产生注册、暂停或移除副作用。
 */
public interface MonitorTaskSchedulerService {

    /**
     * 目的：注册或刷新监控任务调度。
     * 入参：监控任务业务对象。
     * 出参：调度中心返回的任务ID；当前阶段无法分配时可返回原有任务ID。
     * 关键约束：仅对启用的定时任务执行注册或刷新。
     * 异常与副作用：成功后会创建或更新 `snailjob` 调度任务。
     */
    Long registerOrRefreshTask(StandardAddressAdminBo.MonitorTaskBo bo);

    /**
     * 目的：暂停监控任务调度。
     * 入参：任务主键。
     * 出参：是否暂停成功。
     * 关键约束：只影响后续调度，不中断已执行实例。
     * 异常与副作用：成功后会暂停对应的调度任务。
     */
    Boolean pauseTask(Long taskId);

    /**
     * 目的：移除监控任务调度。
     * 入参：任务主键。
     * 出参：是否移除成功。
     * 关键约束：终止后的任务不应再保留后续调度。
     * 异常与副作用：成功后会移除对应的调度任务。
     */
    Boolean removeTask(Long taskId);
}
