package org.dromara.address.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.dromara.address.domain.bo.StandardAddressApprovalBo;
import org.dromara.address.domain.vo.StandardAddressApprovalVo;
import org.dromara.address.service.IStandardAddressApprovalService;
import org.dromara.common.excel.utils.ExcelUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 标准地址审批控制器导出单元测试。
 * 目的：验证“已审批”导出接口会复用服务层筛选结果并写出 Excel。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressApprovalControllerExportTest {

    @Mock
    private IStandardAddressApprovalService approvalService;

    @InjectMocks
    private StandardAddressApprovalController controller;

    @Test
    void shouldExportHandledApprovals() throws Exception {
        StandardAddressApprovalBo bo = new StandardAddressApprovalBo();
        StandardAddressApprovalVo vo = new StandardAddressApprovalVo();
        vo.setApplyNo("STDADDRAPP1001");
        vo.setApprovalStatusName("审批通过");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(approvalService.listHandledForExport(bo)).thenReturn(List.of(vo));

        try (var stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class);
             var excelUtilMock = org.mockito.Mockito.mockStatic(ExcelUtil.class)) {
            stpUtilMock.when(() -> StpUtil.hasPermission("address:standard:export")).thenReturn(true);
            stpUtilMock.when(() -> StpUtil.hasPermission("address:standard:list")).thenReturn(false);
            stpUtilMock.when(() -> StpUtil.hasRoleOr(anyString(), anyString())).thenReturn(false);
            excelUtilMock.when(() -> ExcelUtil.encodingFilename(anyString())).thenReturn("handled.xlsx");

            controller.exportHandledApprovals(bo, response);

            verify(approvalService).listHandledForExport(bo);
            excelUtilMock.verify(() -> ExcelUtil.exportExcel(
                eq(List.of(vo)),
                eq("标准地址已审批记录"),
                eq(StandardAddressApprovalVo.class),
                eq(response)
            ));
        }
    }
}
