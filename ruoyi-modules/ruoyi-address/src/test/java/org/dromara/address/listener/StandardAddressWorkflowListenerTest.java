package org.dromara.address.listener;

import cn.hutool.extra.spring.SpringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.address.domain.StandardAddressApproval;
import org.dromara.address.domain.StandardAddressImportDetail;
import org.dromara.address.mapper.StandardAddressApprovalMapper;
import org.dromara.address.mapper.StandardAddressImportBatchMapper;
import org.dromara.address.mapper.StandardAddressImportDetailMapper;
import org.dromara.address.service.IStandardAddressImportBatchService;
import org.dromara.address.service.impl.StandardAddressApprovalExecutor;
import org.dromara.address.service.impl.StandardAddressApprovalService;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.common.core.exception.ServiceException;
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
import static org.mockito.Mockito.doThrow;
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

    @Mock
    private StandardAddressImportDetailMapper importDetailMapper;

    @Mock
    private StandardAddressImportBatchMapper importBatchMapper;

    @Mock
    private IStandardAddressImportBatchService importBatchService;

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

    @Test
    void shouldMarkImportRowRejectedAndRefreshBatchSummary() throws Exception {
        StandardAddressApproval approval = buildImportApproval(1002L);
        approval.setApplyNo("STDADDRAPP1002");
        approval.setApprovalStatus(StandardAddressApprovalService.APPROVAL_WAITING);

        StandardAddressImportDetail detail = new StandardAddressImportDetail();
        detail.setId(8001L);
        detail.setBatchId(9001L);
        detail.setStatus(StandardAddressImportDetail.STATUS_WAITING_APPROVAL);

        when(approvalMapper.selectById(1002L)).thenReturn(approval);
        when(importDetailMapper.selectById(8001L)).thenReturn(detail);

        try (MockedStatic<SpringUtil> springUtil = org.mockito.Mockito.mockStatic(SpringUtil.class)) {
            springUtil.when(SpringUtil::getApplicationName).thenReturn("ruoyi-address-test");

            ProcessEvent processEvent = new ProcessEvent();
            processEvent.setFlowCode(StandardAddressWorkflowListener.FLOW_CODE);
            processEvent.setBusinessId("1002");
            processEvent.setStatus("back");
            processEvent.setSubmit(false);
            Map<String, Object> params = new HashMap<>();
            params.put("message", "父级地址信息有误");
            params.put("handler", "200");
            processEvent.setParams(params);

            workflowListener.processHandler(processEvent);
        }

        ArgumentCaptor<StandardAddressImportDetail> detailCaptor = ArgumentCaptor.forClass(StandardAddressImportDetail.class);
        verify(importDetailMapper).updateById(detailCaptor.capture());
        assertEquals(StandardAddressImportDetail.STATUS_REJECTED_FAILED, detailCaptor.getValue().getStatus());
        assertEquals("父级地址信息有误", detailCaptor.getValue().getFailReason());
        verify(importBatchService).refreshBatchSummary(9001L);
    }

    @Test
    void shouldMarkImportRowExecuteFailedWhenFormalImportThrows() throws Exception {
        StandardAddressApproval approval = buildImportApproval(1003L);
        approval.setApplyNo("STDADDRAPP1003");
        approval.setApprovalStatus(StandardAddressApprovalService.APPROVAL_WAITING);

        StandardAddressImportDetail detail = new StandardAddressImportDetail();
        detail.setId(8001L);
        detail.setBatchId(9001L);
        detail.setStatus(StandardAddressImportDetail.STATUS_WAITING_APPROVAL);

        when(approvalMapper.selectById(1003L)).thenReturn(approval);
        when(importDetailMapper.selectById(8001L)).thenReturn(detail);
        doThrow(new ServiceException("正式写入失败")).when(approvalExecutor).execute(eq(approval));

        try (MockedStatic<SpringUtil> springUtil = org.mockito.Mockito.mockStatic(SpringUtil.class)) {
            springUtil.when(SpringUtil::getApplicationName).thenReturn("ruoyi-address-test");

            ProcessEvent processEvent = new ProcessEvent();
            processEvent.setFlowCode(StandardAddressWorkflowListener.FLOW_CODE);
            processEvent.setBusinessId("1003");
            processEvent.setStatus("finish");
            processEvent.setSubmit(false);
            processEvent.setParams(new HashMap<>());

            workflowListener.processHandler(processEvent);
        }

        ArgumentCaptor<StandardAddressImportDetail> detailCaptor = ArgumentCaptor.forClass(StandardAddressImportDetail.class);
        verify(importDetailMapper, org.mockito.Mockito.times(2)).updateById(detailCaptor.capture());
        List<StandardAddressImportDetail> updates = detailCaptor.getAllValues();
        assertEquals(StandardAddressImportDetail.STATUS_EXECUTE_FAILED, updates.get(updates.size() - 1).getStatus());
        assertEquals("正式写入失败", updates.get(updates.size() - 1).getFailReason());
        verify(importBatchService, org.mockito.Mockito.times(2)).refreshBatchSummary(9001L);
    }

    private StandardAddressApproval buildImportApproval(Long approvalId) throws Exception {
        StandardAddressImportVo row = new StandardAddressImportVo();
        row.setParentStandName("江苏省南京市鼓楼区中央路");
        row.setSegmName("紫峰大厦");
        row.setSegmTypeName("建筑、楼栋");

        StandardAddressApproval approval = new StandardAddressApproval();
        approval.setId(approvalId);
        approval.setOperationType(StandardAddressApprovalService.OPERATION_IMPORT);
        approval.setRequestPayload(new ObjectMapper().writeValueAsString(
            new StandardAddressApprovalService.ImportApprovalPayload(9001L, 8001L, 12, false, "tester", "标准地址导入.xlsx", row)
        ));
        return approval;
    }
}
