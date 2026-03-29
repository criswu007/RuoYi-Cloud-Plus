package org.dromara.address.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.address.domain.StandardAddressMonitorRule;
import org.dromara.address.mapper.StandardAddressMonitorRecordMapper;
import org.dromara.address.mapper.StandardAddressMonitorRuleMapper;
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

    private final StandardAddressMonitorRuleMapper ruleMapper;
    private final StandardAddressMonitorRecordMapper recordMapper;

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
        int count = executeMonitor();
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
        Long ruleCount = ruleMapper.selectCount(
            Wrappers.<StandardAddressMonitorRule>lambdaQuery().eq(StandardAddressMonitorRule::getStatus, "0")
        );
        if (ruleCount == null || ruleCount <= 0) {
            log.info("未找到启用的监控规则");
            return 0;
        }
        log.warn("非标地址监控任务尚未迁移到 segmId 体系，当前版本跳过执行以避免依赖旧标准地址表");
        return 0;
    }
}
