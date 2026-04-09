package org.dromara.address.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.bo.StandardAddressMergeBo;
import org.dromara.address.domain.bo.StandardAddressSplitBo;
import org.dromara.address.domain.vo.StandardAddressImportResultVo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.address.service.IStandardAddressService;
import org.dromara.address.search.service.StandardAddressSearchExportService;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.core.ExcelResult;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.satoken.utils.LoginHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 标准地址控制器审批文案单元测试。
 * 目的：验证标准地址写接口已切换为“提交审批成功”的返回语义。
 */
@ExtendWith(MockitoExtension.class)
class StandardAddressControllerTest {

    private static final String APPROVAL_MESSAGE = "已提交审批，待审批通过后生效，审批期间原地址可继续使用。";

    @Mock
    private IStandardAddressService standardAddressService;

    @Mock
    private StandardAddressSearchExportService standardAddressSearchExportService;

    @Mock
    private MultipartFile multipartFile;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private StandardAddressController controller;

    @Test
    void shouldReturnApprovalMessageWhenAdd() {
        StandardAddressBo bo = new StandardAddressBo();
        when(standardAddressService.addStandardAddress(bo)).thenReturn(true);

        R<Void> result = controller.addStandardAddress(bo);

        assertEquals(APPROVAL_MESSAGE, result.getMsg());
        verify(standardAddressService).addStandardAddress(bo);
    }

    @Test
    void shouldReturnApprovalMessageWhenEdit() {
        StandardAddressBo bo = new StandardAddressBo();
        when(standardAddressService.updateStandardAddress(bo)).thenReturn(true);

        R<Void> result = controller.editStandardAddress(bo);

        assertEquals(APPROVAL_MESSAGE, result.getMsg());
        verify(standardAddressService).updateStandardAddress(bo);
    }

    @Test
    void shouldReturnApprovalMessageWhenDelete() {
        when(standardAddressService.deleteStandardAddresses(List.of("1001", "1002"), false)).thenReturn(true);

        R<Void> result = controller.removeStandardAddresses(new String[]{"1001", "1002"}, false);

        assertEquals(APPROVAL_MESSAGE, result.getMsg());
        verify(standardAddressService).deleteStandardAddresses(List.of("1001", "1002"), false);
    }

    @Test
    void shouldReturnApprovalMessageWhenMerge() {
        StandardAddressMergeBo bo = new StandardAddressMergeBo();
        bo.setSourceSegmIds(List.of("1001", "1002"));
        bo.setTargetSegmId("1003");
        when(standardAddressService.mergeStandardAddresses(bo.getSourceSegmIds(), bo.getTargetSegmId())).thenReturn(true);

        R<Void> result = controller.mergeStandardAddresses(bo);

        assertEquals(APPROVAL_MESSAGE, result.getMsg());
        verify(standardAddressService).mergeStandardAddresses(bo.getSourceSegmIds(), bo.getTargetSegmId());
    }

    @Test
    void shouldReturnApprovalMessageWhenSplit() {
        StandardAddressSplitBo bo = new StandardAddressSplitBo();
        bo.setSourceSegmId("1001");
        bo.setSplitItems(List.of());
        when(standardAddressService.splitStandardAddress(bo.getSourceSegmId(), bo.getSplitItems())).thenReturn(true);

        R<Void> result = controller.splitStandardAddress(bo);

        assertEquals(APPROVAL_MESSAGE, result.getMsg());
        verify(standardAddressService).splitStandardAddress(bo.getSourceSegmId(), bo.getSplitItems());
    }

    @Test
    void shouldReturnApprovalMessageWhenImport() throws Exception {
        StandardAddressImportResultVo resultVo = new StandardAddressImportResultVo();
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(multipartFile.getOriginalFilename()).thenReturn("标准地址导入.xlsx");

        try (MockedStatic<ExcelUtil> excelUtil = org.mockito.Mockito.mockStatic(ExcelUtil.class);
             MockedStatic<LoginHelper> loginHelper = org.mockito.Mockito.mockStatic(LoginHelper.class)) {
            @SuppressWarnings("unchecked")
            ExcelResult<StandardAddressImportVo> excelResult = org.mockito.Mockito.mock(ExcelResult.class);
            when(excelResult.getList()).thenReturn(List.of());
            excelUtil.when(() -> ExcelUtil.importExcel(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(StandardAddressImportVo.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(excelResult);
            loginHelper.when(LoginHelper::getUsername).thenReturn("tester");
            when(standardAddressService.importStandardAddressData(List.of(), false, "tester", "标准地址导入.xlsx")).thenReturn(resultVo);

            R<StandardAddressImportResultVo> result = controller.importStandardAddressData(multipartFile, false);

            assertEquals(APPROVAL_MESSAGE, result.getMsg());
            assertEquals(resultVo, result.getData());
            verify(standardAddressService).importStandardAddressData(List.of(), false, "tester", "标准地址导入.xlsx");
        }
    }

    @Test
    void shouldProtectWriteEndpointsFromRepeatSubmit() throws Exception {
        assertNotNull(StandardAddressController.class
            .getMethod("addStandardAddress", StandardAddressBo.class)
            .getAnnotation(RepeatSubmit.class));
        assertNotNull(StandardAddressController.class
            .getMethod("editStandardAddress", StandardAddressBo.class)
            .getAnnotation(RepeatSubmit.class));
        assertNotNull(StandardAddressController.class
            .getMethod("removeStandardAddresses", String[].class, boolean.class)
            .getAnnotation(RepeatSubmit.class));
        assertNotNull(StandardAddressController.class
            .getMethod("mergeStandardAddresses", org.dromara.address.domain.bo.StandardAddressMergeBo.class)
            .getAnnotation(RepeatSubmit.class));
        assertNotNull(StandardAddressController.class
            .getMethod("splitStandardAddress", StandardAddressSplitBo.class)
            .getAnnotation(RepeatSubmit.class));
        assertNotNull(StandardAddressController.class
            .getMethod("batchAddStandardAddressChildren", org.dromara.address.domain.bo.StandardAddressBatchAddBo.class)
            .getAnnotation(RepeatSubmit.class));
        assertNotNull(StandardAddressController.class
            .getMethod("importStandardAddressData", MultipartFile.class, boolean.class)
            .getAnnotation(RepeatSubmit.class));
    }
}
