package org.dromara.address.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.dromara.address.domain.bo.StandardAddressApprovalBo;
import org.dromara.address.domain.bo.StandardAddressApprovalActionBo;
import org.dromara.address.domain.vo.StandardAddressApprovalVo;
import org.dromara.address.service.IStandardAddressApprovalService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 标准地址审批控制器单元测试。
 * 目的：验证审批记录分页、详情和按业务 ID 查询接口会透传到审批服务。
 */
@ExtendWith(MockitoExtension.class)
class StandardAddressApprovalControllerTest {

    @Mock
    private IStandardAddressApprovalService approvalService;

    @InjectMocks
    private StandardAddressApprovalController controller;

    @Test
    void shouldDelegateMyPageQuery() {
        StandardAddressApprovalBo bo = new StandardAddressApprovalBo();
        PageQuery pageQuery = new PageQuery();
        TableDataInfo<StandardAddressApprovalVo> tableDataInfo =
            new TableDataInfo<>(List.of(new StandardAddressApprovalVo()), 1);
        when(approvalService.queryMyPage(bo, pageQuery)).thenReturn(tableDataInfo);

        TableDataInfo<StandardAddressApprovalVo> result = controller.queryMyPage(bo, pageQuery);

        assertSame(tableDataInfo, result);
        verify(approvalService).queryMyPage(bo, pageQuery);
    }

    @Test
    void shouldReturnApprovalDetailById() {
        StandardAddressApprovalVo vo = new StandardAddressApprovalVo();
        vo.setId(1001L);
        when(approvalService.getDetail(1001L)).thenReturn(vo);

        R<StandardAddressApprovalVo> result = controller.getDetail(1001L);

        assertEquals(200, result.getCode());
        assertSame(vo, result.getData());
        verify(approvalService).getDetail(1001L);
    }

    @Test
    void shouldReturnApprovalDetailByBusinessId() {
        StandardAddressApprovalVo vo = new StandardAddressApprovalVo();
        vo.setId(1002L);
        when(approvalService.getByBusinessId("1002")).thenReturn(vo);

        R<StandardAddressApprovalVo> result = controller.getByBusinessId("1002");

        assertEquals(200, result.getCode());
        assertSame(vo, result.getData());
        verify(approvalService).getByBusinessId("1002");
    }

    @Test
    void shouldDelegateHandledPageQuery() {
        StandardAddressApprovalBo bo = new StandardAddressApprovalBo();
        PageQuery pageQuery = new PageQuery();
        TableDataInfo<StandardAddressApprovalVo> tableDataInfo =
            new TableDataInfo<>(List.of(new StandardAddressApprovalVo()), 1);
        when(approvalService.queryHandledPage(bo, pageQuery)).thenReturn(tableDataInfo);

        TableDataInfo<StandardAddressApprovalVo> result;
        try (MockedStatic<StpUtil> stpUtil = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.hasPermission("address:standard:list")).thenReturn(false);
            stpUtil.when(() -> StpUtil.hasRoleOr(anyString(), anyString())).thenReturn(true);
            result = controller.queryHandledPage(bo, pageQuery);
        }

        assertSame(tableDataInfo, result);
        verify(approvalService).queryHandledPage(bo, pageQuery);
    }

    @Test
    void shouldDelegateApproveAction() {
        StandardAddressApprovalActionBo bo = new StandardAddressApprovalActionBo();
        bo.setTaskId(9001L);
        bo.setBusinessId("1001");
        when(approvalService.approve(bo)).thenReturn(true);

        R<Void> result;
        try (MockedStatic<StpUtil> stpUtil = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.hasRoleOr(anyString(), anyString())).thenReturn(true);
            result = controller.approve(bo);
        }

        assertEquals(200, result.getCode());
        verify(approvalService).approve(bo);
    }

    @Test
    void shouldRejectApproveActionWhenCurrentUserHasNoApprovalRole() {
        StandardAddressApprovalActionBo bo = new StandardAddressApprovalActionBo();
        bo.setTaskId(9001L);
        bo.setBusinessId("1001");

        ServiceException ex;
        try (MockedStatic<StpUtil> stpUtil = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.hasRoleOr(anyString(), anyString())).thenReturn(false);
            ex = assertThrows(ServiceException.class, () -> controller.approve(bo));
        }

        assertEquals("当前用户没有标准地址审批权限，请联系管理员授权“标准地址审批员”角色或使用系统管理员账号办理", ex.getMessage());
        verify(approvalService, never()).approve(bo);
    }

    @Test
    void shouldDelegateRejectAction() {
        StandardAddressApprovalActionBo bo = new StandardAddressApprovalActionBo();
        bo.setTaskId(9002L);
        bo.setBusinessId("1001");
        bo.setNodeCode("stdaddr-approve");
        bo.setMessage("请补充说明");
        when(approvalService.reject(bo)).thenReturn(true);

        R<Void> result;
        try (MockedStatic<StpUtil> stpUtil = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.hasRoleOr(anyString(), anyString())).thenReturn(true);
            result = controller.reject(bo);
        }

        assertEquals(200, result.getCode());
        verify(approvalService).reject(bo);
    }

    @Test
    void shouldReturnApprovalActionPermissionWhenCurrentUserHasApprovalRole() {
        R<java.util.Map<String, Object>> result;
        try (MockedStatic<StpUtil> stpUtil = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.hasRoleOr(anyString(), anyString())).thenReturn(true);
            result = controller.getActionPermission();
        }

        assertEquals(200, result.getCode());
        assertTrue(Boolean.TRUE.equals(result.getData().get("canApprove")));
        assertEquals("", result.getData().get("message"));
    }

    @Test
    void shouldReturnApprovalActionPermissionHintWhenCurrentUserHasNoApprovalRole() {
        R<java.util.Map<String, Object>> result;
        try (MockedStatic<StpUtil> stpUtil = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.hasRoleOr(anyString(), anyString())).thenReturn(false);
            result = controller.getActionPermission();
        }

        assertEquals(200, result.getCode());
        assertFalse(Boolean.TRUE.equals(result.getData().get("canApprove")));
        assertEquals("当前用户没有标准地址审批权限，请联系管理员授权“标准地址审批员”角色或使用系统管理员账号办理", result.getData().get("message"));
    }

    @Test
    void shouldProtectApprovalActionsFromRepeatSubmit() throws Exception {
        assertNotNull(StandardAddressApprovalController.class
            .getMethod("approve", StandardAddressApprovalActionBo.class)
            .getAnnotation(RepeatSubmit.class));
        assertNotNull(StandardAddressApprovalController.class
            .getMethod("reject", StandardAddressApprovalActionBo.class)
            .getAnnotation(RepeatSubmit.class));
    }
}
