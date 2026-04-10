package org.dromara.address.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.dromara.address.domain.StandardAddressMonitorRecord;
import org.dromara.address.domain.StandardAddressMonitorRule;
import org.dromara.address.domain.StandardAddressMonitorTask;
import org.dromara.address.domain.StandardAddressMonitorTaskRuleRel;
import org.dromara.address.domain.StandardAddressMonitorTaskRunLog;
import org.dromara.address.domain.StandardAddressMonitorTaskScopeRel;
import org.dromara.address.domain.bo.StandardAddressAdminBo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.domain.vo.StandardAddressMonitorTaskSummaryVo;
import org.dromara.address.job.StandardAddressNonStandardMonitorJob;
import org.dromara.address.mapper.StandardAddressMonitorRecordMapper;
import org.dromara.address.mapper.StandardAddressMonitorRuleMapper;
import org.dromara.address.mapper.StandardAddressMonitorTaskRuleRelMapper;
import org.dromara.address.mapper.StandardAddressMonitorTaskRunLogMapper;
import org.dromara.address.mapper.StandardAddressMonitorTaskScopeRelMapper;
import org.dromara.address.mapper.StandardAddressMonitorTaskMapper;
import org.dromara.address.monitor.MonitorExecutionService;
import org.dromara.address.service.MonitorTaskSchedulerService;
import org.dromara.address.service.IStandardAddressMonitorTaskService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 非标地址监控任务服务实现。
 * 目的：封装监控任务摘要统计与手动触发入口。
 * 关键约束：摘要仅统计当前服务可见数据；手动触发直接复用现有监控作业逻辑。
 * 副作用：手动触发会写入新的异常记录。
 */
@Slf4j
@Service
@DS("address")
public class StandardAddressMonitorTaskServiceImpl implements IStandardAddressMonitorTaskService {

    private static final String MANUAL_EXECUTION_LOCK_KEY = "address:monitor:task:manual:execute";
    private static final long MANUAL_EXECUTION_LOCK_EXPIRE_MILLIS = 1800000L;

    private final StandardAddressMonitorTaskMapper monitorTaskMapper;
    private final StandardAddressMonitorTaskRuleRelMapper taskRuleRelMapper;
    private final StandardAddressMonitorTaskScopeRelMapper taskScopeRelMapper;
    private final StandardAddressMonitorTaskRunLogMapper taskRunLogMapper;
    private final StandardAddressMonitorRuleMapper addressMonitorRuleMapper;
    private final StandardAddressMonitorRecordMapper addressMonitorRecordMapper;
    private final StandardAddressNonStandardMonitorJob nonStandardMonitorJob;
    private final MonitorTaskSchedulerService monitorTaskSchedulerService;
    private final MonitorExecutionService monitorExecutionService;
    private final RedissonClient redissonClient;
    private final TaskExecutor taskExecutor;

    private final AtomicInteger lastCreatedCount = new AtomicInteger(0);

    /**
     * 目的：构造监控任务服务并显式绑定应用异步执行器。
     * 入参/出参：入参为监控任务相关持久层、执行器、分布式锁模板与应用异步执行器；出参为无。
     * 关键约束：异步执行器必须使用 `applicationTaskExecutor`，避免与其他默认执行器混用导致巡检任务堆积到错误线程池。
     * 异常与副作用：仅完成依赖注入，不直接触发监控执行或数据库写入。
     */
    public StandardAddressMonitorTaskServiceImpl(
        StandardAddressMonitorTaskMapper monitorTaskMapper,
        StandardAddressMonitorTaskRuleRelMapper taskRuleRelMapper,
        StandardAddressMonitorTaskScopeRelMapper taskScopeRelMapper,
        StandardAddressMonitorTaskRunLogMapper taskRunLogMapper,
        StandardAddressMonitorRuleMapper addressMonitorRuleMapper,
        StandardAddressMonitorRecordMapper addressMonitorRecordMapper,
        StandardAddressNonStandardMonitorJob nonStandardMonitorJob,
        MonitorTaskSchedulerService monitorTaskSchedulerService,
        MonitorExecutionService monitorExecutionService,
        RedissonClient redissonClient,
        @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor
    ) {
        this.monitorTaskMapper = monitorTaskMapper;
        this.taskRuleRelMapper = taskRuleRelMapper;
        this.taskScopeRelMapper = taskScopeRelMapper;
        this.taskRunLogMapper = taskRunLogMapper;
        this.addressMonitorRuleMapper = addressMonitorRuleMapper;
        this.addressMonitorRecordMapper = addressMonitorRecordMapper;
        this.nonStandardMonitorJob = nonStandardMonitorJob;
        this.monitorTaskSchedulerService = monitorTaskSchedulerService;
        this.monitorExecutionService = monitorExecutionService;
        this.redissonClient = redissonClient;
        this.taskExecutor = taskExecutor;
    }

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
        String executionToken = acquireManualExecutionToken();
        if (executionToken == null) {
            return 0;
        }
        try {
            taskExecutor.execute(() -> executeManualMonitorInBackground(executionToken));
            return 1;
        } catch (RuntimeException ex) {
            releaseManualExecutionToken(executionToken);
            throw ex;
        }
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public TableDataInfo<StandardAddressAdminVo.MonitorTaskVo> queryPageList(StandardAddressAdminBo.MonitorTaskBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<StandardAddressMonitorTask> lqw = buildTaskQueryWrapper(bo);
        Page<StandardAddressMonitorTask> page = monitorTaskMapper.selectPage(pageQuery.build(), lqw);
        List<StandardAddressAdminVo.MonitorTaskVo> records = page.getRecords().stream()
            .map(this::buildTaskVo)
            .collect(Collectors.toList());
        fillTaskRelations(records);
        return new TableDataInfo<>(records, page.getTotal());
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public StandardAddressAdminVo.MonitorTaskVo queryById(Long id) {
        StandardAddressMonitorTask entity = monitorTaskMapper.selectById(id);
        StandardAddressAdminVo.MonitorTaskVo vo = buildTaskVo(entity);
        fillTaskRelations(vo == null ? Collections.emptyList() : List.of(vo));
        return vo;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public TableDataInfo<StandardAddressAdminVo.MonitorTaskRunLogVo> queryRunLogPageList(Long taskId, PageQuery pageQuery) {
        LambdaQueryWrapper<StandardAddressMonitorTaskRunLog> lqw = Wrappers.<StandardAddressMonitorTaskRunLog>lambdaQuery()
            .eq(taskId != null, StandardAddressMonitorTaskRunLog::getTaskId, taskId)
            .orderByDesc(StandardAddressMonitorTaskRunLog::getStartedTime, StandardAddressMonitorTaskRunLog::getId);
        Page<StandardAddressMonitorTaskRunLog> page = taskRunLogMapper.selectPage(pageQuery.build(), lqw);
        List<StandardAddressAdminVo.MonitorTaskRunLogVo> records = page.getRecords().stream()
            .map(this::buildTaskRunLogVo)
            .collect(Collectors.toList());
        return new TableDataInfo<>(records, page.getTotal());
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public Boolean insertByBo(StandardAddressAdminBo.MonitorTaskBo bo) {
        StandardAddressMonitorTask entity = buildTaskEntity(bo);
        boolean success = monitorTaskMapper.insert(entity) > 0;
        if (success) {
            bo.setId(entity.getId());
            saveTaskRelations(bo);
            refreshScheduler(bo);
        }
        return success;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public Boolean updateByBo(StandardAddressAdminBo.MonitorTaskBo bo) {
        StandardAddressMonitorTask entity = buildTaskEntity(bo);
        boolean success = monitorTaskMapper.updateById(entity) > 0;
        if (success) {
            saveTaskRelations(bo);
            refreshScheduler(bo);
        }
        return success;
    }

    /**
     * 目的：将监控任务业务对象显式转换为任务实体。
     * 入参/出参：入参为任务业务对象，出参为可直接落库的任务实体。
     * 关键约束：仅映射当前任务主表真实持久化字段，规则与范围集合仍由关系表单独保存。
     * 异常与副作用：当入参为空时返回空实体；无外部写入副作用。
     */
    private StandardAddressMonitorTask buildTaskEntity(StandardAddressAdminBo.MonitorTaskBo bo) {
        StandardAddressMonitorTask entity = new StandardAddressMonitorTask();
        if (bo == null) {
            return entity;
        }
        entity.setId(bo.getId());
        entity.setTaskName(bo.getTaskName());
        entity.setTaskType(bo.getTaskType());
        entity.setExecuteRule(bo.getExecuteRule());
        entity.setMonitorScope(bo.getMonitorScope());
        entity.setTaskStatus(bo.getTaskStatus());
        entity.setTaskDesc(bo.getTaskDesc());
        entity.setSnailJobTaskId(bo.getSnailJobTaskId());
        entity.setLastFailureReason(bo.getFailureReason());
        return entity;
    }

    /**
     * 目的：将监控任务实体显式转换为任务视图对象。
     * 入参/出参：入参为任务实体，出参为任务管理页使用的视图对象。
     * 关键约束：仅映射主表字段，规则与范围集合由 `fillTaskRelations` 二次补齐。
     * 异常与副作用：实体为空时返回 `null`；无外部副作用。
     */
    private StandardAddressAdminVo.MonitorTaskVo buildTaskVo(StandardAddressMonitorTask entity) {
        if (entity == null) {
            return null;
        }
        StandardAddressAdminVo.MonitorTaskVo vo = new StandardAddressAdminVo.MonitorTaskVo();
        vo.setId(entity.getId());
        vo.setTaskName(entity.getTaskName());
        vo.setTaskType(entity.getTaskType());
        vo.setExecuteRule(entity.getExecuteRule());
        vo.setLastExecuteTime(entity.getLastExecuteTime());
        vo.setLastSuccessTime(entity.getLastSuccessTime());
        vo.setTaskStatus(entity.getTaskStatus());
        vo.setMonitorScope(entity.getMonitorScope());
        vo.setTaskDesc(entity.getTaskDesc());
        vo.setLastFailureReason(entity.getLastFailureReason());
        vo.setSnailJobTaskId(entity.getSnailJobTaskId());
        return vo;
    }

    /**
     * 目的：将运行日志实体显式转换为运行日志视图对象。
     * 入参/出参：入参为运行日志实体，出参为任务运行日志列表视图对象。
     * 关键约束：首版仅映射列表展示所需字段，不补充额外统计派生值。
     * 异常与副作用：实体为空时返回 `null`；无外部副作用。
     */
    private StandardAddressAdminVo.MonitorTaskRunLogVo buildTaskRunLogVo(StandardAddressMonitorTaskRunLog entity) {
        if (entity == null) {
            return null;
        }
        StandardAddressAdminVo.MonitorTaskRunLogVo vo = new StandardAddressAdminVo.MonitorTaskRunLogVo();
        vo.setId(entity.getId());
        vo.setTaskId(entity.getTaskId());
        vo.setTriggerMode(entity.getTriggerMode());
        vo.setExecuteStatus(entity.getExecuteStatus());
        vo.setExecuteMessage(entity.getExecuteMessage());
        vo.setScannedCount(entity.getScannedCount());
        vo.setHitCount(entity.getHitCount());
        vo.setCreatedCount(entity.getCreatedCount());
        vo.setStartedTime(entity.getStartedTime());
        vo.setFinishedTime(entity.getFinishedTime());
        return vo;
    }

    /**
     * 目的：为顶部“立即执行监控”入口申请 Redis 占位令牌。
     * 入参/出参：无显式入参；成功时返回当前执行令牌，失败时返回 `null`。
     * 关键约束：令牌必须使用 `setIfAbsent` 语义占位，避免同一时刻出现多个全局手动巡检。
     * 异常与副作用：会写入一个带过期时间的 Redis 键；不直接写业务表。
     */
    private String acquireManualExecutionToken() {
        String executionToken = UUID.randomUUID().toString();
        RBucket<String> bucket = redissonClient.getBucket(MANUAL_EXECUTION_LOCK_KEY);
        boolean acquired = bucket.setIfAbsent(executionToken, Duration.ofMillis(MANUAL_EXECUTION_LOCK_EXPIRE_MILLIS));
        return acquired ? executionToken : null;
    }

    /**
     * 目的：在后台线程中执行全局手动巡检并在结束后释放 Redis 占位令牌。
     * 入参/出参：入参为当前执行令牌；无返回值。
     * 关键约束：无论执行成功还是失败都必须尝试释放当前令牌，但不能误删后续新执行写入的占位键。
     * 异常与副作用：会触发真实监控扫描、更新最近新增数量并写日志；异常仅记录，不回抛到 HTTP 调用线程。
     */
    private void executeManualMonitorInBackground(String executionToken) {
        try {
            int createdCount = nonStandardMonitorJob.executeMonitor();
            lastCreatedCount.set(createdCount);
            log.info("手动非标地址巡检执行完成, createdCount={}", createdCount);
        } catch (Exception ex) {
            log.error("手动非标地址巡检执行失败", ex);
        } finally {
            releaseManualExecutionToken(executionToken);
        }
    }

    /**
     * 目的：释放当前手动巡检占位令牌。
     * 入参/出参：入参为当前执行令牌；无返回值。
     * 关键约束：只有 Redis 中仍保存当前令牌时才允许删除，避免前一轮异步线程误删后一轮已提交任务的占位键。
     * 异常与副作用：会删除 Redis 键；无数据库写入副作用。
     */
    private void releaseManualExecutionToken(String executionToken) {
        RBucket<String> bucket = redissonClient.getBucket(MANUAL_EXECUTION_LOCK_KEY);
        String currentToken = bucket.get();
        if (StringUtils.equals(executionToken, currentToken)) {
            bucket.delete();
        }
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public Boolean rerunById(Long id) {
        if (id == null) {
            return false;
        }
        StandardAddressMonitorTask task = monitorTaskMapper.selectById(id);
        if (task == null) {
            return false;
        }
        monitorExecutionService.executeTask(id, "MANUAL");
        return true;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public Boolean pauseById(Long id) {
        boolean success = updateTaskStatus(id, "PAUSED");
        if (success) {
            monitorTaskSchedulerService.pauseTask(id);
        }
        return success;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public Boolean terminateById(Long id) {
        boolean success = updateTaskStatus(id, "TERMINATED");
        if (success) {
            monitorTaskSchedulerService.removeTask(id);
        }
        return success;
    }

    /**
     * 目的：构建监控任务查询条件。
     * 入参：任务查询业务对象。
     * 出参：任务主表查询包装器。
     * 关键约束：当前阶段仅对主表字段做筛选，不下钻关联规则和范围表。
     * 异常与副作用：无写入副作用。
     */
    private LambdaQueryWrapper<StandardAddressMonitorTask> buildTaskQueryWrapper(StandardAddressAdminBo.MonitorTaskBo bo) {
        LambdaQueryWrapper<StandardAddressMonitorTask> lqw = Wrappers.lambdaQuery();
        if (bo == null) {
            return lqw;
        }
        lqw.like(StringUtils.isNotBlank(bo.getTaskName()), StandardAddressMonitorTask::getTaskName, bo.getTaskName());
        lqw.eq(StringUtils.isNotBlank(bo.getTaskType()), StandardAddressMonitorTask::getTaskType, bo.getTaskType());
        lqw.eq(StringUtils.isNotBlank(bo.getTaskStatus()), StandardAddressMonitorTask::getTaskStatus, bo.getTaskStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitorScope()), StandardAddressMonitorTask::getMonitorScope, bo.getMonitorScope());
        return lqw;
    }

    /**
     * 目的：统一更新监控任务状态。
     * 入参：任务ID与目标状态。
     * 出参：是否更新成功。
     * 关键约束：仅更新主表状态字段，不在当前阶段触发调度同步。
     * 异常与副作用：成功后会修改任务状态。
     */
    private Boolean updateTaskStatus(Long id, String targetStatus) {
        if (id == null) {
            return false;
        }
        StandardAddressMonitorTask entity = new StandardAddressMonitorTask();
        entity.setId(id);
        entity.setTaskStatus(targetStatus);
        return monitorTaskMapper.updateById(entity) > 0;
    }

    /**
     * 目的：批量填充任务与规则、范围之间的展示关系。
     * 入参：任务视图列表。
     * 出参：无，直接回填到传入视图对象。
     * 关键约束：必须批量查询关系表，避免任务列表出现逐条回查 N+1。
     * 异常与副作用：无写入副作用，会直接修改传入视图对象。
     */
    private void fillTaskRelations(List<StandardAddressAdminVo.MonitorTaskVo> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        List<Long> taskIds = tasks.stream().map(StandardAddressAdminVo.MonitorTaskVo::getId).filter(java.util.Objects::nonNull).toList();
        if (taskIds.isEmpty()) {
            return;
        }
        Map<Long, List<Long>> taskRuleMap = taskRuleRelMapper.selectList(
            Wrappers.<StandardAddressMonitorTaskRuleRel>lambdaQuery().in(StandardAddressMonitorTaskRuleRel::getTaskId, taskIds)
        ).stream().collect(Collectors.groupingBy(
            StandardAddressMonitorTaskRuleRel::getTaskId,
            LinkedHashMap::new,
            Collectors.mapping(StandardAddressMonitorTaskRuleRel::getRuleId, Collectors.toList())
        ));
        List<StandardAddressMonitorTaskScopeRel> scopeRels = taskScopeRelMapper.selectList(
            Wrappers.<StandardAddressMonitorTaskScopeRel>lambdaQuery().in(StandardAddressMonitorTaskScopeRel::getTaskId, taskIds)
        );
        Map<Long, List<String>> regionMap = scopeRels.stream()
            .filter(item -> "REGION".equals(item.getScopeType()))
            .collect(Collectors.groupingBy(
                StandardAddressMonitorTaskScopeRel::getTaskId,
                LinkedHashMap::new,
                Collectors.mapping(StandardAddressMonitorTaskScopeRel::getScopeValue, Collectors.toList())
            ));
        Map<Long, List<Long>> addressMap = scopeRels.stream()
            .filter(item -> "ADDRESS".equals(item.getScopeType()))
            .collect(Collectors.groupingBy(
                StandardAddressMonitorTaskScopeRel::getTaskId,
                LinkedHashMap::new,
                Collectors.mapping(item -> Long.valueOf(item.getScopeValue()), Collectors.toList())
            ));
        for (StandardAddressAdminVo.MonitorTaskVo item : tasks) {
            if (item == null || item.getId() == null) {
                continue;
            }
            item.setRelatedRuleIds(taskRuleMap.getOrDefault(item.getId(), Collections.emptyList()));
            item.setRegionIds(regionMap.getOrDefault(item.getId(), Collections.emptyList()));
            item.setAddressIds(addressMap.getOrDefault(item.getId(), Collections.emptyList()));
        }
    }

    /**
     * 目的：保存任务关联的规则与范围明细。
     * 入参：任务业务对象。
     * 出参：无。
     * 关键约束：保存前先按任务ID清理旧关系，再全量重建当前关系。
     * 异常与副作用：会删除旧关系并新增当前任务的规则、范围关系记录。
     */
    private void saveTaskRelations(StandardAddressAdminBo.MonitorTaskBo bo) {
        if (bo == null || bo.getId() == null) {
            return;
        }
        taskRuleRelMapper.delete(
            Wrappers.<StandardAddressMonitorTaskRuleRel>lambdaQuery().eq(StandardAddressMonitorTaskRuleRel::getTaskId, bo.getId())
        );
        taskScopeRelMapper.delete(
            Wrappers.<StandardAddressMonitorTaskScopeRel>lambdaQuery().eq(StandardAddressMonitorTaskScopeRel::getTaskId, bo.getId())
        );
        List<Long> relatedRuleIds = bo.getRelatedRuleIds() == null ? Collections.emptyList() : bo.getRelatedRuleIds();
        for (Long ruleId : relatedRuleIds) {
            if (ruleId == null) {
                continue;
            }
            StandardAddressMonitorTaskRuleRel rel = new StandardAddressMonitorTaskRuleRel();
            rel.setTaskId(bo.getId());
            rel.setRuleId(ruleId);
            taskRuleRelMapper.insert(rel);
        }
        for (String regionId : normalizeScopeValues(bo.getRegionIds())) {
            insertScopeRel(bo.getId(), "REGION", regionId);
        }
        List<Long> addressIds = bo.getAddressIds() == null ? Collections.emptyList() : bo.getAddressIds();
        for (Long addressId : addressIds) {
            if (addressId == null) {
                continue;
            }
            insertScopeRel(bo.getId(), "ADDRESS", String.valueOf(addressId));
        }
    }

    /**
     * 目的：刷新任务对应的调度配置。
     * 入参：任务业务对象。
     * 出参：无。
     * 关键约束：启用中的定时任务才注册或刷新调度；暂停和终止由专用动作处理。
     * 异常与副作用：成功时会回写任务的 `snailJobTaskId`。
     */
    private void refreshScheduler(StandardAddressAdminBo.MonitorTaskBo bo) {
        if (bo == null) {
            return;
        }
        if ("PAUSED".equals(bo.getTaskStatus())) {
            monitorTaskSchedulerService.pauseTask(bo.getId());
            return;
        }
        if ("TERMINATED".equals(bo.getTaskStatus())) {
            monitorTaskSchedulerService.removeTask(bo.getId());
            return;
        }
        Long snailJobTaskId = monitorTaskSchedulerService.registerOrRefreshTask(bo);
        if (snailJobTaskId == null || java.util.Objects.equals(snailJobTaskId, bo.getSnailJobTaskId())) {
            return;
        }
        StandardAddressMonitorTask entity = new StandardAddressMonitorTask();
        entity.setId(bo.getId());
        entity.setSnailJobTaskId(snailJobTaskId);
        monitorTaskMapper.updateById(entity);
        bo.setSnailJobTaskId(snailJobTaskId);
    }

    /**
     * 目的：插入一条任务范围关系记录。
     * 入参：任务ID、范围类型和范围值。
     * 出参：无。
     * 关键约束：空值直接跳过，不生成无效关系。
     * 异常与副作用：成功后会新增一条任务范围关系记录。
     */
    private void insertScopeRel(Long taskId, String scopeType, String scopeValue) {
        if (taskId == null || StringUtils.isBlank(scopeValue)) {
            return;
        }
        StandardAddressMonitorTaskScopeRel rel = new StandardAddressMonitorTaskScopeRel();
        rel.setTaskId(taskId);
        rel.setScopeType(scopeType);
        rel.setScopeValue(scopeValue);
        taskScopeRelMapper.insert(rel);
    }

    /**
     * 目的：清洗范围值集合中的空串和重复值。
     * 入参：原始范围值列表。
     * 出参：清洗后的范围值列表。
     * 关键约束：返回顺序保持输入顺序。
     * 异常与副作用：无写入副作用。
     */
    private List<String> normalizeScopeValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.isBlank(value) || result.contains(value)) {
                continue;
            }
            result.add(value);
        }
        return result;
    }
}
