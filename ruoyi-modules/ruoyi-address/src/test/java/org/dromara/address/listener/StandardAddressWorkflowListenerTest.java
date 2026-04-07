package org.dromara.address.listener;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.address.domain.StandardAddressApproval;
import org.dromara.address.mapper.StandardAddressApprovalMapper;
import org.dromara.address.service.impl.StandardAddressApprovalExecutor;
import org.dromara.workflow.api.event.ProcessEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * workflow 监听器单元测试。
 * 目的：验证审批通过事件会触发正式执行并回写申请单状态。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressWorkflowListenerTest {

    @Mock
    private StandardAddressApprovalMapper approvalMapper;

    @Mock
    private StandardAddressApprovalExecutor approvalExecutor;

    @InjectMocks
    private StandardAddressWorkflowListener workflowListener;

    @Test
    void shouldExecuteApprovalWhenProcessFinished() {
        StandardAddressApproval approval = new StandardAddressApproval();
        approval.setId(1001L);
        approval.setOperationType("ADD");
        approval.setBusinessStatus("waiting");
        approval.setApprovalStatus("WAITING");

        when(approvalMapper.selectById(1001L)).thenReturn(approval);

        try (MockedStatic<SpringUtil> springUtil = org.mockito.Mockito.mockStatic(SpringUtil.class)) {
            springUtil.when(SpringUtil::getApplicationName).thenReturn("ruoyi-address-test");

            ProcessEvent processEvent = new ProcessEvent();
            processEvent.setFlowCode(StandardAddressWorkflowListener.FLOW_CODE);
            processEvent.setBusinessId("1001");
            processEvent.setStatus("finish");
            processEvent.setSubmit(false);
            Map<String, Object> params = new HashMap<>();
            params.put("handler", "200");
            processEvent.setParams(params);

            workflowListener.processHandler(processEvent);
        }

        verify(approvalExecutor).execute(eq(approval));
        ArgumentCaptor<StandardAddressApproval> entityCaptor = ArgumentCaptor.forClass(StandardAddressApproval.class);
        verify(approvalMapper, org.mockito.Mockito.times(2)).updateById(entityCaptor.capture());
        List<StandardAddressApproval> updates = entityCaptor.getAllValues();
        assertEquals("APPROVED", updates.get(updates.size() - 1).getApprovalStatus());
    }
}
