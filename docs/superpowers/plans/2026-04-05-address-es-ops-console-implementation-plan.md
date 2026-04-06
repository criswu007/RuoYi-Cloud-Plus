# 地址 ES 运维台 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `ruoyi-address` 与 `ruoyi-address-ui` 中落地 ES 运维单页驾驶舱、全量重建任务化、Redis 串行门禁和本地 standalone 联调配置。

**Architecture:** 保留现有 `AddressSearchMaintenanceService` 作为低层重建/repair 执行器，在其上增加运维任务编排服务、任务表和概览聚合能力。前端新增单页 `ES 运维` 页面，通过新接口展示 ES 运行态、重建任务与 repair 列表，并通过口令确认触发受控写操作。

**Tech Stack:** Spring Boot, MyBatis-Plus, Easy-ES, Lock4j/LockTemplate, Redis/Redisson, Vue2, vue-router, Element UI, Vite, Vitest, JUnit 5, Mockito

---

## 文件结构

- 后端任务模型与配置
  - Modify: `ruoyi-modules/ruoyi-address/sql/address/address_search_support.sql`
  - Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/config/AddressSearchProperties.java`
  - Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/AddressSearchMaintenanceTask.java`
  - Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/AddressSearchMaintenanceBo.java`
  - Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/AddressSearchMaintenanceVo.java`
  - Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddressSearchMaintenanceTaskMapper.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchPropertiesTest.java`
- 后端任务编排与执行进度
  - Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/support/AddressSearchMaintenanceProgressListener.java`
  - Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchMaintenanceTaskService.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchMaintenanceService.java`
  - Create: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchMaintenanceTaskServiceTest.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchMaintenanceServiceTest.java`
- 后端概览与接口
  - Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/model/AddressSearchIndexRuntimeInfo.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchGateway.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/InstallationAddressSearchGateway.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsInstallationAddressSearchGateway.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/AddressSearchMaintenanceController.java`
  - Create: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/AddressSearchMaintenanceControllerTest.java`
- 前端 API、路由与导航
  - Modify: `ruoyi-address-ui/src/api/address.js`
  - Modify: `ruoyi-address-ui/src/api/address.test.js`
  - Modify: `ruoyi-address-ui/src/router/index.js`
  - Modify: `ruoyi-address-ui/src/router/index.test.js`
  - Modify: `ruoyi-address-ui/src/App.vue`
  - Create: `ruoyi-address-ui/src/App.test.js`
- 前端 ES 运维页
  - Create: `ruoyi-address-ui/src/views/SearchOpsConsole.vue`
  - Create: `ruoyi-address-ui/src/views/SearchOpsConsole.test.js`
- standalone 联调配置与 smoke
  - Modify: `ruoyi-modules/ruoyi-address/src/main/resources/application-standalone.yml`
  - Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/StandaloneProfileSmokeTest.java`

### Task 1: 定义运维任务模型与配置扩展

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/sql/address/address_search_support.sql`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/config/AddressSearchProperties.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/AddressSearchMaintenanceTask.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/AddressSearchMaintenanceBo.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/AddressSearchMaintenanceVo.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddressSearchMaintenanceTaskMapper.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchPropertiesTest.java`

- [ ] **Step 1: 先写配置默认值失败测试**

```java
@Tag("dev")
public class AddressSearchPropertiesTest {

    @Test
    void shouldUseTaskOneDefaultSearchConfiguration() {
        AddressSearchProperties properties = new AddressSearchProperties();

        assertEquals("wait_for", properties.getRefreshPolicy());
        assertEquals("address_standard_search", properties.getStandard().getAlias());
        assertFalse(properties.getStandard().getReadEnabled());
        assertFalse(properties.getStandard().getWriteEnabled());
        assertEquals("address_installation_search", properties.getInstallation().getAlias());
        assertFalse(properties.getInstallation().getReadEnabled());
        assertFalse(properties.getInstallation().getWriteEnabled());
        assertEquals("http://127.0.0.1:5601", properties.getKibanaUrl());
        assertEquals("address:search:maintenance:running", properties.getMaintenanceLockKey());
    }
}
```

- [ ] **Step 2: 运行测试，确认新字段尚未实现**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=AddressSearchPropertiesTest test`

Expected: FAIL，提示 `getKibanaUrl` 或 `getMaintenanceLockKey` 不存在。

- [ ] **Step 3: 实现最小配置、DDL、实体与 BO/VO 骨架**

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/config/AddressSearchProperties.java
@Data
@Component
@ConfigurationProperties(prefix = "address.search")
public class AddressSearchProperties {

    private Boolean enabled = Boolean.FALSE;
    private String refreshPolicy = "wait_for";
    private Integer rebuildBatchSize = 500;
    private String pitKeepAlive = "1m";
    private String kibanaUrl = "http://127.0.0.1:5601";
    private String maintenanceLockKey = "address:search:maintenance:running";
    private IndexProperties standard = new IndexProperties();
    private IndexProperties installation = new IndexProperties();

    @Data
    public static class IndexProperties {
        private String alias;
        private Boolean readEnabled = Boolean.FALSE;
        private Boolean writeEnabled = Boolean.FALSE;
    }

    public AddressSearchProperties() {
        this.standard.setAlias("address_standard_search");
        this.installation.setAlias("address_installation_search");
    }
}
```

```sql
-- ruoyi-modules/ruoyi-address/sql/address/address_search_support.sql
create table if not exists address_search_maintenance_task (
    id bigint primary key,
    task_type varchar(32) not null,
    target_alias varchar(128) not null,
    physical_index_name varchar(128),
    status varchar(32) not null,
    current_phase varchar(32) not null,
    total_count bigint not null default 0,
    processed_count bigint not null default 0,
    progress_percent int not null default 0,
    error_message varchar(1000),
    trigger_by varchar(64),
    started_time datetime,
    finished_time datetime,
    created_time datetime not null,
    updated_time datetime not null
);
```

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/AddressSearchMaintenanceTask.java
@Data
@TableName("address_search_maintenance_task")
public class AddressSearchMaintenanceTask implements Serializable {

    @TableId(value = "id")
    private Long id;
    private String taskType;
    private String targetAlias;
    private String physicalIndexName;
    private String status;
    private String currentPhase;
    private Long totalCount;
    private Long processedCount;
    private Integer progressPercent;
    private String errorMessage;
    private String triggerBy;
    private Date startedTime;
    private Date finishedTime;
    private Date createdTime;
    private Date updatedTime;
}
```

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/AddressSearchMaintenanceBo.java
public final class AddressSearchMaintenanceBo {

    private AddressSearchMaintenanceBo() {
    }

    @Data
    public static class RebuildTaskSubmitBo {
        @NotBlank(message = "confirmationCode不能为空")
        private String confirmationCode;
    }

    @Data
    public static class RepairExecuteBo {
        @NotBlank(message = "confirmationCode不能为空")
        private String confirmationCode;
    }

    @Data
    public static class TaskQueryBo {
        private String taskType;
        private String status;
    }

    @Data
    public static class RepairTaskQueryBo {
        private String entityType;
        private String status;
    }
}
```

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/AddressSearchMaintenanceVo.java
public final class AddressSearchMaintenanceVo {

    private AddressSearchMaintenanceVo() {
    }

    @Data
    public static class OverviewVo {
        private Boolean esReachable;
        private String clusterName;
        private String clusterStatus;
        private String esVersion;
        private IndexSummaryVo standardIndex;
        private IndexSummaryVo installationIndex;
        private Long pendingRepairCount;
        private Long failedRepairCount;
        private RunningTaskVo runningTask;
        private String kibanaUrl;
    }

    @Data
    public static class IndexSummaryVo {
        private String alias;
        private String physicalIndexName;
        private Long docCount;
    }

    @Data
    public static class RunningTaskVo {
        private Long id;
        private String taskType;
        private String status;
        private Integer progressPercent;
    }

    @Data
    public static class TaskVo {
        private Long id;
        private String taskType;
        private String targetAlias;
        private String physicalIndexName;
        private String status;
        private String currentPhase;
        private Long totalCount;
        private Long processedCount;
        private Integer progressPercent;
        private String errorMessage;
        private String triggerBy;
        private Date startedTime;
        private Date finishedTime;
        private Date createdTime;
    }

    @Data
    public static class RepairTaskVo {
        private Long id;
        private String entityType;
        private String entityId;
        private String repairAction;
        private String status;
        private Integer retryCount;
        private Date updatedTime;
    }
}
```

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddressSearchMaintenanceTaskMapper.java
public interface AddressSearchMaintenanceTaskMapper extends BaseMapper<AddressSearchMaintenanceTask> {
}
```

- [ ] **Step 4: 重新运行配置测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=AddressSearchPropertiesTest test`

Expected: PASS，`AddressSearchPropertiesTest` 通过。

- [ ] **Step 5: 提交任务 1**

```bash
git add \
  ruoyi-modules/ruoyi-address/sql/address/address_search_support.sql \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/config/AddressSearchProperties.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/AddressSearchMaintenanceTask.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/AddressSearchMaintenanceBo.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/AddressSearchMaintenanceVo.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddressSearchMaintenanceTaskMapper.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchPropertiesTest.java
git commit -m "feat: add address search maintenance task model"
```

### Task 2: 实现重建任务化与执行进度回写

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/support/AddressSearchMaintenanceProgressListener.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchMaintenanceTaskService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchMaintenanceService.java`
- Create: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchMaintenanceTaskServiceTest.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchMaintenanceServiceTest.java`

- [ ] **Step 1: 先写任务提交与锁冲突失败测试**

```java
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class AddressSearchMaintenanceTaskServiceTest {

    @Mock
    private AddressSearchMaintenanceTaskMapper taskMapper;

    @Mock
    private AddressSearchRepairTaskMapper repairTaskMapper;

    @Mock
    private AddressSearchMaintenanceService maintenanceService;

    @Mock
    private StandardAddressSearchGateway standardAddressSearchGateway;

    @Mock
    private InstallationAddressSearchGateway installationAddressSearchGateway;

    @Mock
    private LockTemplate lockTemplate;

    @Mock
    private TaskExecutor taskExecutor;

    private AddressSearchMaintenanceTaskService taskService;

    @BeforeEach
    void setUp() {
        AddressSearchProperties properties = new AddressSearchProperties();
        taskService = new AddressSearchMaintenanceTaskService(
            taskMapper,
            repairTaskMapper,
            maintenanceService,
            standardAddressSearchGateway,
            installationAddressSearchGateway,
            properties,
            lockTemplate,
            taskExecutor
        );
    }

    @Test
    void shouldCreatePendingStandardTaskAndDispatchAsyncRunner() {
        LockInfo lockInfo = org.mockito.Mockito.mock(LockInfo.class);
        when(lockTemplate.lock(
            "address:search:maintenance:running",
            30000L,
            3000L,
            RedissonLockExecutor.class
        )).thenReturn(lockInfo);
        doAnswer(invocation -> {
            AddressSearchMaintenanceTask task = invocation.getArgument(0);
            task.setId(9001L);
            return 1;
        }).when(taskMapper).insert(any(AddressSearchMaintenanceTask.class));

        Long taskId = taskService.submitStandardRebuild("REBUILD_STANDARD");

        assertEquals(9001L, taskId);
        verify(taskExecutor).execute(any(Runnable.class));
    }

    @Test
    void shouldRejectWhenWriteGuardIsBusy() {
        when(lockTemplate.lock(
            "address:search:maintenance:running",
            30000L,
            3000L,
            RedissonLockExecutor.class
        )).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> taskService.submitInstallationRebuild("REBUILD_INSTALLATION"));

        assertEquals("当前已有 ES 运维写任务执行中，请稍后再试", ex.getMessage());
    }
}
```

- [ ] **Step 2: 运行测试，确认任务编排服务尚不存在**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=AddressSearchMaintenanceTaskServiceTest test`

Expected: FAIL，提示 `AddressSearchMaintenanceTaskService` 或相关方法不存在。

- [ ] **Step 3: 实现任务编排服务、进度监听器与重建回调**

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/support/AddressSearchMaintenanceProgressListener.java
public interface AddressSearchMaintenanceProgressListener {

    AddressSearchMaintenanceProgressListener NO_OP = new AddressSearchMaintenanceProgressListener() {
    };

    default void onTotalResolved(long totalCount) {
    }

    default void onPhysicalIndexPrepared(String physicalIndexName) {
    }

    default void onBatchCompleted(long processedCount) {
    }

    default void onAliasSwitched() {
    }
}
```

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchMaintenanceTaskService.java
@Service
@RequiredArgsConstructor
public class AddressSearchMaintenanceTaskService {

    private static final String TASK_TYPE_REBUILD_STANDARD = "REBUILD_STANDARD";
    private static final String TASK_TYPE_REBUILD_INSTALLATION = "REBUILD_INSTALLATION";
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

    public Long submitStandardRebuild(String confirmationCode) {
        validateConfirmationCode(confirmationCode, TASK_TYPE_REBUILD_STANDARD);
        LockInfo lockInfo = acquireWriteGuard();
        AddressSearchMaintenanceTask task = createPendingTask(TASK_TYPE_REBUILD_STANDARD, properties.getStandard().getAlias());
        taskExecutor.execute(() -> executeStandardTask(task.getId(), lockInfo));
        return task.getId();
    }

    public Long submitInstallationRebuild(String confirmationCode) {
        validateConfirmationCode(confirmationCode, TASK_TYPE_REBUILD_INSTALLATION);
        LockInfo lockInfo = acquireWriteGuard();
        AddressSearchMaintenanceTask task = createPendingTask(TASK_TYPE_REBUILD_INSTALLATION, properties.getInstallation().getAlias());
        taskExecutor.execute(() -> executeInstallationTask(task.getId(), lockInfo));
        return task.getId();
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
        if (!expected.equals(actual)) {
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
        long safeTotal = totalCount == null ? (current.getTotalCount() == null ? 0L : current.getTotalCount()) : totalCount;
        long safeProcessed = processedCount == null ? (current.getProcessedCount() == null ? 0L : current.getProcessedCount()) : processedCount;
        int progressPercent = safeTotal <= 0 ? 0 : (int) Math.min(99, safeProcessed * 100 / safeTotal);
        AddressSearchMaintenanceTask updated = new AddressSearchMaintenanceTask();
        updated.setId(taskId);
        updated.setCurrentPhase(phase);
        updated.setPhysicalIndexName(physicalIndexName == null ? current.getPhysicalIndexName() : physicalIndexName);
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
        updated.setErrorMessage(StringUtils.substring(errorMessage, 0, 1000));
        updated.setFinishedTime(new Date());
        updated.setUpdatedTime(new Date());
        taskMapper.updateById(updated);
    }
}
```

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchMaintenanceService.java
public void rebuildStandardIndex() {
    rebuildStandardIndex(AddressSearchMaintenanceProgressListener.NO_OP);
}

public void rebuildStandardIndex(AddressSearchMaintenanceProgressListener listener) {
    long totalCount = addrSegmMapper.selectCount(Wrappers.<AddrSegm>lambdaQuery()
        .eq(AddrSegm::getDeleteState, DELETE_STATE_ACTIVE));
    listener.onTotalResolved(totalCount);
    String rebuildIndex = standardAddressSearchGateway.prepareRebuildIndex();
    listener.onPhysicalIndexPrepared(rebuildIndex);
    long processedCount = 0L;
    String lastSegmId = null;
    while (true) {
        Page<AddrSegm> page = new Page<>(1, resolveRebuildBatchSize(), false);
        Page<AddrSegm> batchPage = addrSegmMapper.selectPage(page, Wrappers.<AddrSegm>lambdaQuery()
            .eq(AddrSegm::getDeleteState, DELETE_STATE_ACTIVE)
            .gt(StringUtils.isNotBlank(lastSegmId), AddrSegm::getSegmId, lastSegmId)
            .orderByAsc(AddrSegm::getSegmId));
        List<AddrSegm> records = batchPage == null || batchPage.getRecords() == null
            ? Collections.emptyList()
            : batchPage.getRecords();
        if (records.isEmpty()) {
            break;
        }
        requireSuccess(standardAddressSearchGateway.bulkIndex(rebuildIndex, records), "标准地址重建批量写入失败");
        processedCount += records.size();
        listener.onBatchCompleted(processedCount);
        lastSegmId = records.get(records.size() - 1).getSegmId();
    }
    standardAddressSearchGateway.switchAlias(rebuildIndex);
    listener.onAliasSwitched();
}
```

```java
// ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchMaintenanceServiceTest.java
@Test
void shouldPublishProgressDuringStandardRebuild() {
    AddressSearchMaintenanceProgressListener listener = org.mockito.Mockito.mock(AddressSearchMaintenanceProgressListener.class);
    when(standardAddressSearchGateway.prepareRebuildIndex()).thenReturn("address_standard_search_v2");
    Page<AddrSegm> page1 = new Page<>(1, 500, 1);
    page1.setRecords(List.of(buildAddrSegm("SEG001")));
    Page<AddrSegm> page2 = new Page<>(1, 500, 0);
    page2.setRecords(List.of());
    when(addrSegmMapper.selectCount(any())).thenReturn(1L);
    when(addrSegmMapper.selectPage(any(Page.class), any())).thenReturn(page1, page2);
    when(standardAddressSearchGateway.bulkIndex("address_standard_search_v2", page1.getRecords())).thenReturn(true);

    maintenanceService.rebuildStandardIndex(listener);

    verify(listener).onTotalResolved(1L);
    verify(listener).onPhysicalIndexPrepared("address_standard_search_v2");
    verify(listener).onBatchCompleted(1L);
    verify(listener).onAliasSwitched();
}
```

- [ ] **Step 4: 运行后端任务化测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=AddressSearchMaintenanceTaskServiceTest,AddressSearchMaintenanceServiceTest test`

Expected: PASS，任务提交、锁冲突和进度监听测试通过。

- [ ] **Step 5: 提交任务 2**

```bash
git add \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/support/AddressSearchMaintenanceProgressListener.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchMaintenanceTaskService.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchMaintenanceService.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchMaintenanceTaskServiceTest.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchMaintenanceServiceTest.java
git commit -m "feat: add async address search maintenance orchestration"
```

### Task 3: 实现概览聚合、repair 重放门禁与控制器接口

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/model/AddressSearchIndexRuntimeInfo.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/InstallationAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsInstallationAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchMaintenanceTaskService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/AddressSearchMaintenanceController.java`
- Create: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/AddressSearchMaintenanceControllerTest.java`

- [ ] **Step 1: 先写概览与控制器失败测试**

```java
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class AddressSearchMaintenanceControllerTest {

    @Mock
    private AddressSearchMaintenanceTaskService taskService;

    @InjectMocks
    private AddressSearchMaintenanceController controller;

    @Test
    void shouldSubmitStandardRebuildTask() {
        AddressSearchMaintenanceBo.RebuildTaskSubmitBo bo = new AddressSearchMaintenanceBo.RebuildTaskSubmitBo();
        bo.setConfirmationCode("REBUILD_STANDARD");
        when(taskService.submitStandardRebuild("REBUILD_STANDARD")).thenReturn(9001L);

        R<Long> response = controller.createStandardRebuildTask(bo);

        assertEquals(9001L, response.getData());
    }

    @Test
    void shouldExposeOpsOverview() {
        AddressSearchMaintenanceVo.OverviewVo overview = new AddressSearchMaintenanceVo.OverviewVo();
        overview.setEsReachable(Boolean.TRUE);
        overview.setPendingRepairCount(3L);
        when(taskService.getOverview()).thenReturn(overview);

        R<AddressSearchMaintenanceVo.OverviewVo> response = controller.getOverview();

        assertTrue(response.getData().getEsReachable());
        assertEquals(3L, response.getData().getPendingRepairCount());
    }
}
```

```java
@Test
void shouldBuildOverviewFromGatewayRuntimeInfo() {
    AddressSearchIndexRuntimeInfo standard = new AddressSearchIndexRuntimeInfo(
        "address_standard_search",
        "address_standard_search_v2",
        2318018L
    );
    AddressSearchIndexRuntimeInfo installation = new AddressSearchIndexRuntimeInfo(
        "address_installation_search",
        "address_installation_search_v2",
        100000L
    );
    when(standardAddressSearchGateway.ping()).thenReturn(true);
    when(standardAddressSearchGateway.getRuntimeInfo()).thenReturn(standard);
    when(installationAddressSearchGateway.getRuntimeInfo()).thenReturn(installation);
    when(repairTaskMapper.selectCount(any())).thenReturn(12L, 2L);

    AddressSearchMaintenanceVo.OverviewVo overview = taskService.getOverview();

    assertTrue(overview.getEsReachable());
    assertEquals("address_standard_search_v2", overview.getStandardIndex().getPhysicalIndexName());
    assertEquals(12L, overview.getPendingRepairCount());
    assertEquals(2L, overview.getFailedRepairCount());
}
```

- [ ] **Step 2: 运行测试，确认概览与接口尚未就位**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=AddressSearchMaintenanceTaskServiceTest,AddressSearchMaintenanceControllerTest test`

Expected: FAIL，提示 `getOverview`、`createStandardRebuildTask` 或网关运行态方法不存在。

- [ ] **Step 3: 实现网关运行态、repair 重放门禁与新接口**

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/model/AddressSearchIndexRuntimeInfo.java
public record AddressSearchIndexRuntimeInfo(
    String alias,
    String physicalIndexName,
    long documentCount
) {
}
```

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchGateway.java
boolean ping();

AddressSearchIndexRuntimeInfo getRuntimeInfo();
```

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/InstallationAddressSearchGateway.java
boolean ping();

AddressSearchIndexRuntimeInfo getRuntimeInfo();
```

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchMaintenanceTaskService.java
public AddressSearchMaintenanceVo.OverviewVo getOverview() {
    AddressSearchMaintenanceVo.OverviewVo overview = new AddressSearchMaintenanceVo.OverviewVo();
    overview.setEsReachable(standardAddressSearchGateway.ping());
    overview.setStandardIndex(toIndexSummary(standardAddressSearchGateway.getRuntimeInfo()));
    overview.setInstallationIndex(toIndexSummary(installationAddressSearchGateway.getRuntimeInfo()));
    overview.setPendingRepairCount(countRepairTasksByStatus("PENDING"));
    overview.setFailedRepairCount(countRepairTasksByStatus("FAILED"));
    overview.setRunningTask(findRunningTask());
    overview.setKibanaUrl(properties.getKibanaUrl());
    return overview;
}

public AddressSearchMaintenanceVo.TaskVo getTask(Long taskId) {
    return toTaskVo(taskMapper.selectById(taskId));
}

public void executeRepairTask(Long taskId, String confirmationCode) {
    validateConfirmationCode(confirmationCode, "REPLAY_REPAIR");
    LockInfo lockInfo = acquireWriteGuard();
    try {
        maintenanceService.executeRepairTask(taskId);
    } finally {
        lockTemplate.releaseLock(lockInfo);
    }
}

public TableDataInfo<AddressSearchMaintenanceVo.TaskVo> listTasks(AddressSearchMaintenanceBo.TaskQueryBo bo, PageQuery pageQuery) {
    Page<AddressSearchMaintenanceTask> page = taskMapper.selectPage(pageQuery.build(),
        Wrappers.<AddressSearchMaintenanceTask>lambdaQuery()
            .eq(StringUtils.isNotBlank(bo.getTaskType()), AddressSearchMaintenanceTask::getTaskType, bo.getTaskType())
            .eq(StringUtils.isNotBlank(bo.getStatus()), AddressSearchMaintenanceTask::getStatus, bo.getStatus())
            .orderByDesc(AddressSearchMaintenanceTask::getCreatedTime));
    return TableDataInfo.build(page.convert(this::toTaskVo));
}

public TableDataInfo<AddressSearchMaintenanceVo.RepairTaskVo> listRepairTasks(AddressSearchMaintenanceBo.RepairTaskQueryBo bo, PageQuery pageQuery) {
    Page<AddressSearchRepairTask> page = repairTaskMapper.selectPage(pageQuery.build(),
        Wrappers.<AddressSearchRepairTask>lambdaQuery()
            .eq(StringUtils.isNotBlank(bo.getEntityType()), AddressSearchRepairTask::getEntityType, bo.getEntityType())
            .eq(StringUtils.isNotBlank(bo.getStatus()), AddressSearchRepairTask::getStatus, bo.getStatus())
            .orderByDesc(AddressSearchRepairTask::getUpdatedTime));
    return TableDataInfo.build(page.convert(this::toRepairTaskVo));
}

private long countRepairTasksByStatus(String status) {
    return repairTaskMapper.selectCount(Wrappers.<AddressSearchRepairTask>lambdaQuery()
        .eq(AddressSearchRepairTask::getStatus, status));
}

private AddressSearchMaintenanceVo.RunningTaskVo findRunningTask() {
    AddressSearchMaintenanceTask task = taskMapper.selectOne(Wrappers.<AddressSearchMaintenanceTask>lambdaQuery()
        .eq(AddressSearchMaintenanceTask::getStatus, "RUNNING")
        .orderByDesc(AddressSearchMaintenanceTask::getUpdatedTime)
        .last("limit 1"));
    if (task == null) {
        return null;
    }
    AddressSearchMaintenanceVo.RunningTaskVo vo = new AddressSearchMaintenanceVo.RunningTaskVo();
    vo.setId(task.getId());
    vo.setTaskType(task.getTaskType());
    vo.setStatus(task.getStatus());
    vo.setProgressPercent(task.getProgressPercent());
    return vo;
}

private AddressSearchMaintenanceVo.IndexSummaryVo toIndexSummary(AddressSearchIndexRuntimeInfo runtimeInfo) {
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
```

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java
public boolean ping() {
    try {
        return Boolean.TRUE.equals(elasticsearchClient.ping().value());
    } catch (IOException ex) {
        return false;
    }
}

public AddressSearchIndexRuntimeInfo getRuntimeInfo() {
    String alias = addressSearchProperties.getStandard().getAlias();
    String physicalIndexName = resolveCurrentPhysicalIndex(alias);
    long documentCount = countDocuments(alias);
    return new AddressSearchIndexRuntimeInfo(alias, physicalIndexName, documentCount);
}

private String resolveCurrentPhysicalIndex(String alias) {
    try {
        return elasticsearchClient.indices()
            .getAlias(request -> request.name(alias))
            .result()
            .keySet()
            .stream()
            .findFirst()
            .orElse(alias);
    } catch (IOException ex) {
        throw new IllegalStateException("查询标准地址 alias 指向失败", ex);
    }
}

private long countDocuments(String alias) {
    try {
        return elasticsearchClient.count(request -> request.index(alias)).count();
    } catch (IOException ex) {
        throw new IllegalStateException("统计标准地址索引文档数失败", ex);
    }
}
```

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsInstallationAddressSearchGateway.java
public boolean ping() {
    try {
        return Boolean.TRUE.equals(elasticsearchClient.ping().value());
    } catch (IOException ex) {
        return false;
    }
}

public AddressSearchIndexRuntimeInfo getRuntimeInfo() {
    String alias = addressSearchProperties.getInstallation().getAlias();
    String physicalIndexName = resolveCurrentPhysicalIndex(alias);
    long documentCount = countDocuments(alias);
    return new AddressSearchIndexRuntimeInfo(alias, physicalIndexName, documentCount);
}

private String resolveCurrentPhysicalIndex(String alias) {
    try {
        return elasticsearchClient.indices()
            .getAlias(request -> request.name(alias))
            .result()
            .keySet()
            .stream()
            .findFirst()
            .orElse(alias);
    } catch (IOException ex) {
        throw new IllegalStateException("查询安装地址 alias 指向失败", ex);
    }
}

private long countDocuments(String alias) {
    try {
        return elasticsearchClient.count(request -> request.index(alias)).count();
    } catch (IOException ex) {
        throw new IllegalStateException("统计安装地址索引文档数失败", ex);
    }
}
```

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/AddressSearchMaintenanceController.java
@SaCheckPermission("address:search:maintain")
@GetMapping("/ops/overview")
public R<AddressSearchMaintenanceVo.OverviewVo> getOverview() {
    return R.ok(addressSearchMaintenanceTaskService.getOverview());
}

@SaCheckPermission("address:search:maintain")
@PostMapping("/tasks/rebuild/standard")
public R<Long> createStandardRebuildTask(@RequestBody @Validated AddressSearchMaintenanceBo.RebuildTaskSubmitBo bo) {
    return R.ok(addressSearchMaintenanceTaskService.submitStandardRebuild(bo.getConfirmationCode()));
}

@SaCheckPermission("address:search:maintain")
@PostMapping("/tasks/rebuild/installation")
public R<Long> createInstallationRebuildTask(@RequestBody @Validated AddressSearchMaintenanceBo.RebuildTaskSubmitBo bo) {
    return R.ok(addressSearchMaintenanceTaskService.submitInstallationRebuild(bo.getConfirmationCode()));
}

@SaCheckPermission("address:search:maintain")
@GetMapping("/tasks")
public TableDataInfo<AddressSearchMaintenanceVo.TaskVo> listTasks(AddressSearchMaintenanceBo.TaskQueryBo bo, PageQuery pageQuery) {
    return addressSearchMaintenanceTaskService.listTasks(bo, pageQuery);
}

@SaCheckPermission("address:search:maintain")
@GetMapping("/tasks/{taskId}")
public R<AddressSearchMaintenanceVo.TaskVo> getTask(@PathVariable Long taskId) {
    return R.ok(addressSearchMaintenanceTaskService.getTask(taskId));
}

@SaCheckPermission("address:search:maintain")
@GetMapping("/repair/tasks")
public TableDataInfo<AddressSearchMaintenanceVo.RepairTaskVo> listRepairTasks(AddressSearchMaintenanceBo.RepairTaskQueryBo bo, PageQuery pageQuery) {
    return addressSearchMaintenanceTaskService.listRepairTasks(bo, pageQuery);
}

@SaCheckPermission("address:search:maintain")
@PostMapping("/repair/{taskId}/execute")
public R<Void> executeRepairTask(@PathVariable Long taskId,
                                 @RequestBody @Validated AddressSearchMaintenanceBo.RepairExecuteBo bo) {
    addressSearchMaintenanceTaskService.executeRepairTask(taskId, bo.getConfirmationCode());
    return R.ok();
}
```

- [ ] **Step 4: 运行概览与控制器测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=AddressSearchMaintenanceTaskServiceTest,AddressSearchMaintenanceControllerTest test`

Expected: PASS，概览聚合、repair 门禁和控制器新接口测试通过。

- [ ] **Step 5: 提交任务 3**

```bash
git add \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/model/AddressSearchIndexRuntimeInfo.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/InstallationAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsInstallationAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchMaintenanceTaskService.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/AddressSearchMaintenanceController.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/AddressSearchMaintenanceControllerTest.java
git commit -m "feat: expose address search ops maintenance apis"
```

### Task 4: 接入前端 API、路由与导航

**Files:**
- Modify: `ruoyi-address-ui/src/api/address.js`
- Modify: `ruoyi-address-ui/src/api/address.test.js`
- Modify: `ruoyi-address-ui/src/router/index.js`
- Modify: `ruoyi-address-ui/src/router/index.test.js`
- Modify: `ruoyi-address-ui/src/App.vue`
- Create: `ruoyi-address-ui/src/App.test.js`

- [ ] **Step 1: 先写前端 API 与路由失败测试**

```javascript
// ruoyi-address-ui/src/api/address.test.js
it('ES 运维接口应暴露概览、重建任务与 repair 契约', () => {
  addressApi.getSearchOpsOverview();
  addressApi.createStandardRebuildTask({ confirmationCode: 'REBUILD_STANDARD' });
  addressApi.createInstallationRebuildTask({ confirmationCode: 'REBUILD_INSTALLATION' });
  addressApi.getSearchMaintenanceTasks({ status: 'RUNNING', pageNum: 1, pageSize: 10 });
  addressApi.getSearchMaintenanceTaskDetail(9001);
  addressApi.getSearchRepairTasks({ status: 'FAILED', pageNum: 1, pageSize: 10 });
  addressApi.executeSearchRepairTask(88, { confirmationCode: 'REPLAY_REPAIR' });

  expect(mockRequest.get).toHaveBeenNthCalledWith(1, '/address/search/ops/overview');
  expect(mockRequest.post).toHaveBeenNthCalledWith(1, '/address/search/tasks/rebuild/standard', { confirmationCode: 'REBUILD_STANDARD' });
  expect(mockRequest.post).toHaveBeenNthCalledWith(2, '/address/search/tasks/rebuild/installation', { confirmationCode: 'REBUILD_INSTALLATION' });
  expect(mockRequest.get).toHaveBeenNthCalledWith(2, '/address/search/tasks', { params: { status: 'RUNNING', pageNum: 1, pageSize: 10 } });
  expect(mockRequest.get).toHaveBeenNthCalledWith(3, '/address/search/tasks/9001');
  expect(mockRequest.get).toHaveBeenNthCalledWith(4, '/address/search/repair/tasks', { params: { status: 'FAILED', pageNum: 1, pageSize: 10 } });
  expect(mockRequest.post).toHaveBeenNthCalledWith(3, '/address/search/repair/88/execute', { confirmationCode: 'REPLAY_REPAIR' });
});
```

```javascript
// ruoyi-address-ui/src/router/index.test.js
it('应提供 ES 运维页路由', () => {
  const route = router.options.routes.find(item => item.path === '/ops/search');

  expect(route).toBeTruthy();
});
```

```javascript
// ruoyi-address-ui/src/App.test.js
import { describe, expect, it } from 'vitest';
import App from './App.vue';

describe('应用导航', () => {
  it('应提供 ES 运维页标题映射', () => {
    const state = App.data();

    expect(state.titleMap['/ops/search']).toBe('ES 运维');
  });
});
```

- [ ] **Step 2: 运行前端失败测试**

Run: `npm --prefix ruoyi-address-ui run test -- src/api/address.test.js src/router/index.test.js src/App.test.js`

Expected: FAIL，提示 `getSearchOpsOverview`、`/ops/search` 或 `titleMap['/ops/search']` 不存在。

- [ ] **Step 3: 实现前端 API、路由与导航入口**

```javascript
// ruoyi-address-ui/src/api/address.js
export const getSearchOpsOverview = () =>
  request.get('/address/search/ops/overview');

export const createStandardRebuildTask = data =>
  request.post('/address/search/tasks/rebuild/standard', data);

export const createInstallationRebuildTask = data =>
  request.post('/address/search/tasks/rebuild/installation', data);

export const getSearchMaintenanceTasks = params =>
  request.get('/address/search/tasks', { params });

export const getSearchMaintenanceTaskDetail = taskId =>
  request.get(`/address/search/tasks/${taskId}`);

export const getSearchRepairTasks = params =>
  request.get('/address/search/repair/tasks', { params });

export const executeSearchRepairTask = (taskId, data) =>
  request.post(`/address/search/repair/${taskId}/execute`, data);
```

```javascript
// ruoyi-address-ui/src/router/index.js
import SearchOpsConsole from '../views/SearchOpsConsole.vue';

export default new Router({
  mode: 'history',
  routes: [
    { path: '/', redirect: '/standard/list' },
    { path: '/standard/list', component: StandardList },
    { path: '/standard/detail/:segmId', component: StandardDetail },
    { path: '/standard/merge', component: StandardMerge },
    { path: '/standard/split', component: StandardSplit },
    { path: '/import/records', component: ImportRecords },
    { path: '/operation/logs', component: OperationLogs },
    { path: '/installation/list', component: InstallationList },
    { path: '/selection/tools', component: SelectionTools },
    { path: '/standard/labels', component: AddressLabels },
    { path: '/management/station', component: ManagementStation },
    { path: '/monitor/records', component: MonitorRecords },
    { path: '/monitor/rules', component: MonitorRules },
    { path: '/monitor/task', component: MonitorTask },
    { path: '/ops/search', component: SearchOpsConsole }
  ]
});
```

```vue
<!-- ruoyi-address-ui/src/App.vue -->
<el-submenu index="ops">
  <template slot="title">运维支持</template>
  <el-menu-item index="/ops/search">ES 运维</el-menu-item>
</el-submenu>
```

```javascript
// ruoyi-address-ui/src/App.vue script data
titleMap: {
  '/standard/list': '标准地址列表',
  '/standard/detail': '标准地址详情',
  '/standard/merge': '标准地址合并',
  '/standard/split': '标准地址拆分',
  '/standard/labels': '标签库管理',
  '/import/records': '导入记录查询',
  '/operation/logs': '地址操作日志',
  '/installation/list': '安装地址列表',
  '/selection/tools': '选址平台',
  '/management/station': '管理站管理',
  '/monitor/records': '异常地址治理',
  '/monitor/rules': '非标监控规则',
  '/monitor/task': '监控任务摘要',
  '/ops/search': 'ES 运维'
}
```

- [ ] **Step 4: 重新运行前端 API 与路由测试**

Run: `npm --prefix ruoyi-address-ui run test -- src/api/address.test.js src/router/index.test.js src/App.test.js`

Expected: PASS，前端 API 契约、路由和标题映射测试通过。

- [ ] **Step 5: 提交任务 4**

```bash
git add \
  ruoyi-address-ui/src/api/address.js \
  ruoyi-address-ui/src/api/address.test.js \
  ruoyi-address-ui/src/router/index.js \
  ruoyi-address-ui/src/router/index.test.js \
  ruoyi-address-ui/src/App.vue \
  ruoyi-address-ui/src/App.test.js
git commit -m "feat: add address search ops routes and api"
```

### Task 5: 实现 ES 运维页状态模型与交互

**Files:**
- Create: `ruoyi-address-ui/src/views/SearchOpsConsole.vue`
- Create: `ruoyi-address-ui/src/views/SearchOpsConsole.test.js`

- [ ] **Step 1: 先写页面状态模型失败测试**

```javascript
import { describe, expect, it, vi } from 'vitest';
import SearchOpsConsole from './SearchOpsConsole.vue';

describe('ES 运维页状态模型', () => {
  it('默认状态应维护概览、重建任务和 repair 查询模型', () => {
    const state = SearchOpsConsole.data();

    expect(state.overview).toEqual({});
    expect(state.taskQuery).toEqual({
      taskType: '',
      status: ''
    });
    expect(state.repairQuery).toEqual({
      entityType: '',
      status: ''
    });
    expect(state.standardConfirmationCode).toBe('');
    expect(state.installationConfirmationCode).toBe('');
    expect(state.repairConfirmationCode).toBe('');
  });

  it('buildTaskQueryParams 应输出 GET 查询兼容字段', () => {
    const params = SearchOpsConsole.methods.buildTaskQueryParams.call({
      taskQuery: {
        taskType: 'REBUILD_STANDARD',
        status: 'RUNNING'
      },
      taskPageNum: 2,
      taskPageSize: 10
    });

    expect(params).toEqual({
      taskType: 'REBUILD_STANDARD',
      status: 'RUNNING',
      pageNum: 2,
      pageSize: 10
    });
  });

  it('存在运行中任务时应启动 5 秒轮询', () => {
    const setIntervalSpy = vi.spyOn(global, 'setInterval').mockReturnValue(1);
    const ctx = {
      pollingTimer: null,
      refreshOverviewAndTasks: vi.fn()
    };

    SearchOpsConsole.methods.startPolling.call(ctx);

    expect(setIntervalSpy).toHaveBeenCalledWith(expect.any(Function), 5000);
    setIntervalSpy.mockRestore();
  });
});
```

- [ ] **Step 2: 运行页面失败测试**

Run: `npm --prefix ruoyi-address-ui run test -- src/views/SearchOpsConsole.test.js`

Expected: FAIL，提示 `SearchOpsConsole.vue` 不存在。

- [ ] **Step 3: 实现运维页模板、状态与轮询逻辑**

```vue
<template>
  <div class="search-ops-console">
    <el-row :gutter="16" class="summary-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card">
          <div class="summary-label">ES 连通状态</div>
          <div class="summary-value">{{ overview.esReachable ? '健康' : '异常' }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card">
          <div class="summary-label">标准地址索引</div>
          <div class="summary-value text-value">{{ standardPhysicalIndexName }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card">
          <div class="summary-label">安装地址索引</div>
          <div class="summary-value text-value">{{ installationPhysicalIndexName }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="summary-card warning">
          <div class="summary-label">待处理 repair</div>
          <div class="summary-value">{{ overview.pendingRepairCount || 0 }}</div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {
  getSearchOpsOverview,
  createStandardRebuildTask,
  createInstallationRebuildTask,
  getSearchMaintenanceTasks,
  getSearchRepairTasks,
  executeSearchRepairTask
} from '../api/address';

export default {
  data() {
    return {
      overview: {},
      taskList: [],
      repairList: [],
      taskLoading: false,
      repairLoading: false,
      actionLoading: false,
      pollingTimer: null,
      taskPageNum: 1,
      taskPageSize: 10,
      repairPageNum: 1,
      repairPageSize: 10,
      taskQuery: {
        taskType: '',
        status: ''
      },
      repairQuery: {
        entityType: '',
        status: ''
      },
      standardConfirmationCode: '',
      installationConfirmationCode: '',
      repairConfirmationCode: ''
    };
  },
  computed: {
    standardPhysicalIndexName() {
      return this.overview.standardIndex && this.overview.standardIndex.physicalIndexName
        ? this.overview.standardIndex.physicalIndexName
        : '-';
    },
    installationPhysicalIndexName() {
      return this.overview.installationIndex && this.overview.installationIndex.physicalIndexName
        ? this.overview.installationIndex.physicalIndexName
        : '-';
    }
  },
  mounted() {
    this.refreshOverviewAndTasks();
    this.refreshRepairTasks();
    this.startPolling();
  },
  beforeDestroy() {
    this.stopPolling();
  },
  methods: {
    buildTaskQueryParams() {
      return {
        ...this.taskQuery,
        pageNum: this.taskPageNum,
        pageSize: this.taskPageSize
      };
    },
    buildRepairQueryParams() {
      return {
        ...this.repairQuery,
        pageNum: this.repairPageNum,
        pageSize: this.repairPageSize
      };
    },
    startPolling() {
      this.stopPolling();
      this.pollingTimer = setInterval(() => {
        this.refreshOverviewAndTasks();
      }, 5000);
    },
    stopPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer);
        this.pollingTimer = null;
      }
    },
    async refreshOverviewAndTasks() {
      const [overviewRes, taskRes] = await Promise.all([
        getSearchOpsOverview(),
        getSearchMaintenanceTasks(this.buildTaskQueryParams())
      ]);
      this.overview = overviewRes.data || {};
      this.taskList = taskRes.rows || [];
    },
    async refreshRepairTasks() {
      const res = await getSearchRepairTasks(this.buildRepairQueryParams());
      this.repairList = res.rows || [];
    },
    async triggerStandardRebuild() {
      await createStandardRebuildTask({ confirmationCode: this.standardConfirmationCode });
      this.standardConfirmationCode = '';
      await this.refreshOverviewAndTasks();
    },
    async triggerInstallationRebuild() {
      await createInstallationRebuildTask({ confirmationCode: this.installationConfirmationCode });
      this.installationConfirmationCode = '';
      await this.refreshOverviewAndTasks();
    },
    async replayRepair(taskId) {
      await executeSearchRepairTask(taskId, { confirmationCode: this.repairConfirmationCode });
      this.repairConfirmationCode = '';
      await this.refreshRepairTasks();
    }
  }
};
</script>
```

- [ ] **Step 4: 运行页面测试并做一次前端构建**

Run: `npm --prefix ruoyi-address-ui run test -- src/views/SearchOpsConsole.test.js`

Expected: PASS，`SearchOpsConsole.test.js` 通过。

Run: `npm --prefix ruoyi-address-ui run build`

Expected: PASS，Vite 构建成功。

- [ ] **Step 5: 提交任务 5**

```bash
git add \
  ruoyi-address-ui/src/views/SearchOpsConsole.vue \
  ruoyi-address-ui/src/views/SearchOpsConsole.test.js
git commit -m "feat: add address search ops console"
```

### Task 6: 打开 standalone 联调配置并完成 smoke 验证

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/resources/application-standalone.yml`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/StandaloneProfileSmokeTest.java`

- [ ] **Step 1: 先写 standalone 绑定失败测试**

```java
@Tag("dev")
@ActiveProfiles("standalone")
@SpringBootTest(
    classes = RuoYiAddressApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class StandaloneProfileSmokeTest {

    @Autowired
    private AddressSearchProperties addressSearchProperties;

    @Autowired
    private RedisProperties redisProperties;

    @Value("${easy-es.address}")
    private String easyEsAddress;

    @Test
    void shouldExposeLocalRedisAndEsEndpointsUnderStandaloneProfile() {
        assertEquals("127.0.0.1", redisProperties.getHost());
        assertEquals(6379, redisProperties.getPort());
        assertEquals("ruoyi123", redisProperties.getPassword());
        assertEquals("127.0.0.1:9200", easyEsAddress);
        assertEquals("http://127.0.0.1:5601", addressSearchProperties.getKibanaUrl());
        assertEquals("address:search:maintenance:running", addressSearchProperties.getMaintenanceLockKey());
    }
}
```

- [ ] **Step 2: 运行 standalone 失败测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=StandaloneProfileSmokeTest test`

Expected: FAIL，提示 standalone profile 尚未绑定本地 Redis 或 Easy-ES 地址。

- [ ] **Step 3: 打开 standalone 的 Redis/Lock4j/ES 本地联调配置**

```yaml
# ruoyi-modules/ruoyi-address/src/main/resources/application-standalone.yml
spring:
  main:
    allow-bean-definition-overriding: true
  autoconfigure:
    exclude:
      - org.apache.seata.spring.boot.autoconfigure.SeataCoreAutoConfiguration
      - org.apache.seata.spring.boot.autoconfigure.SeataAutoConfiguration
      - org.apache.seata.spring.boot.autoconfigure.SeataDataSourceAutoConfiguration
      - org.apache.seata.spring.boot.autoconfigure.SeataHttpAutoConfiguration
      - org.apache.seata.spring.boot.autoconfigure.SeataSagaAutoConfiguration
      - org.dromara.common.translation.config.TranslationConfig
      - org.dromara.common.translation.core.impl.DeptNameTranslationImpl
      - org.dromara.common.translation.core.impl.DictTypeTranslationImpl
      - org.dromara.common.translation.core.impl.OssUrlTranslationImpl
      - org.dromara.common.translation.core.impl.UserNameTranslationImpl
      - org.dromara.common.translation.core.impl.NicknameTranslationImpl
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: ruoyi123
      database: 0
      repositories:
        enabled: false

easy-es:
  enable: true
  compatible: true
  address: 127.0.0.1:9200
  schema: http

address:
  search:
    enabled: true
    kibana-url: http://127.0.0.1:5601
    maintenance-lock-key: address:search:maintenance:running
    refresh-policy: wait_for
    rebuild-batch-size: 500
    pit-keep-alive: 1m
    standard:
      alias: address_standard_search
      read-enabled: false
      write-enabled: false
    installation:
      alias: address_installation_search
      read-enabled: false
      write-enabled: false
```

- [ ] **Step 4: 运行 smoke 与手工联调命令**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=StandaloneProfileSmokeTest test`

Expected: PASS，standalone profile 绑定本地 Redis/ES/Kibana 配置成功。

Run: `docker compose -f script/docker/docker-compose.yml up -d redis elasticsearch kibana`

Expected: PASS，三个容器启动完成。

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests spring-boot:run -Dspring-boot.run.profiles=standalone`

Expected: PASS，后端启动后可访问 `http://127.0.0.1:9206`。

Run: `npm --prefix ruoyi-address-ui run dev`

Expected: PASS，前端开发服务器启动，`/ops/search` 页面可访问。

- [ ] **Step 5: 提交任务 6**

```bash
git add \
  ruoyi-modules/ruoyi-address/src/main/resources/application-standalone.yml \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/StandaloneProfileSmokeTest.java
git commit -m "chore: enable standalone es ops smoke config"
```
