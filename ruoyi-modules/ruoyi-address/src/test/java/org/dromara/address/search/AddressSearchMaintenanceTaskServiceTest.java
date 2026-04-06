package org.dromara.address.search;

import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.executor.RedissonLockExecutor;
import org.dromara.address.config.AddressSearchProperties;
import org.dromara.address.domain.AddressSearchMaintenanceTask;
import org.dromara.address.domain.vo.AddressSearchMaintenanceVo;
import org.dromara.address.mapper.AddressSearchMaintenanceTaskMapper;
import org.dromara.address.mapper.AddressSearchRepairTaskMapper;
import org.dromara.address.search.model.AddressSearchIndexRuntimeInfo;
import org.dromara.address.search.service.AddressSearchMaintenanceService;
import org.dromara.address.search.service.AddressSearchMaintenanceTaskService;
import org.dromara.address.search.service.InstallationAddressSearchGateway;
import org.dromara.address.search.service.StandardAddressSearchGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        assertEquals(Boolean.TRUE, overview.getEsReachable());
        assertEquals("address_standard_search_v2", overview.getStandardIndex().getPhysicalIndexName());
        assertEquals(12L, overview.getPendingRepairCount());
        assertEquals(2L, overview.getFailedRepairCount());
    }
}
