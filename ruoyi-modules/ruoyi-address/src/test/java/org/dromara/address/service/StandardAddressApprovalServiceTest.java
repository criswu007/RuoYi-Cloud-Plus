package org.dromara.address.service;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.StandardAddressApproval;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.bo.StandardAddressApprovalActionBo;
import org.dromara.address.domain.bo.StandardAddressApprovalBo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.StandardAddressApprovalMapper;
import org.dromara.address.service.impl.StandardAddressApprovalService;
import org.dromara.address.service.impl.StandardAddressApprovalExecutor;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.address.workflow.AddressWorkflowHttpClient;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.api.RemoteWorkflowService;
import org.dromara.workflow.api.domain.RemoteStartProcess;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审批申请服务单元测试。
 * 目的：验证标准地址新增申请会先落申请单，再发起 workflow。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressApprovalServiceTest {

    @Mock
    private StandardAddressApprovalMapper approvalMapper;

    @Mock
    private AddrSegmMapper addrSegmMapper;

    @Mock
    private RemoteWorkflowService remoteWorkflowService;

    @Mock
    private AddressWorkflowHttpClient workflowHttpClient;

    @Mock
    private StandardAddressApprovalExecutor approvalExecutor;

    @InjectMocks
    private StandardAddressApprovalService approvalService;

    @Test
    void shouldCreateAddApprovalAndStartWorkflow() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setParentSegmId("parent-1");
        bo.setSegmName("莲花新城南苑");
        bo.setAddrLevel(9);
        bo.setSegmType("180005");

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("parent-1");
        parent.setStandName("江苏省南京市建邺区");

        when(addrSegmMapper.selectById("parent-1")).thenReturn(parent);
        when(approvalMapper.insert(any(StandardAddressApproval.class))).thenReturn(1);
        when(approvalMapper.updateById(any(StandardAddressApproval.class))).thenReturn(1);
        when(remoteWorkflowService.startCompleteTask(any(RemoteStartProcess.class))).thenReturn(true);
        when(remoteWorkflowService.getInstanceIdByBusinessId(any(String.class))).thenReturn(9001L);

        Boolean result;
        try (MockedStatic<SpringUtil> springUtil = org.mockito.Mockito.mockStatic(SpringUtil.class);
             MockedStatic<LoginHelper> loginHelper = org.mockito.Mockito.mockStatic(LoginHelper.class)) {
            springUtil.when(() -> SpringUtil.getBean(ObjectMapper.class)).thenReturn(new ObjectMapper());
            loginHelper.when(LoginHelper::getUserId).thenReturn(100L);
            loginHelper.when(LoginHelper::getUsername).thenReturn("tester");
            loginHelper.when(LoginHelper::getDeptId).thenReturn(200L);
            loginHelper.when(LoginHelper::getDeptName).thenReturn("研发部");
            result = approvalService.submitAddApproval(bo);
        }

        assertTrue(Boolean.TRUE.equals(result));

        ArgumentCaptor<StandardAddressApproval> entityCaptor = ArgumentCaptor.forClass(StandardAddressApproval.class);
        verify(approvalMapper).insert(entityCaptor.capture());
        StandardAddressApproval approval = entityCaptor.getValue();
        assertEquals("ADD", approval.getOperationType());
        assertEquals("WAITING", approval.getApprovalStatus());
        assertEquals(9001L, approval.getInstanceId());
        assertNotNull(approval.getApplyNo());

        ArgumentCaptor<RemoteStartProcess> workflowCaptor = ArgumentCaptor.forClass(RemoteStartProcess.class);
        verify(remoteWorkflowService).startCompleteTask(workflowCaptor.capture());
        RemoteStartProcess process = workflowCaptor.getValue();
        assertEquals(String.valueOf(approval.getId()), process.getBusinessId());
        assertEquals(StandardAddressApprovalService.FLOW_CODE, process.getFlowCode());
        assertEquals(approval.getApplyNo(), process.getBizExt().getBusinessCode());
        assertTrue(process.getBizExt().getBusinessTitle().contains("标准地址新增审批"));
    }

    @Test
    void shouldQueryHandledPageByCurrentApprover() {
        StandardAddressApproval approval = new StandardAddressApproval();
        approval.setId(1001L);
        approval.setApplyNo("STDADDRAPP1001");
        approval.setBizTitle("标准地址编辑审批-莲花新城南苑");
        approval.setApprovalStatus(StandardAddressApprovalService.APPROVAL_REJECTED);

        Page<StandardAddressApproval> page = new Page<>(1, 10, 1);
        page.setRecords(java.util.List.of(approval));
        when(approvalMapper.selectPage(any(Page.class), any())).thenReturn(page);

        TableDataInfo<?> result;
        try (MockedStatic<LoginHelper> loginHelper = org.mockito.Mockito.mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::getUserId).thenReturn(100L);
            result = approvalService.queryHandledPage(new StandardAddressApprovalBo(), new PageQuery());
        }

        assertEquals(1, result.getTotal());
        verify(approvalMapper).selectPage(any(Page.class), any());
    }

    @Test
    void shouldRejectApprovalAndTerminateWorkflowTask() {
        StandardAddressApprovalActionBo bo = new StandardAddressApprovalActionBo();
        bo.setTaskId(9002L);
        bo.setBusinessId("1001");
        bo.setNodeCode("stdaddr-approve");
        bo.setMessage("请补充说明");

        StandardAddressApproval approval = new StandardAddressApproval();
        approval.setId(1001L);
        approval.setDelFlag("0");
        approval.setApprovalStatus(StandardAddressApprovalService.APPROVAL_WAITING);

        when(approvalMapper.selectById(1001L)).thenReturn(approval);
        when(approvalMapper.updateById(any(StandardAddressApproval.class))).thenReturn(1);

        Boolean result;
        try (MockedStatic<LoginHelper> loginHelper = org.mockito.Mockito.mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::getUserId).thenReturn(100L);
            loginHelper.when(LoginHelper::getUsername).thenReturn("approver");
            result = approvalService.reject(bo);
        }

        assertTrue(Boolean.TRUE.equals(result));
        verify(workflowHttpClient).terminationTask(9002L, "请补充说明");
        ArgumentCaptor<StandardAddressApproval> approvalCaptor = ArgumentCaptor.forClass(StandardAddressApproval.class);
        verify(approvalMapper).updateById(approvalCaptor.capture());
        StandardAddressApproval updated = approvalCaptor.getValue();
        assertEquals(StandardAddressApprovalService.APPROVAL_REJECTED, updated.getApprovalStatus());
        assertEquals("请补充说明", updated.getRejectReason());
    }

    @Test
    void shouldRejectDuplicateApprovalWithFriendlyMessage() {
        StandardAddressApprovalActionBo bo = new StandardAddressApprovalActionBo();
        bo.setTaskId(9002L);
        bo.setBusinessId("1001");
        bo.setMessage("重复驳回");

        StandardAddressApproval approval = new StandardAddressApproval();
        approval.setId(1001L);
        approval.setDelFlag("0");
        approval.setApprovalStatus(StandardAddressApprovalService.APPROVAL_REJECTED);
        approval.setCurrentTaskId(null);
        when(approvalMapper.selectById(1001L)).thenReturn(approval);

        org.dromara.common.core.exception.ServiceException ex = assertThrows(
            org.dromara.common.core.exception.ServiceException.class,
            () -> approvalService.reject(bo)
        );

        assertEquals("该审批已处理，请刷新列表后重试", ex.getMessage());
        verify(workflowHttpClient, never()).terminationTask(any(), any());
        verify(approvalMapper, never()).updateById(any(StandardAddressApproval.class));
    }

    @Test
    void shouldApproveDuplicateApprovalWithFriendlyMessage() {
        StandardAddressApprovalActionBo bo = new StandardAddressApprovalActionBo();
        bo.setTaskId(9001L);
        bo.setBusinessId("1001");
        bo.setMessage("审批通过");

        StandardAddressApproval approval = new StandardAddressApproval();
        approval.setId(1001L);
        approval.setDelFlag("0");
        approval.setApprovalStatus(StandardAddressApprovalService.APPROVAL_APPROVED);
        approval.setCurrentTaskId(null);
        when(approvalMapper.selectById(1001L)).thenReturn(approval);

        org.dromara.common.core.exception.ServiceException ex = assertThrows(
            org.dromara.common.core.exception.ServiceException.class,
            () -> approvalService.approve(bo)
        );

        assertEquals("该审批已处理，请刷新列表后重试", ex.getMessage());
        verify(workflowHttpClient, never()).completeTask(any());
        verify(approvalExecutor, never()).execute(any(StandardAddressApproval.class));
    }

    @Test
    void shouldListHandledApprovalsForExport() {
        StandardAddressApproval approval = new StandardAddressApproval();
        approval.setId(1001L);
        approval.setApplyNo("STDADDRAPP1001");
        approval.setBizTitle("标准地址编辑审批-莲花新城南苑");
        approval.setApprovalStatus(StandardAddressApprovalService.APPROVAL_APPROVED);
        approval.setApproveUserName("approver");

        when(approvalMapper.selectList(any())).thenReturn(java.util.List.of(approval));

        java.util.List<org.dromara.address.domain.vo.StandardAddressApprovalVo> result;
        try (MockedStatic<LoginHelper> loginHelper = org.mockito.Mockito.mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::getUserId).thenReturn(100L);
            result = approvalService.listHandledForExport(new StandardAddressApprovalBo());
        }

        assertEquals(1, result.size());
        assertEquals("STDADDRAPP1001", result.get(0).getApplyNo());
        assertEquals("审批通过", result.get(0).getApprovalStatusName());
        verify(approvalMapper).selectList(any());
    }

    @Test
    void shouldRejectDuplicateImportApprovalWhenActiveApprovalExists() {
        StandardAddressApproval existing = new StandardAddressApproval();
        existing.setId(1001L);
        existing.setApplyNo("STDADDRAPP1001");
        existing.setApprovalStatus(StandardAddressApprovalService.APPROVAL_WAITING);
        existing.setDelFlag("0");
        existing.setSubmitUserId(100L);

        when(approvalMapper.selectOne(any())).thenReturn(existing);

        org.dromara.common.core.exception.ServiceException ex;
        try (MockedStatic<SpringUtil> springUtil = org.mockito.Mockito.mockStatic(SpringUtil.class);
             MockedStatic<LoginHelper> loginHelper = org.mockito.Mockito.mockStatic(LoginHelper.class)) {
            springUtil.when(() -> SpringUtil.getBean(ObjectMapper.class)).thenReturn(new ObjectMapper());
            loginHelper.when(LoginHelper::getUserId).thenReturn(100L);
            loginHelper.when(LoginHelper::getUsername).thenReturn("tester");
            loginHelper.when(LoginHelper::getDeptId).thenReturn(200L);
            loginHelper.when(LoginHelper::getDeptName).thenReturn("研发部");
            ex = assertThrows(
                org.dromara.common.core.exception.ServiceException.class,
                () -> approvalService.submitImportApproval(java.util.List.of(), false, "tester", "标准地址导入.xlsx")
            );
        }

        assertEquals("相同内容已提交审批，请勿重复提交。申请单号：STDADDRAPP1001", ex.getMessage());
        verify(approvalMapper).selectOne(any());
        verify(approvalMapper, never()).insert(any(StandardAddressApproval.class));
    }

    @Test
    void shouldRejectDuplicateImportApprovalWhenInsertConflicts() {
        when(approvalMapper.selectOne(any())).thenReturn(null);
        when(approvalMapper.insert(any(StandardAddressApproval.class))).thenThrow(new DuplicateKeyException("duplicate"));

        StandardAddressApproval existing = new StandardAddressApproval();
        existing.setId(1002L);
        existing.setApplyNo("STDADDRAPP1002");
        existing.setApprovalStatus(StandardAddressApprovalService.APPROVAL_WAITING);
        existing.setDelFlag("0");
        when(approvalMapper.selectOne(any())).thenReturn(null, existing);

        org.dromara.common.core.exception.ServiceException ex;
        try (MockedStatic<SpringUtil> springUtil = org.mockito.Mockito.mockStatic(SpringUtil.class);
             MockedStatic<LoginHelper> loginHelper = org.mockito.Mockito.mockStatic(LoginHelper.class)) {
            springUtil.when(() -> SpringUtil.getBean(ObjectMapper.class)).thenReturn(new ObjectMapper());
            loginHelper.when(LoginHelper::getUserId).thenReturn(100L);
            loginHelper.when(LoginHelper::getUsername).thenReturn("tester");
            loginHelper.when(LoginHelper::getDeptId).thenReturn(200L);
            loginHelper.when(LoginHelper::getDeptName).thenReturn("研发部");
            ex = assertThrows(
                org.dromara.common.core.exception.ServiceException.class,
                () -> approvalService.submitImportApproval(java.util.List.of(), false, "tester", "标准地址导入.xlsx")
            );
        }

        assertEquals("相同内容已提交审批，请勿重复提交。申请单号：STDADDRAPP1002", ex.getMessage());
        verify(approvalMapper).insert(any(StandardAddressApproval.class));
    }

    @Test
    void shouldCreateSingleRowImportApprovalAndCarryBatchContext() throws Exception {
        StandardAddressImportVo row = new StandardAddressImportVo();
        row.setParentStandName("江苏省南京市鼓楼区中央路");
        row.setSegmName("紫峰大厦");
        row.setSegmTypeName("建筑、楼栋");

        when(approvalMapper.selectOne(any())).thenReturn(null);
        when(approvalMapper.insert(any(StandardAddressApproval.class))).thenReturn(1);
        when(approvalMapper.updateById(any(StandardAddressApproval.class))).thenReturn(1);
        when(remoteWorkflowService.startCompleteTask(any(RemoteStartProcess.class))).thenReturn(true);
        when(remoteWorkflowService.getInstanceIdByBusinessId(any(String.class))).thenReturn(9003L);

        StandardAddressApproval result;
        try (MockedStatic<SpringUtil> springUtil = org.mockito.Mockito.mockStatic(SpringUtil.class);
             MockedStatic<LoginHelper> loginHelper = org.mockito.Mockito.mockStatic(LoginHelper.class)) {
            springUtil.when(() -> SpringUtil.getBean(ObjectMapper.class)).thenReturn(new ObjectMapper());
            loginHelper.when(LoginHelper::getUserId).thenReturn(100L);
            loginHelper.when(LoginHelper::getUsername).thenReturn("tester");
            loginHelper.when(LoginHelper::getDeptId).thenReturn(200L);
            loginHelper.when(LoginHelper::getDeptName).thenReturn("研发部");
            result = approvalService.submitImportRowApproval(row, 9001L, 8001L, 12, false, "tester", "标准地址导入.xlsx");
        }

        assertNotNull(result);
        assertEquals(StandardAddressApprovalService.OPERATION_IMPORT, result.getOperationType());

        ArgumentCaptor<StandardAddressApproval> entityCaptor = ArgumentCaptor.forClass(StandardAddressApproval.class);
        verify(approvalMapper).insert(entityCaptor.capture());
        StandardAddressApproval approval = entityCaptor.getValue();
        assertTrue(approval.getTargetSummary().contains("第12行"));

        StandardAddressApprovalService.ImportApprovalPayload payload =
            new ObjectMapper().readValue(approval.getRequestPayload(), StandardAddressApprovalService.ImportApprovalPayload.class);
        assertEquals(9001L, payload.getBatchId());
        assertEquals(8001L, payload.getItemId());
        assertEquals(12, payload.getRowNum());
        assertEquals("tester", payload.getOperName());
        assertEquals("标准地址导入.xlsx", payload.getFileName());
        assertEquals("紫峰大厦", payload.getRow().getSegmName());

        ArgumentCaptor<RemoteStartProcess> workflowCaptor = ArgumentCaptor.forClass(RemoteStartProcess.class);
        verify(remoteWorkflowService).startCompleteTask(workflowCaptor.capture());
        assertEquals(String.valueOf(approval.getId()), workflowCaptor.getValue().getBusinessId());
    }
}
