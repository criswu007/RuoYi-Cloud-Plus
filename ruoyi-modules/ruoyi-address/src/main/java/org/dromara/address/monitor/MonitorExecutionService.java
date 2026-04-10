package org.dromara.address.monitor;

/**
 * 监控任务执行服务接口。
 * 目的：统一承接手动执行、任务重跑和 `snailjob` 调度触发的执行入口。
 * 入参/出参：入参为任务ID和触发方式，出参为本次新增异常记录数量。
 * 关键约束：执行链路需围绕任务、规则和范围模型运转，不直接依赖旧监控实现。
 * 异常与副作用：成功时会新增任务运行日志、异常记录并回写任务执行结果。
 */
public interface MonitorExecutionService {

    /**
     * 目的：执行指定监控任务。
     * 入参：任务ID与触发方式。
     * 出参：本次新增异常记录数量。
     * 关键约束：任务ID为空时由实现层决定是否执行默认全局监控逻辑。
     * 异常与副作用：成功时会写入运行日志与异常记录。
     */
    int executeTask(Long taskId, String triggerMode);
}
