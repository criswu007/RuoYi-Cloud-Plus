package org.dromara.address.search.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.executor.RedissonLockExecutor;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.address.config.AddressSearchProperties;
import org.dromara.address.domain.AddressSearchMaintenanceTask;
import org.dromara.address.domain.AddressSearchRepairTask;
import org.dromara.address.domain.bo.AddressSearchMaintenanceBo;
import org.dromara.address.domain.vo.AddressSearchMaintenanceVo;
import org.dromara.address.mapper.AddressSearchMaintenanceTaskMapper;
import org.dromara.address.mapper.AddressSearchRepairTaskMapper;
import org.dromara.address.search.model.AddressSearchIndexRuntimeInfo;
import org.dromara.address.search.support.AddressSearchMaintenanceProgressListener;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 地址搜索运维任务编排服务。
 * <p>
 * 目的：统一承载 ES 运维台的重建任务入队、进度回写、概览聚合和 repair 重放门禁，屏蔽控制器对底层执行器的直接依赖。
 * 入参/出参：输入运维口令、任务查询条件或 repair 任务主键，输出任务主键、概览视图或分页结果。
 * 关键约束：所有写操作必须通过 Redis 锁串行化；重建任务以异步方式执行；概览查询只做聚合，不触发实际运维动作。
 * 异常与副作用：会写 `address_search_maintenance_task`、读取 repair 表与 ES 运行态，并触发异步重建或 repair 执行。
 * </p>
 */
@Service
public class AddressSearchMaintenanceTaskService {

    private static final String TASK_TYPE_REBUILD_STANDARD = "REBUILD_STANDARD";
    private static final String TASK_TYPE_REBUILD_INSTALLATION = "REBUILD_INSTALLATION";
    private static final String REPAIR_CONFIRMATION_CODE = "REPLAY_REPAIR";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final long LOCK_EXPIRE_MILLIS = 30000L;
    private static final long LOCK_ACQUIRE_TIMEOUT_MILLIS = 3000L;

    private final AddressSearchMaintenanceTaskMapper taskMapper;
    private final AddressSearchRepairTaskMapper repairTaskMapper;
    private final AddressSearchMaintenanceService maintenanceService;
    private final StandardAddressSearchGateway standardAddressSearchGateway;
    private final InstallationAddressSearchGateway installationAddressSearchGateway;
    private final AddressSearchProperties properties;
    private final LockTemplate lockTemplate;
    private final TaskExecutor taskExecutor;

    /**
     * 目的：构造 ES 运维任务编排服务并显式绑定应用异步执行器。
     * 入参：任务/repair 持久层、运维服务、索引网关、搜索配置、分布式锁模板与应用任务执行器。
     * 出参：无。
     * 关键约束：异步执行器必须使用 `applicationTaskExecutor`，避免与 Spring Integration 自动注册的 `taskScheduler` 产生歧义。
     * 异常与副作用：仅完成依赖注入，不直接触发任务执行或持久化副作用。
     *
     * @param taskMapper 运维任务 Mapper
     * @param repairTaskMapper repair 任务 Mapper
     * @param maintenanceService 运维执行服务
     * @param standardAddressSearchGateway 标准地址索引网关
     * @param installationAddressSearchGateway 安装地址索引网关
     * @param properties 搜索配置
     * @param lockTemplate 分布式锁模板
     * @param taskExecutor 应用异步执行器
     */
    public AddressSearchMaintenanceTaskService(
        AddressSearchMaintenanceTaskMapper taskMapper,
        AddressSearchRepairTaskMapper repairTaskMapper,
        AddressSearchMaintenanceService maintenanceService,
        StandardAddressSearchGateway standardAddressSearchGateway,
        InstallationAddressSearchGateway installationAddressSearchGateway,
        AddressSearchProperties properties,
        LockTemplate lockTemplate,
        @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor
    ) {
        this.taskMapper = taskMapper;
        this.repairTaskMapper = repairTaskMapper;
        this.maintenanceService = maintenanceService;
        this.standardAddressSearchGateway = standardAddressSearchGateway;
        this.installationAddressSearchGateway = installationAddressSearchGateway;
        this.properties = properties;
        this.lockTemplate = lockTemplate;
        this.taskExecutor = taskExecutor;
    }

    /**
     * 目的：提交标准地址全量重建任务并异步派发执行。
     * 入参：标准地址重建确认口令。
     * 出参：新建运维任务主键。
     * 关键约束：口令必须匹配 `REBUILD_STANDARD`；若已有写任务占用锁则必须拒绝。
     * 异常与副作用：会申请 Redis 锁、写入任务表，并派发异步重建线程。
     *
     * @param confirmationCode 标准地址重建确认口令
     * @return 运维任务主键
     */
    public Long submitStandardRebuild(String confirmationCode) {
        validateConfirmationCode(confirmationCode, TASK_TYPE_REBUILD_STANDARD);
        LockInfo lockInfo = acquireWriteGuard();
        AddressSearchMaintenanceTask task = createPendingTask(TASK_TYPE_REBUILD_STANDARD, properties.getStandard().getAlias());
        taskExecutor.execute(() -> executeStandardTask(task.getId(), lockInfo));
        return task.getId();
    }

    /**
     * 目的：提交安装地址全量重建任务并异步派发执行。
     * 入参：安装地址重建确认口令。
     * 出参：新建运维任务主键。
     * 关键约束：口令必须匹配 `REBUILD_INSTALLATION`；若已有写任务占用锁则必须拒绝。
     * 异常与副作用：会申请 Redis 锁、写入任务表，并派发异步重建线程。
     *
     * @param confirmationCode 安装地址重建确认口令
     * @return 运维任务主键
     */
    public Long submitInstallationRebuild(String confirmationCode) {
        validateConfirmationCode(confirmationCode, TASK_TYPE_REBUILD_INSTALLATION);
        LockInfo lockInfo = acquireWriteGuard();
        AddressSearchMaintenanceTask task = createPendingTask(TASK_TYPE_REBUILD_INSTALLATION, properties.getInstallation().getAlias());
        taskExecutor.execute(() -> executeInstallationTask(task.getId(), lockInfo));
        return task.getId();
    }

    /**
     * 目的：聚合 ES 运维页概览所需的索引运行态、repair 统计和运行中任务信息。
     * 入参：无。
     * 出参：运维总览视图。
     * 关键约束：只做状态查询，不触发任何写操作；repair 统计口径以 repair 表当前状态为准。
     * 异常与副作用：会访问 ES 运行态接口和数据库查询任务/repair 状态。
     *
     * @return 运维概览
     */
    public AddressSearchMaintenanceVo.OverviewVo getOverview() {
        AddressSearchMaintenanceVo.OverviewVo overview = new AddressSearchMaintenanceVo.OverviewVo();
        overview.setEsReachable(standardAddressSearchGateway.ping());
        overview.setStandardIndex(toIndexSummary(standardAddressSearchGateway.getRuntimeInfo()));
        overview.setInstallationIndex(toIndexSummary(installationAddressSearchGateway.getRuntimeInfo()));
        overview.setPendingRepairCount(countRepairTasksByStatus(STATUS_PENDING));
        overview.setFailedRepairCount(countRepairTasksByStatus(STATUS_FAILED));
        overview.setRunningTask(findRunningTask());
        overview.setKibanaUrl(properties.getKibanaUrl());
        return overview;
    }

    /**
     * 目的：按条件分页查询运维任务记录。
     * 入参：任务查询条件与分页参数。
     * 出参：任务分页视图。
     * 关键约束：仅按任务类型和状态过滤，排序口径固定为创建时间倒序。
     * 异常与副作用：会读取任务表，不触发写操作。
     *
     * @param bo 任务查询条件
     * @param pageQuery 分页参数
     * @return 运维任务分页结果
     */
    public TableDataInfo<AddressSearchMaintenanceVo.TaskVo> listTasks(AddressSearchMaintenanceBo.TaskQueryBo bo, PageQuery pageQuery) {
        AddressSearchMaintenanceBo.TaskQueryBo actualBo = bo == null ? new AddressSearchMaintenanceBo.TaskQueryBo() : bo;
        PageQuery actualPageQuery = pageQuery == null ? new PageQuery() : pageQuery;
        Page<AddressSearchMaintenanceTask> page = taskMapper.selectPage(actualPageQuery.build(),
            Wrappers.<AddressSearchMaintenanceTask>lambdaQuery()
                .eq(StringUtils.isNotBlank(actualBo.getTaskType()), AddressSearchMaintenanceTask::getTaskType, actualBo.getTaskType())
                .eq(StringUtils.isNotBlank(actualBo.getStatus()), AddressSearchMaintenanceTask::getStatus, actualBo.getStatus())
                .orderByDesc(AddressSearchMaintenanceTask::getCreatedTime));
        return TableDataInfo.build(page.convert(this::toTaskVo));
    }

    /**
     * 目的：查询单个运维任务详情。
     * 入参：任务主键。
     * 出参：任务视图；不存在时返回 `null`。
     * 关键约束：仅按主键查询，不做额外聚合。
     * 异常与副作用：会读取任务表，不触发写操作。
     *
     * @param taskId 运维任务主键
     * @return 运维任务视图
     */
    public AddressSearchMaintenanceVo.TaskVo getTask(Long taskId) {
        AddressSearchMaintenanceTask task = taskMapper.selectById(taskId);
        return task == null ? null : toTaskVo(task);
    }

    /**
     * 目的：按条件分页查询 repair 任务。
     * 入参：repair 查询条件与分页参数。
     * 出参：repair 任务分页视图。
     * 关键约束：仅按实体类型和状态过滤，排序口径固定为更新时间倒序。
     * 异常与副作用：会读取 repair 表，不触发写操作。
     *
     * @param bo repair 查询条件
     * @param pageQuery 分页参数
     * @return repair 任务分页结果
     */
    public TableDataInfo<AddressSearchMaintenanceVo.RepairTaskVo> listRepairTasks(AddressSearchMaintenanceBo.RepairTaskQueryBo bo, PageQuery pageQuery) {
        AddressSearchMaintenanceBo.RepairTaskQueryBo actualBo = bo == null ? new AddressSearchMaintenanceBo.RepairTaskQueryBo() : bo;
        PageQuery actualPageQuery = pageQuery == null ? new PageQuery() : pageQuery;
        Page<AddressSearchRepairTask> page = repairTaskMapper.selectPage(actualPageQuery.build(),
            Wrappers.<AddressSearchRepairTask>lambdaQuery()
                .eq(StringUtils.isNotBlank(actualBo.getEntityType()), AddressSearchRepairTask::getEntityType, actualBo.getEntityType())
                .eq(StringUtils.isNotBlank(actualBo.getStatus()), AddressSearchRepairTask::getStatus, actualBo.getStatus())
                .orderByDesc(AddressSearchRepairTask::getUpdatedTime));
        return TableDataInfo.build(page.convert(this::toRepairTaskVo));
    }

    /**
     * 目的：在串行门禁保护下重放单条 repair 任务。
     * 入参：repair 任务主键与确认口令。
     * 出参：无。
     * 关键约束：口令必须匹配 `REPLAY_REPAIR`；repair 与重建共享同一把写锁，避免并发写 ES。
     * 异常与副作用：会申请 Redis 锁，并触发 repair 执行及 repair 表状态回写。
     *
     * @param taskId repair 任务主键
     * @param confirmationCode repair 执行确认口令
     */
    public void executeRepairTask(Long taskId, String confirmationCode) {
        validateConfirmationCode(confirmationCode, REPAIR_CONFIRMATION_CODE);
        LockInfo lockInfo = acquireWriteGuard();
        try {
            maintenanceService.executeRepairTask(taskId);
        } finally {
            lockTemplate.releaseLock(lockInfo);
        }
    }

    private void executeStandardTask(Long taskId, LockInfo lockInfo) {
        markTaskRunning(taskId);
        try {
            maintenanceService.rebuildStandardIndex(buildProgressListener(taskId));
            markTaskSuccess(taskId);
        } catch (Exception ex) {
            markTaskFailed(taskId, ex.getMessage());
        } finally {
            lockTemplate.releaseLock(lockInfo);
        }
    }

    private void executeInstallationTask(Long taskId, LockInfo lockInfo) {
        markTaskRunning(taskId);
        try {
            maintenanceService.rebuildInstallationIndex(buildProgressListener(taskId));
            markTaskSuccess(taskId);
        } catch (Exception ex) {
            markTaskFailed(taskId, ex.getMessage());
        } finally {
            lockTemplate.releaseLock(lockInfo);
        }
    }

    private LockInfo acquireWriteGuard() {
        LockInfo lockInfo = lockTemplate.lock(
            properties.getMaintenanceLockKey(),
            LOCK_EXPIRE_MILLIS,
            LOCK_ACQUIRE_TIMEOUT_MILLIS,
            RedissonLockExecutor.class
        );
        if (lockInfo == null) {
            throw new IllegalStateException("当前已有 ES 运维写任务执行中，请稍后再试");
        }
        return lockInfo;
    }

    private void validateConfirmationCode(String actual, String expected) {
        if (!StringUtils.equals(actual, expected)) {
            throw new IllegalArgumentException("确认口令错误");
        }
    }

    private AddressSearchMaintenanceTask createPendingTask(String taskType, String targetAlias) {
        AddressSearchMaintenanceTask task = new AddressSearchMaintenanceTask();
        Date now = new Date();
        task.setId(IdUtil.getSnowflakeNextId());
        task.setTaskType(taskType);
        task.setTargetAlias(targetAlias);
        task.setStatus(STATUS_PENDING);
        task.setCurrentPhase("PRECHECK");
        task.setTotalCount(0L);
        task.setProcessedCount(0L);
        task.setProgressPercent(0);
        task.setTriggerBy(LoginHelper.isLogin() ? LoginHelper.getUserIdStr() : "standalone");
        task.setCreatedTime(now);
        task.setUpdatedTime(now);
        taskMapper.insert(task);
        return task;
    }

    private AddressSearchMaintenanceProgressListener buildProgressListener(Long taskId) {
        return new AddressSearchMaintenanceProgressListener() {
            @Override
            public void onTotalResolved(long totalCount) {
                updateTaskProgress(taskId, "PRECHECK", null, totalCount, 0L);
            }

            @Override
            public void onPhysicalIndexPrepared(String physicalIndexName) {
                updateTaskProgress(taskId, "CREATE_INDEX", physicalIndexName, null, null);
            }

            @Override
            public void onBatchCompleted(long processedCount) {
                updateTaskProgress(taskId, "BULK_INDEX", null, null, processedCount);
            }

            @Override
            public void onAliasSwitched() {
                updateTaskProgress(taskId, "SWITCH_ALIAS", null, null, null);
            }
        };
    }

    private void markTaskRunning(Long taskId) {
        AddressSearchMaintenanceTask updated = new AddressSearchMaintenanceTask();
        updated.setId(taskId);
        updated.setStatus(STATUS_RUNNING);
        updated.setCurrentPhase("PRECHECK");
        updated.setStartedTime(new Date());
        updated.setUpdatedTime(new Date());
        taskMapper.updateById(updated);
    }

    private void updateTaskProgress(Long taskId, String phase, String physicalIndexName, Long totalCount, Long processedCount) {
        AddressSearchMaintenanceTask current = taskMapper.selectById(taskId);
        long safeTotal = totalCount == null ? resolveLong(current == null ? null : current.getTotalCount()) : totalCount;
        long safeProcessed = processedCount == null ? resolveLong(current == null ? null : current.getProcessedCount()) : processedCount;
        int progressPercent = safeTotal <= 0 ? 0 : (int) Math.min(99, safeProcessed * 100 / safeTotal);
        AddressSearchMaintenanceTask updated = new AddressSearchMaintenanceTask();
        updated.setId(taskId);
        updated.setCurrentPhase(phase);
        updated.setPhysicalIndexName(physicalIndexName == null && current != null ? current.getPhysicalIndexName() : physicalIndexName);
        updated.setTotalCount(safeTotal);
        updated.setProcessedCount(safeProcessed);
        updated.setProgressPercent(progressPercent);
        updated.setUpdatedTime(new Date());
        taskMapper.updateById(updated);
    }

    private void markTaskSuccess(Long taskId) {
        AddressSearchMaintenanceTask updated = new AddressSearchMaintenanceTask();
        updated.setId(taskId);
        updated.setStatus(STATUS_SUCCESS);
        updated.setCurrentPhase("FINISH");
        updated.setProgressPercent(100);
        updated.setFinishedTime(new Date());
        updated.setUpdatedTime(new Date());
        taskMapper.updateById(updated);
    }

    private void markTaskFailed(Long taskId, String errorMessage) {
        AddressSearchMaintenanceTask updated = new AddressSearchMaintenanceTask();
        updated.setId(taskId);
        updated.setStatus(STATUS_FAILED);
        updated.setErrorMessage(StringUtils.substring(StringUtils.defaultString(errorMessage, "未知异常"), 0, 1000));
        updated.setFinishedTime(new Date());
        updated.setUpdatedTime(new Date());
        taskMapper.updateById(updated);
    }

    private long countRepairTasksByStatus(String status) {
        Long count = repairTaskMapper.selectCount(Wrappers.<AddressSearchRepairTask>lambdaQuery()
            .eq(AddressSearchRepairTask::getStatus, status));
        return resolveLong(count);
    }

    private AddressSearchMaintenanceVo.RunningTaskVo findRunningTask() {
        Page<AddressSearchMaintenanceTask> page = taskMapper.selectPage(new Page<>(1, 1, false),
            Wrappers.<AddressSearchMaintenanceTask>lambdaQuery()
                .eq(AddressSearchMaintenanceTask::getStatus, STATUS_RUNNING)
                .orderByDesc(AddressSearchMaintenanceTask::getUpdatedTime));
        List<AddressSearchMaintenanceTask> records = page == null ? null : page.getRecords();
        if (records == null || records.isEmpty()) {
            return null;
        }
        AddressSearchMaintenanceTask task = records.get(0);
        AddressSearchMaintenanceVo.RunningTaskVo vo = new AddressSearchMaintenanceVo.RunningTaskVo();
        vo.setId(task.getId());
        vo.setTaskType(task.getTaskType());
        vo.setStatus(task.getStatus());
        vo.setProgressPercent(task.getProgressPercent());
        return vo;
    }

    private AddressSearchMaintenanceVo.IndexSummaryVo toIndexSummary(AddressSearchIndexRuntimeInfo runtimeInfo) {
        if (runtimeInfo == null) {
            return null;
        }
        AddressSearchMaintenanceVo.IndexSummaryVo vo = new AddressSearchMaintenanceVo.IndexSummaryVo();
        vo.setAlias(runtimeInfo.alias());
        vo.setPhysicalIndexName(runtimeInfo.physicalIndexName());
        vo.setDocCount(runtimeInfo.documentCount());
        return vo;
    }

    private AddressSearchMaintenanceVo.TaskVo toTaskVo(AddressSearchMaintenanceTask task) {
        AddressSearchMaintenanceVo.TaskVo vo = new AddressSearchMaintenanceVo.TaskVo();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }

    private AddressSearchMaintenanceVo.RepairTaskVo toRepairTaskVo(AddressSearchRepairTask task) {
        AddressSearchMaintenanceVo.RepairTaskVo vo = new AddressSearchMaintenanceVo.RepairTaskVo();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }

    private long resolveLong(Long value) {
        return value == null ? 0L : value;
    }
}
