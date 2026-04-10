package org.dromara.address.service;

import org.dromara.address.domain.StandardAddressMonitorTask;
import org.dromara.address.domain.bo.StandardAddressAdminBo;
import org.dromara.address.job.StandardAddressNonStandardMonitorJob;
import org.dromara.address.mapper.StandardAddressMonitorRecordMapper;
import org.dromara.address.mapper.StandardAddressMonitorRuleMapper;
import org.dromara.address.mapper.StandardAddressMonitorTaskMapper;
import org.dromara.address.mapper.StandardAddressMonitorTaskRuleRelMapper;
import org.dromara.address.mapper.StandardAddressMonitorTaskRunLogMapper;
import org.dromara.address.mapper.StandardAddressMonitorTaskScopeRelMapper;
import org.dromara.address.monitor.MonitorExecutionService;
import org.dromara.address.service.impl.StandardAddressMonitorTaskServiceImpl;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.core.task.TaskExecutor;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressMonitorTaskExecutionServiceTest {

    @Mock
    private StandardAddressMonitorTaskMapper monitorTaskMapper;

    @Mock
    private StandardAddressMonitorTaskRuleRelMapper taskRuleRelMapper;

    @Mock
    private StandardAddressMonitorTaskScopeRelMapper taskScopeRelMapper;

    @Mock
    private StandardAddressMonitorTaskRunLogMapper taskRunLogMapper;

    @Mock
    private StandardAddressMonitorRuleMapper addressMonitorRuleMapper;

    @Mock
    private StandardAddressMonitorRecordMapper addressMonitorRecordMapper;

    @Mock
    private StandardAddressNonStandardMonitorJob nonStandardMonitorJob;

    @Mock
    private MonitorTaskSchedulerService monitorTaskSchedulerService;

    @Mock
    private MonitorExecutionService monitorExecutionService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<String> manualExecutionBucket;

    @Mock
    private TaskExecutor taskExecutor;

    @InjectMocks
    private StandardAddressMonitorTaskServiceImpl service;

    @Test
    void shouldSubmitManualExecutionAsynchronously() {
        AtomicReference<String> tokenRef = new AtomicReference<>();
        when(redissonClient.<String>getBucket("address:monitor:task:manual:execute")).thenReturn(manualExecutionBucket);
        when(manualExecutionBucket.setIfAbsent(anyString(), any(Duration.class))).thenAnswer(invocation -> {
            tokenRef.set(invocation.getArgument(0));
            return true;
        });
        when(manualExecutionBucket.get()).thenAnswer(invocation -> tokenRef.get());
        when(nonStandardMonitorJob.executeMonitor()).thenReturn(7);
        when(addressMonitorRuleMapper.selectCount(any())).thenReturn(0L);
        when(addressMonitorRecordMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));

        Integer result = service.executeNow();

        assertEquals(1, result);
        assertEquals(7, service.querySummary().getLastCreatedCount());
        verify(taskExecutor).execute(any(Runnable.class));
        verify(manualExecutionBucket).delete();
    }

    @Test
    void shouldRejectManualExecutionWhenAnotherRunIsActive() {
        when(redissonClient.<String>getBucket("address:monitor:task:manual:execute")).thenReturn(manualExecutionBucket);
        when(manualExecutionBucket.setIfAbsent(anyString(), any(Duration.class))).thenReturn(false);

        Integer result = service.executeNow();

        assertEquals(0, result);
        verify(taskExecutor, never()).execute(any(Runnable.class));
        verify(nonStandardMonitorJob, never()).executeMonitor();
    }

    @Test
    void shouldDelegateRerunToMonitorExecutionService() {
        StandardAddressMonitorTask task = new StandardAddressMonitorTask();
        task.setId(101L);
        when(monitorTaskMapper.selectById(101L)).thenReturn(task);
        when(monitorExecutionService.executeTask(101L, "MANUAL")).thenReturn(2);

        boolean success = service.rerunById(101L);

        assertTrue(success);
        verify(monitorExecutionService).executeTask(101L, "MANUAL");
    }

    @Test
    void shouldInsertMonitorTaskFromBo() {
        when(monitorTaskMapper.insert(org.mockito.ArgumentMatchers.any(StandardAddressMonitorTask.class))).thenAnswer(invocation -> {
            StandardAddressMonitorTask entity = invocation.getArgument(0);
            entity.setId(2001L);
            return 1;
        });

        StandardAddressAdminBo.MonitorTaskBo bo = new StandardAddressAdminBo.MonitorTaskBo();
        bo.setTaskName("联调测试任务");
        bo.setTaskType("MANUAL");
        bo.setMonitorScope("ALL");
        bo.setTaskStatus("ENABLED");

        Boolean result = assertDoesNotThrow(() -> service.insertByBo(bo));

        assertTrue(result);
        assertEquals(2001L, bo.getId());
        verify(monitorTaskMapper).insert(org.mockito.ArgumentMatchers.any(StandardAddressMonitorTask.class));
    }

    @Test
    void shouldMapEntityPageToMonitorTaskVoPage() {
        StandardAddressMonitorTask entity = new StandardAddressMonitorTask();
        entity.setId(3001L);
        entity.setTaskName("联调测试任务");
        entity.setTaskType("MANUAL");
        entity.setMonitorScope("ALL");
        entity.setTaskStatus("ENABLED");

        Page<StandardAddressMonitorTask> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(entity));

        when(monitorTaskMapper.selectPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(page);
        when(taskRuleRelMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyList());
        when(taskScopeRelMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyList());

        var result = service.queryPageList(new StandardAddressAdminBo.MonitorTaskBo(), new PageQuery(10, 1));

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRows().size());
        assertEquals("联调测试任务", result.getRows().get(0).getTaskName());
        assertEquals("MANUAL", result.getRows().get(0).getTaskType());
    }
}
