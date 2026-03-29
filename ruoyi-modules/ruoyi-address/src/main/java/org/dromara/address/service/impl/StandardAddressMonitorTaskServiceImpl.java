package org.dromara.address.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.StandardAddressMonitorRecord;
import org.dromara.address.domain.StandardAddressMonitorRule;
import org.dromara.address.domain.vo.StandardAddressMonitorTaskSummaryVo;
import org.dromara.address.job.StandardAddressNonStandardMonitorJob;
import org.dromara.address.mapper.StandardAddressMonitorRecordMapper;
import org.dromara.address.mapper.StandardAddressMonitorRuleMapper;
import org.dromara.address.service.IStandardAddressMonitorTaskService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 非标地址监控任务服务实现。
 * 目的：封装监控任务摘要统计与手动触发入口。
 * 关键约束：摘要仅统计当前服务可见数据；手动触发直接复用现有监控作业逻辑。
 * 副作用：手动触发会写入新的异常记录。
 */
@RequiredArgsConstructor
@Service
@DS("address")
public class StandardAddressMonitorTaskServiceImpl implements IStandardAddressMonitorTaskService {

    private final StandardAddressMonitorRuleMapper addressMonitorRuleMapper;
    private final StandardAddressMonitorRecordMapper addressMonitorRecordMapper;
    private final StandardAddressNonStandardMonitorJob nonStandardMonitorJob;

    private final AtomicInteger lastCreatedCount = new AtomicInteger(0);

    @Override
    public StandardAddressMonitorTaskSummaryVo querySummary() {
        StandardAddressMonitorTaskSummaryVo vo = new StandardAddressMonitorTaskSummaryVo();
        vo.setTotalRuleCount(addressMonitorRuleMapper.selectCount(Wrappers.lambdaQuery(StandardAddressMonitorRule.class)));
        vo.setEnabledRuleCount(addressMonitorRuleMapper.selectCount(
            Wrappers.<StandardAddressMonitorRule>lambdaQuery().eq(StandardAddressMonitorRule::getStatus, "0")
        ));
        vo.setPendingRecordCount(addressMonitorRecordMapper.selectCount(
            Wrappers.<StandardAddressMonitorRecord>lambdaQuery().eq(StandardAddressMonitorRecord::getStatus, "0")
        ));
        vo.setIgnoredRecordCount(addressMonitorRecordMapper.selectCount(
            Wrappers.<StandardAddressMonitorRecord>lambdaQuery().eq(StandardAddressMonitorRecord::getStatus, "1")
        ));
        vo.setProcessedRecordCount(addressMonitorRecordMapper.selectCount(
            Wrappers.<StandardAddressMonitorRecord>lambdaQuery().eq(StandardAddressMonitorRecord::getStatus, "2")
        ));
        vo.setLastCreatedCount(lastCreatedCount.get());
        return vo;
    }

    @Override
    public Integer executeNow() {
        int createdCount = nonStandardMonitorJob.executeMonitor();
        lastCreatedCount.set(createdCount);
        return createdCount;
    }
}
