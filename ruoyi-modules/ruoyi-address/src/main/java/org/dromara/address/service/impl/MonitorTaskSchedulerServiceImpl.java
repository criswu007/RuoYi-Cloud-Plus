package org.dromara.address.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.dromara.address.domain.bo.StandardAddressAdminBo;
import org.dromara.address.service.MonitorTaskSchedulerService;
import org.springframework.stereotype.Service;

/**
 * 监控任务调度服务实现。
 * 目的：统一承接监控任务与 `snailjob` 的注册、暂停和移除动作。
 * 入参/出参：入参为任务业务对象或任务主键，出参为调度任务ID或统一布尔结果。
 * 关键约束：当前阶段先保留 `snailjob` 对接落点，真实远程注册能力后续补齐。
 * 异常与副作用：当前实现只输出日志，不主动抛出异常。
 */
@Slf4j
@Service
@DS("address")
public class MonitorTaskSchedulerServiceImpl implements MonitorTaskSchedulerService {

    @Override
    /**
     * {@inheritDoc}
     */
    public Long registerOrRefreshTask(StandardAddressAdminBo.MonitorTaskBo bo) {
        if (bo == null || !"SCHEDULE".equals(bo.getTaskType()) || !"ENABLED".equals(bo.getTaskStatus())) {
            return bo == null ? null : bo.getSnailJobTaskId();
        }
        log.info("监控任务调度注册占位执行, taskId={}, taskName={}, executeRule={}",
            bo.getId(), bo.getTaskName(), bo.getExecuteRule());
        return bo.getSnailJobTaskId();
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public Boolean pauseTask(Long taskId) {
        log.info("监控任务调度暂停占位执行, taskId={}", taskId);
        return true;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public Boolean removeTask(Long taskId) {
        log.info("监控任务调度移除占位执行, taskId={}", taskId);
        return true;
    }
}
