package org.dromara.address.monitor.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.StandardAddressMonitorRecord;
import org.dromara.address.domain.StandardAddressMonitorRule;
import org.dromara.address.domain.StandardAddressMonitorTask;
import org.dromara.address.domain.StandardAddressMonitorTaskRuleRel;
import org.dromara.address.domain.StandardAddressMonitorTaskRunLog;
import org.dromara.address.domain.StandardAddressMonitorTaskScopeRel;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.StandardAddressMonitorRecordMapper;
import org.dromara.address.mapper.StandardAddressMonitorRuleMapper;
import org.dromara.address.mapper.StandardAddressMonitorTaskMapper;
import org.dromara.address.mapper.StandardAddressMonitorTaskRuleRelMapper;
import org.dromara.address.mapper.StandardAddressMonitorTaskRunLogMapper;
import org.dromara.address.mapper.StandardAddressMonitorTaskScopeRelMapper;
import org.dromara.address.monitor.MonitorExecutionService;
import org.dromara.address.monitor.detector.MonitorDetectContext;
import org.dromara.address.monitor.detector.MonitorHitResult;
import org.dromara.address.monitor.detector.MonitorRuleDetector;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 监控任务执行服务实现。
 * 目的：封装监控任务统一执行入口，负责装载任务配置、分页扫描标准地址、调用 detector 并回写运行结果。
 * 入参/出参：入参为任务ID和触发方式，出参为本次新增异常记录数量。
 * 关键约束：执行链路统一围绕任务、规则与范围模型运转；detector 只负责计算，记录落库与去重由执行器集中处理。
 * 异常与副作用：会新增或更新监控记录、写入任务运行日志，并回写任务最近执行结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@DS("address")
public class MonitorExecutionServiceImpl implements MonitorExecutionService {

    private static final long SCAN_PAGE_SIZE = 200L;

    private final StandardAddressMonitorTaskMapper taskMapper;
    private final StandardAddressMonitorTaskRuleRelMapper taskRuleRelMapper;
    private final StandardAddressMonitorTaskScopeRelMapper taskScopeRelMapper;
    private final StandardAddressMonitorTaskRunLogMapper taskRunLogMapper;
    private final StandardAddressMonitorRuleMapper ruleMapper;
    private final StandardAddressMonitorRecordMapper recordMapper;
    private final AddrSegmMapper addrSegmMapper;
    private final List<MonitorRuleDetector> detectors;

    @Override
    /**
     * {@inheritDoc}
     */
    public int executeTask(Long taskId, String triggerMode) {
        Map<String, MonitorRuleDetector> detectorMap = buildDetectorMap();
        if (taskId == null) {
            return executeWithoutTask(triggerMode, detectorMap);
        }
        StandardAddressMonitorTask task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("监控执行跳过，未找到任务定义, taskId={}, triggerMode={}", taskId, triggerMode);
            return 0;
        }
        StandardAddressMonitorTaskRunLog runLog = createRunLog(task, triggerMode);
        taskRunLogMapper.insert(runLog);
        try {
            List<StandardAddressMonitorRule> rules = loadTaskRules(taskId);
            List<StandardAddressMonitorTaskScopeRel> scopeRels = loadTaskScopes(taskId);
            ExecutionStats stats = executeScan(task, runLog.getId(), rules, scopeRels, detectorMap);
            finishRunLogSuccess(runLog.getId(), stats);
            touchTaskSuccess(task.getId());
            return Math.toIntExact(stats.createdCount);
        } catch (Exception ex) {
            finishRunLogFailure(runLog.getId(), ex.getMessage());
            touchTaskFailure(task.getId(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * 目的：执行不绑定任务定义的手动全局巡检。
     * 入参：触发方式与 detector 映射。
     * 出参：新增异常记录数。
     * 关键约束：该路径不创建任务运行日志，只复用当前启用规则做全量巡检。
     * 异常与副作用：会新增或更新监控记录，但不回写任务主表。
     */
    private int executeWithoutTask(String triggerMode, Map<String, MonitorRuleDetector> detectorMap) {
        List<StandardAddressMonitorRule> rules = ruleMapper.selectList(
            Wrappers.<StandardAddressMonitorRule>lambdaQuery()
                .eq(StandardAddressMonitorRule::getStatus, "0")
                .orderByAsc(StandardAddressMonitorRule::getPriority, StandardAddressMonitorRule::getId)
        );
        if (rules == null || rules.isEmpty()) {
            log.info("监控执行跳过，未找到启用规则, taskId=null, triggerMode={}", triggerMode);
            return 0;
        }
        ExecutionStats stats = executeScan(null, null, rules, List.of(), detectorMap);
        log.info("全局手动巡检执行完成, triggerMode={}, scannedCount={}, hitCount={}, createdCount={}, unsupportedRuleCount={}",
            triggerMode, stats.scannedCount, stats.hitCount, stats.createdCount, stats.unsupportedRuleCount);
        return Math.toIntExact(stats.createdCount);
    }

    /**
     * 目的：为当前执行链路构建模板到 detector 的映射。
     * 入参：无。
     * 出参：`templateCode -> detector` 映射。
     * 关键约束：后注册的同模板 detector 不覆盖先注册实例，避免执行链路出现不稳定路由。
     * 异常与副作用：无写入副作用。
     */
    private Map<String, MonitorRuleDetector> buildDetectorMap() {
        Map<String, MonitorRuleDetector> detectorMap = new LinkedHashMap<>();
        if (detectors == null || detectors.isEmpty()) {
            return detectorMap;
        }
        for (MonitorRuleDetector detector : detectors) {
            if (detector == null || StringUtils.isBlank(detector.getTemplateCode())) {
                continue;
            }
            detectorMap.putIfAbsent(detector.getTemplateCode(), detector);
        }
        return detectorMap;
    }

    /**
     * 目的：加载任务绑定且处于启用态的规则列表。
     * 入参：任务ID。
     * 出参：任务可执行规则集合。
     * 关键约束：规则过滤必须以关系表为准，并剔除已停用规则。
     * 异常与副作用：无写入副作用。
     */
    private List<StandardAddressMonitorRule> loadTaskRules(Long taskId) {
        List<Long> ruleIds = taskRuleRelMapper.selectList(
            Wrappers.<StandardAddressMonitorTaskRuleRel>lambdaQuery()
                .eq(StandardAddressMonitorTaskRuleRel::getTaskId, taskId)
        ).stream().map(StandardAddressMonitorTaskRuleRel::getRuleId).filter(Objects::nonNull).distinct().toList();
        if (ruleIds.isEmpty()) {
            return List.of();
        }
        return ruleMapper.selectList(
            Wrappers.<StandardAddressMonitorRule>lambdaQuery()
                .in(StandardAddressMonitorRule::getId, ruleIds)
                .eq(StandardAddressMonitorRule::getStatus, "0")
                .orderByAsc(StandardAddressMonitorRule::getPriority, StandardAddressMonitorRule::getId)
        );
    }

    /**
     * 目的：加载任务的范围明细。
     * 入参：任务ID。
     * 出参：范围关系集合。
     * 关键约束：全量任务允许返回空集合，区域和地址任务必须依赖关系表值收敛扫描范围。
     * 异常与副作用：无写入副作用。
     */
    private List<StandardAddressMonitorTaskScopeRel> loadTaskScopes(Long taskId) {
        return taskScopeRelMapper.selectList(
            Wrappers.<StandardAddressMonitorTaskScopeRel>lambdaQuery()
                .eq(StandardAddressMonitorTaskScopeRel::getTaskId, taskId)
        );
    }

    /**
     * 目的：执行单次监控扫描并汇总统计结果。
     * 入参：任务定义、任务运行日志ID、规则列表、范围列表与 detector 映射。
     * 出参：本轮执行统计结果。
     * 关键约束：规则按模板筛出可执行子集；地址必须分页读取，禁止一次性加载大表。
     * 异常与副作用：会新增或更新异常记录。
     */
    private ExecutionStats executeScan(StandardAddressMonitorTask task,
                                       Long runLogId,
                                       List<StandardAddressMonitorRule> rules,
                                       List<StandardAddressMonitorTaskScopeRel> scopeRels,
                                       Map<String, MonitorRuleDetector> detectorMap) {
        ExecutionStats stats = new ExecutionStats();
        if (rules == null || rules.isEmpty()) {
            stats.message = "未配置启用规则, unsupportedRuleCount=0";
            return stats;
        }
        List<StandardAddressMonitorRule> executableRules = rules.stream()
            .filter(rule -> detectorMap.containsKey(rule.getRuleTemplate()))
            .toList();
        stats.unsupportedRuleCount = rules.size() - executableRules.size();
        if (executableRules.isEmpty()) {
            stats.message = String.format("未找到可执行的规则模板, unsupportedRuleCount=%d", stats.unsupportedRuleCount);
            return stats;
        }
        long pageNo = 1L;
        while (true) {
            Page<AddrSegm> page = addrSegmMapper.selectPage(new Page<>(pageNo, SCAN_PAGE_SIZE), buildAddressPageQuery(task, scopeRels));
            List<AddrSegm> addresses = page == null ? List.of() : page.getRecords();
            if (addresses == null || addresses.isEmpty()) {
                break;
            }
            for (AddrSegm address : addresses) {
                stats.scannedCount++;
                MonitorDetectContext context = buildDetectContext(task, address);
                for (StandardAddressMonitorRule rule : executableRules) {
                    MonitorHitResult hitResult = detectorMap.get(rule.getRuleTemplate()).detect(rule, context);
                    if (hitResult == null || !hitResult.isHit()) {
                        continue;
                    }
                    stats.hitCount++;
                    if (upsertMonitorRecord(task, runLogId, rule, context, hitResult)) {
                        stats.createdCount++;
                    }
                }
            }
            pageNo++;
        }
        stats.message = String.format("扫描完成, scannedCount=%d, hitCount=%d, createdCount=%d, unsupportedRuleCount=%d",
            stats.scannedCount, stats.hitCount, stats.createdCount, stats.unsupportedRuleCount);
        return stats;
    }

    /**
     * 目的：构建分页扫描标准地址的查询条件。
     * 入参：任务定义与范围关系集合。
     * 出参：标准地址分页查询包装器。
     * 关键约束：全量任务不附加范围条件；区域与地址任务必须使用关系表值收敛范围。
     * 异常与副作用：无写入副作用。
     */
    private LambdaQueryWrapper<AddrSegm> buildAddressPageQuery(StandardAddressMonitorTask task, List<StandardAddressMonitorTaskScopeRel> scopeRels) {
        LambdaQueryWrapper<AddrSegm> query = Wrappers.lambdaQuery();
        query.apply("coalesce(delete_state, '0') = '0'");
        if (task != null && "REGION".equals(task.getMonitorScope())) {
            List<String> regionIds = scopeRels.stream()
                .filter(item -> "REGION".equals(item.getScopeType()))
                .map(StandardAddressMonitorTaskScopeRel::getScopeValue)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
            if (regionIds.isEmpty()) {
                query.eq(AddrSegm::getSegmId, "__NO_RESULT__");
            } else {
                query.in(AddrSegm::getRegionId, regionIds);
            }
        } else if (task != null && "ADDRESS".equals(task.getMonitorScope())) {
            List<String> segmIds = scopeRels.stream()
                .filter(item -> "ADDRESS".equals(item.getScopeType()))
                .map(StandardAddressMonitorTaskScopeRel::getScopeValue)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
            if (segmIds.isEmpty()) {
                query.eq(AddrSegm::getSegmId, "__NO_RESULT__");
            } else {
                query.in(AddrSegm::getSegmId, segmIds);
            }
        }
        query.orderByAsc(AddrSegm::getSegmId);
        return query;
    }

    /**
     * 目的：把主事实表地址转换为 detector 所需的检测上下文。
     * 入参：任务定义与地址实体。
     * 出参：检测上下文对象。
     * 关键约束：标准地址主键优先解析为 Long，解析失败时允许为空并由去重键兜底。
     * 异常与副作用：无写入副作用。
     */
    private MonitorDetectContext buildDetectContext(StandardAddressMonitorTask task, AddrSegm address) {
        MonitorDetectContext context = new MonitorDetectContext();
        context.setTaskId(task == null ? null : task.getId());
        context.setTaskName(task == null ? null : task.getTaskName());
        context.setStandardAddressId(parseStandardAddressId(address == null ? null : address.getSegmId()));
        context.setStandName(address == null ? null : address.getStandName());
        context.setSegmName(address == null ? null : address.getSegmName());
        context.setRegionId(address == null ? null : address.getRegionId());
        return context;
    }

    /**
     * 目的：根据命中结果新增或更新异常记录。
     * 入参：任务定义、运行日志ID、命中规则、检测上下文与命中结果。
     * 出参：本次是否新建了异常记录。
     * 关键约束：相同 `dedupKey` 在去重窗口内只更新一条现有记录，不重复插入。
     * 异常与副作用：会写 `address_standard_monitor_record`。
     */
    private boolean upsertMonitorRecord(StandardAddressMonitorTask task,
                                       Long runLogId,
                                       StandardAddressMonitorRule rule,
                                       MonitorDetectContext context,
                                       MonitorHitResult hitResult) {
        Date now = new Date();
        String dedupKey = StringUtils.defaultIfBlank(hitResult.getDedupKey(), buildDefaultDedupKey(rule, context));
        StandardAddressMonitorRecord existing = recordMapper.selectOne(
            Wrappers.<StandardAddressMonitorRecord>lambdaQuery()
                .eq(StandardAddressMonitorRecord::getDedupKey, dedupKey)
                .ge(resolveWindowStart(rule, now) != null, StandardAddressMonitorRecord::getLastDetectedTime, resolveWindowStart(rule, now))
                .orderByDesc(StandardAddressMonitorRecord::getLastDetectedTime)
                .last("limit 1")
        );
        if (existing != null) {
            StandardAddressMonitorRecord update = new StandardAddressMonitorRecord();
            update.setId(existing.getId());
            fillRecordSnapshot(update, task, runLogId, rule, context, hitResult, dedupKey, now);
            update.setHitCount((existing.getHitCount() == null ? 0 : existing.getHitCount()) + 1);
            update.setLastDetectedTime(now);
            recordMapper.updateById(update);
            return false;
        }

        StandardAddressMonitorRecord record = new StandardAddressMonitorRecord();
        fillRecordSnapshot(record, task, runLogId, rule, context, hitResult, dedupKey, now);
        record.setFirstDetectedTime(now);
        record.setLastDetectedTime(now);
        record.setHitCount(1);
        record.setStatus("0");
        recordMapper.insert(record);
        return true;
    }

    /**
     * 目的：统一填充异常记录快照字段。
     * 入参：待写入记录、任务定义、运行日志ID、命中规则、检测上下文、命中结果、去重键与当前时间。
     * 出参：无，直接回填到传入记录。
     * 关键约束：快照字段必须独立保存，避免后续主数据变化影响预警展示。
     * 异常与副作用：会直接修改传入记录对象。
     */
    private void fillRecordSnapshot(StandardAddressMonitorRecord record,
                                    StandardAddressMonitorTask task,
                                    Long runLogId,
                                    StandardAddressMonitorRule rule,
                                    MonitorDetectContext context,
                                    MonitorHitResult hitResult,
                                    String dedupKey,
                                    Date now) {
        record.setStandardAddressId(context.getStandardAddressId());
        record.setRuleId(rule.getId());
        record.setStandNameSnapshot(context.getStandName());
        record.setRegionIdSnapshot(context.getRegionId());
        record.setRuleNameSnapshot(rule.getName());
        record.setRuleTemplateSnapshot(rule.getRuleTemplate());
        record.setTaskId(task == null ? null : task.getId());
        record.setTaskRunLogId(runLogId);
        record.setTaskNameSnapshot(task == null ? null : task.getTaskName());
        record.setSeverity(StringUtils.defaultIfBlank(hitResult.getSeverity(), rule.getSeverity()));
        record.setHitDetailJson(StringUtils.defaultIfBlank(hitResult.getDetailJson(), JSONUtil.toJsonStr(Map.of("message", "命中详情为空"))));
        record.setDedupKey(dedupKey);
        record.setRemark(null);
        record.setProcessBy(null);
        record.setProcessTime(null);
        record.setLastDetectedTime(now);
    }

    /**
     * 目的：构造默认去重键。
     * 入参：规则实体与检测上下文。
     * 出参：稳定去重键字符串。
     * 关键约束：去重键必须同时绑定规则与地址，避免不同规则错误合并。
     * 异常与副作用：无写入副作用。
     */
    private String buildDefaultDedupKey(StandardAddressMonitorRule rule, MonitorDetectContext context) {
        return String.format("%s:%s:%s",
            StringUtils.defaultIfBlank(rule.getRuleTemplate(), "UNKNOWN"),
            rule.getId(),
            context.getStandardAddressId());
    }

    /**
     * 目的：根据规则配置计算去重窗口起始时间。
     * 入参：规则实体与当前时间。
     * 出参：窗口起始时间；未配置窗口时返回 `null`。
     * 关键约束：`dedupHours <= 0` 视为不限制窗口。
     * 异常与副作用：无写入副作用。
     */
    private Date resolveWindowStart(StandardAddressMonitorRule rule, Date now) {
        if (rule == null || rule.getDedupHours() == null || rule.getDedupHours() <= 0 || now == null) {
            return null;
        }
        return new Date(now.getTime() - rule.getDedupHours() * 3600_000L);
    }

    /**
     * 目的：创建任务执行开始时的运行日志。
     * 入参：任务定义与触发方式。
     * 出参：待落库的运行日志对象。
     * 关键约束：开始时统一置为 `RUNNING`，结束状态由执行结果回写。
     * 异常与副作用：无数据库写入副作用，实际落库由调用方控制。
     */
    private StandardAddressMonitorTaskRunLog createRunLog(StandardAddressMonitorTask task, String triggerMode) {
        StandardAddressMonitorTaskRunLog runLog = new StandardAddressMonitorTaskRunLog();
        runLog.setTaskId(task.getId());
        runLog.setTriggerMode(triggerMode);
        runLog.setExecuteStatus("RUNNING");
        runLog.setExecuteMessage("监控任务开始执行");
        runLog.setScannedCount(0L);
        runLog.setHitCount(0L);
        runLog.setCreatedCount(0L);
        runLog.setStartedTime(new Date());
        return runLog;
    }

    /**
     * 目的：把运行日志回写为成功或跳过状态。
     * 入参：运行日志ID与执行统计结果。
     * 出参：无。
     * 关键约束：有扫描或命中结果时写 `SUCCESS`，否则写 `SKIPPED`。
     * 异常与副作用：会更新运行日志表。
     */
    private void finishRunLogSuccess(Long runLogId, ExecutionStats stats) {
        if (runLogId == null) {
            return;
        }
        StandardAddressMonitorTaskRunLog update = new StandardAddressMonitorTaskRunLog();
        update.setId(runLogId);
        update.setExecuteStatus(stats.scannedCount > 0 || stats.hitCount > 0 ? "SUCCESS" : "SKIPPED");
        update.setExecuteMessage(stats.message);
        update.setScannedCount(stats.scannedCount);
        update.setHitCount(stats.hitCount);
        update.setCreatedCount(stats.createdCount);
        update.setFinishedTime(new Date());
        taskRunLogMapper.updateById(update);
    }

    /**
     * 目的：把运行日志回写为失败状态。
     * 入参：运行日志ID与失败原因。
     * 出参：无。
     * 关键约束：失败原因为空时写默认文案，避免运行日志出现空白状态说明。
     * 异常与副作用：会更新运行日志表。
     */
    private void finishRunLogFailure(Long runLogId, String errorMessage) {
        if (runLogId == null) {
            return;
        }
        StandardAddressMonitorTaskRunLog update = new StandardAddressMonitorTaskRunLog();
        update.setId(runLogId);
        update.setExecuteStatus("FAILED");
        update.setExecuteMessage(StringUtils.defaultIfBlank(errorMessage, "监控任务执行失败"));
        update.setFinishedTime(new Date());
        taskRunLogMapper.updateById(update);
    }

    /**
     * 目的：回写任务最近成功执行信息。
     * 入参：任务ID。
     * 出参：无。
     * 关键约束：成功后需要同时刷新最近执行时间和最近成功时间，并清空失败原因。
     * 异常与副作用：会更新任务主表。
     */
    private void touchTaskSuccess(Long taskId) {
        StandardAddressMonitorTask update = new StandardAddressMonitorTask();
        update.setId(taskId);
        update.setLastExecuteTime(new Date());
        update.setLastSuccessTime(new Date());
        update.setLastFailureReason(null);
        taskMapper.updateById(update);
    }

    /**
     * 目的：回写任务最近失败信息。
     * 入参：任务ID与失败原因。
     * 出参：无。
     * 关键约束：失败时只更新最近执行时间和失败原因，不刷新成功时间。
     * 异常与副作用：会更新任务主表。
     */
    private void touchTaskFailure(Long taskId, String errorMessage) {
        StandardAddressMonitorTask update = new StandardAddressMonitorTask();
        update.setId(taskId);
        update.setLastExecuteTime(new Date());
        update.setLastFailureReason(StringUtils.defaultIfBlank(errorMessage, "监控任务执行失败"));
        taskMapper.updateById(update);
    }

    /**
     * 目的：把标准地址字符串主键安全转换为 Long。
     * 入参：标准地址字符串主键。
     * 出参：转换后的 Long；无法转换时返回 `null`。
     * 关键约束：监控记录当前仍沿用 Long 类型字段，非数字主键只能先降级为空。
     * 异常与副作用：转换失败只记录调试日志，无写入副作用。
     */
    private Long parseStandardAddressId(String segmId) {
        if (StringUtils.isBlank(segmId) || !segmId.matches("\\d+")) {
            return null;
        }
        return Long.valueOf(segmId);
    }

    /**
     * 执行统计结果。
     */
    private static class ExecutionStats {
        private long scannedCount;
        private long hitCount;
        private long createdCount;
        private int unsupportedRuleCount;
        private String message;
    }
}
