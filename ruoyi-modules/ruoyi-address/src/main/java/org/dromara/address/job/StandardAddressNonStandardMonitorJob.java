package org.dromara.address.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.address.monitor.MonitorExecutionService;
import org.springframework.stereotype.Component;

/**
 * 非标地址监控任务。
 * 目的：承接非标地址监控定时任务入口，在监控模块迁移完成前显式跳过旧实现。
 * 关键约束：当前版本不得再依赖历史标准地址主表执行扫描。
 * 副作用：仅输出任务日志，不写入监控记录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DS("address")
public class StandardAddressNonStandardMonitorJob {

    private final MonitorExecutionService monitorExecutionService;

    /**
     * 执行非标地址监控。
     *
     * @param args 任务参数
     *
     * 关键约束：规则为空时直接返回；已迁移前固定走跳过逻辑。
     * 副作用：输出执行日志，无数据写入。
     */
    @JobExecutor(name = "StandardAddressNonStandardMonitorJob")
    public void execute(Object args) {
        log.info("开始执行非标地址监控任务");
        int count = monitorExecutionService.executeTask(parseTaskId(args), "SCHEDULE");
        log.info("非标地址监控任务执行完成，新增异常记录: {} 条", count);
    }

    /**
     * 执行一次完整监控扫描并返回新增记录数。
     *
     * @return 新增异常记录数量
     *
     * 关键约束：仅用于判断任务是否需要提示，实际扫描逻辑待监控模块完成 `segmId` 迁移后补回。
     * 副作用：当前无数据写入。
     */
    public int executeMonitor() {
        return monitorExecutionService.executeTask(null, "MANUAL");
    }

    /**
     * 目的：从 `snailjob` 任务参数中解析监控任务主键。
     * 入参：任务参数对象。
     * 出参：解析得到的任务主键，无法解析时返回 `null`。
     * 关键约束：当前仅兼容数字或数字字符串参数。
     * 异常与副作用：解析失败时仅记录调试日志，无写入副作用。
     */
    private Long parseTaskId(Object args) {
        if (args instanceof Number number) {
            return number.longValue();
        }
        if (args instanceof String value && value.matches("\\d+")) {
            return Long.valueOf(value);
        }
        log.debug("未从任务参数中解析到监控任务ID, args={}", args);
        return null;
    }
}
