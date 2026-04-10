package org.dromara.address.monitor.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.dromara.address.monitor.detector.FormatStandardDetector;
import org.dromara.address.monitor.detector.MonitorRuleDetector;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class MonitorExecutionServiceImplTest {

    @Mock
    private StandardAddressMonitorTaskMapper taskMapper;

    @Mock
    private StandardAddressMonitorTaskRuleRelMapper taskRuleRelMapper;

    @Mock
    private StandardAddressMonitorTaskScopeRelMapper taskScopeRelMapper;

    @Mock
    private StandardAddressMonitorTaskRunLogMapper taskRunLogMapper;

    @Mock
    private StandardAddressMonitorRuleMapper ruleMapper;

    @Mock
    private StandardAddressMonitorRecordMapper recordMapper;

    @Mock
    private AddrSegmMapper addrSegmMapper;

    @InjectMocks
    private MonitorExecutionServiceImpl service;

    @Test
    void shouldExecuteTaskAndPersistMonitorRecordWithRunLogSummary() {
        StandardAddressMonitorTask task = new StandardAddressMonitorTask();
        task.setId(101L);
        task.setTaskName("鼓楼区夜间巡检");
        task.setMonitorScope("REGION");
        task.setTaskStatus("ENABLED");

        StandardAddressMonitorRule supportedRule = new StandardAddressMonitorRule();
        supportedRule.setId(11L);
        supportedRule.setName("格式规范巡检");
        supportedRule.setRuleTemplate("FORMAT_STANDARD");
        supportedRule.setStatus("0");
        supportedRule.setSeverity("HIGH");
        supportedRule.setDedupHours(24);

        StandardAddressMonitorRule unsupportedRule = new StandardAddressMonitorRule();
        unsupportedRule.setId(12L);
        unsupportedRule.setName("未知规则");
        unsupportedRule.setRuleTemplate("UNKNOWN_TEMPLATE");
        unsupportedRule.setStatus("0");
        unsupportedRule.setSeverity("LOW");

        StandardAddressMonitorTaskRuleRel supportedRel = new StandardAddressMonitorTaskRuleRel();
        supportedRel.setTaskId(101L);
        supportedRel.setRuleId(11L);
        StandardAddressMonitorTaskRuleRel unsupportedRel = new StandardAddressMonitorTaskRuleRel();
        unsupportedRel.setTaskId(101L);
        unsupportedRel.setRuleId(12L);

        StandardAddressMonitorTaskScopeRel scopeRel = new StandardAddressMonitorTaskScopeRel();
        scopeRel.setTaskId(101L);
        scopeRel.setScopeType("REGION");
        scopeRel.setScopeValue("320106");

        AddrSegm abnormalAddress = new AddrSegm();
        abnormalAddress.setSegmId("10001");
        abnormalAddress.setStandName("123456");
        abnormalAddress.setRegionId("320106");
        abnormalAddress.setDeleteState("0");

        Page<AddrSegm> firstPage = new Page<>(1, 200, 1);
        firstPage.setRecords(List.of(abnormalAddress));
        Page<AddrSegm> secondPage = new Page<>(2, 200, 1);
        secondPage.setRecords(List.of());

        when(taskMapper.selectById(101L)).thenReturn(task);
        when(taskRuleRelMapper.selectList(any())).thenReturn(List.of(supportedRel, unsupportedRel));
        when(taskScopeRelMapper.selectList(any())).thenReturn(List.of(scopeRel));
        when(ruleMapper.selectList(any())).thenReturn(List.of(supportedRule, unsupportedRule));
        when(addrSegmMapper.selectPage(any(Page.class), any())).thenReturn(firstPage, secondPage);
        when(recordMapper.selectOne(any())).thenReturn(null);
        when(taskRunLogMapper.insert(any(StandardAddressMonitorTaskRunLog.class))).thenAnswer(invocation -> {
            StandardAddressMonitorTaskRunLog log = invocation.getArgument(0);
            log.setId(501L);
            return 1;
        });
        when(recordMapper.insert(any(StandardAddressMonitorRecord.class))).thenReturn(1);
        when(taskRunLogMapper.updateById(any(StandardAddressMonitorTaskRunLog.class))).thenReturn(1);
        when(taskMapper.updateById(any(StandardAddressMonitorTask.class))).thenReturn(1);

        service = new MonitorExecutionServiceImpl(
            taskMapper,
            taskRuleRelMapper,
            taskScopeRelMapper,
            taskRunLogMapper,
            ruleMapper,
            recordMapper,
            addrSegmMapper,
            List.<MonitorRuleDetector>of(new FormatStandardDetector())
        );

        int createdCount = service.executeTask(101L, "MANUAL");

        assertEquals(1, createdCount);

        ArgumentCaptor<StandardAddressMonitorRecord> recordCaptor = ArgumentCaptor.forClass(StandardAddressMonitorRecord.class);
        verify(recordMapper).insert(recordCaptor.capture());
        StandardAddressMonitorRecord insertedRecord = recordCaptor.getValue();
        assertEquals(10001L, insertedRecord.getStandardAddressId());
        assertEquals("123456", insertedRecord.getStandNameSnapshot());
        assertEquals("格式规范巡检", insertedRecord.getRuleNameSnapshot());
        assertEquals("FORMAT_STANDARD", insertedRecord.getRuleTemplateSnapshot());
        assertEquals("HIGH", insertedRecord.getSeverity());
        assertEquals("0", insertedRecord.getStatus());
        assertEquals(501L, insertedRecord.getTaskRunLogId());
        assertNotNull(insertedRecord.getFirstDetectedTime());

        ArgumentCaptor<StandardAddressMonitorTaskRunLog> runLogCaptor = ArgumentCaptor.forClass(StandardAddressMonitorTaskRunLog.class);
        verify(taskRunLogMapper).updateById(runLogCaptor.capture());
        StandardAddressMonitorTaskRunLog updatedRunLog = runLogCaptor.getValue();
        assertEquals("SUCCESS", updatedRunLog.getExecuteStatus());
        assertEquals(1L, updatedRunLog.getScannedCount());
        assertEquals(1L, updatedRunLog.getHitCount());
        assertEquals(1L, updatedRunLog.getCreatedCount());
        assertTrue(updatedRunLog.getExecuteMessage().contains("unsupportedRuleCount=1"));

        ArgumentCaptor<StandardAddressMonitorTask> taskCaptor = ArgumentCaptor.forClass(StandardAddressMonitorTask.class);
        verify(taskMapper).updateById(taskCaptor.capture());
        StandardAddressMonitorTask updatedTask = taskCaptor.getValue();
        assertEquals(101L, updatedTask.getId());
        assertNotNull(updatedTask.getLastExecuteTime());
        assertNotNull(updatedTask.getLastSuccessTime());
        assertNull(updatedTask.getLastFailureReason());
    }
}
