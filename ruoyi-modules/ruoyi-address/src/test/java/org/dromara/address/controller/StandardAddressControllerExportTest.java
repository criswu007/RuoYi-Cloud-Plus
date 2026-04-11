package org.dromara.address.controller;

import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressImportResultVo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.address.domain.vo.StandardAddressVo;
//import org.dromara.address.excel.AddressTemplateExcelExporter;
import org.dromara.address.search.service.StandardAddressSearchExportService;
import org.dromara.address.service.IStandardAddressService;
import org.dromara.common.excel.core.DropDownOptions;
import org.dromara.common.excel.core.ExcelListener;
import org.dromara.common.excel.core.ExcelResult;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.excel.utils.ExcelWriterWrapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressControllerExportTest {

    @Mock
    private IStandardAddressService standardAddressService;

    @Mock
    private StandardAddressSearchExportService standardAddressSearchExportService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private StandardAddressController controller;

    @Test
    void shouldDelegateExportStreamingToSearchExportService() {
        StandardAddressBo bo = new StandardAddressBo();
        MockHttpServletResponse response = new MockHttpServletResponse();
        @SuppressWarnings("unchecked")
        ExcelWriterWrapper<StandardAddressVo> writer = org.mockito.Mockito.mock(ExcelWriterWrapper.class);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<List<StandardAddressVo>> consumer = invocation.getArgument(2);
            consumer.accept(buildRows(500, 1));
            consumer.accept(buildRows(1, 501));
            return null;
        }).when(standardAddressSearchExportService).writeRows(eq(bo), eq(500), any());

        try (var excelUtilMock = org.mockito.Mockito.mockStatic(ExcelUtil.class)) {
            excelUtilMock.when(() -> ExcelUtil.encodingFilename(anyString())).thenReturn("test.xlsx");
            excelUtilMock.when(() -> ExcelUtil.exportExcel(eq(StandardAddressVo.class), any(java.io.OutputStream.class), any(Consumer.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Consumer<ExcelWriterWrapper<StandardAddressVo>> consumer = invocation.getArgument(2);
                    consumer.accept(writer);
                    return null;
                });
            controller.exportStandardAddresses(bo, response);
        }

        verify(standardAddressSearchExportService).writeRows(eq(bo), eq(500), any());
        verify(writer, org.mockito.Mockito.times(2)).write(anyList(), any());
        verify(standardAddressService, never()).queryStandardAddressList(any());
        verify(standardAddressService, never()).queryStandardAddressPageList(any(), any());
    }

    @Test
    void shouldWrapAndResetResponseWhenExportFails() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        RuntimeException exportException = new RuntimeException("boom");
        try (var excelUtilMock = org.mockito.Mockito.mockStatic(ExcelUtil.class)) {
            excelUtilMock.when(() -> ExcelUtil.encodingFilename(anyString())).thenReturn("test.xlsx");
            excelUtilMock.when(() -> ExcelUtil.exportExcel(eq(StandardAddressVo.class), any(java.io.OutputStream.class), any(Consumer.class)))
                .thenThrow(exportException);
            RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> controller.exportStandardAddresses(new StandardAddressBo(), response));
            assertEquals("导出标准地址异常", thrown.getMessage());
            assertSame(exportException, thrown.getCause());
            assertNull(response.getContentType());
        }
    }

//    @Test
//    void shouldDelegateImportTemplateDownloadToAddressTemplateExcelExporter() throws Exception {
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        List<DropDownOptions> options = List.of(
//            new DropDownOptions(0, List.of("建筑、楼栋")),
//            new DropDownOptions(1, List.of("是", "否"))
//        );
//        when(standardAddressService.listStandardAddressImportTemplateOptions()).thenReturn(options);
//
//        try (var addressExcelMock = org.mockito.Mockito.mockStatic(AddressTemplateExcelExporter.class);
//             var excelUtilMock = org.mockito.Mockito.mockStatic(ExcelUtil.class)) {
//            excelUtilMock.when(() -> ExcelUtil.encodingFilename(anyString())).thenReturn("template.xlsx");
//
//            controller.downloadStandardAddressImportTemplate(response);
//
//            addressExcelMock.verify(() -> AddressTemplateExcelExporter.exportTemplate(
//                eq(List.<StandardAddressImportVo>of()),
//                eq("标准地址导入模板"),
//                eq(StandardAddressImportVo.class),
//                any(java.io.OutputStream.class),
//                eq(options),
//                eq(false)
//            ));
//
//            excelUtilMock.verify(() -> ExcelUtil.encodingFilename(anyString()));
//            excelUtilMock.verify(() -> ExcelUtil.exportExcel(
//                eq(List.<StandardAddressImportVo>of()),
//                eq("标准地址导入模板"),
//                eq(StandardAddressImportVo.class),
//                eq(response)
//            ), never());
//            excelUtilMock.verify(() -> ExcelUtil.exportExcel(
//                eq(List.<StandardAddressImportVo>of()),
//                eq("标准地址导入模板"),
//                eq(StandardAddressImportVo.class),
//                eq(response),
//                eq(options)
//            ), never());
//            excelUtilMock.verify(() -> ExcelUtil.exportExcel(
//                eq(List.<StandardAddressImportVo>of()),
//                eq("标准地址导入模板"),
//                eq(StandardAddressImportVo.class),
//                any(java.io.OutputStream.class)
//            ), never());
//            excelUtilMock.verify(() -> ExcelUtil.exportExcel(
//                eq(List.<StandardAddressImportVo>of()),
//                eq("标准地址导入模板"),
//                eq(StandardAddressImportVo.class),
//                eq(false),
//                any(java.io.OutputStream.class),
//                eq(options)
//            ), never());
//            assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8",
//                response.getContentType());
//            assertTrue(response.getHeader("Content-Disposition").contains("template.xlsx"));
//        }
//    }

    @Test
    void shouldUseInitializedExcelListenerWhenImporting() throws Exception {
        StandardAddressImportResultVo resultVo = new StandardAddressImportResultVo();
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(multipartFile.getOriginalFilename()).thenReturn("标准地址导入.xlsx");

        try (var excelUtilMock = org.mockito.Mockito.mockStatic(ExcelUtil.class);
             var loginHelperMock = org.mockito.Mockito.mockStatic(LoginHelper.class)) {
            @SuppressWarnings("unchecked")
            ExcelResult<StandardAddressImportVo> excelResult = org.mockito.Mockito.mock(ExcelResult.class);
            when(excelResult.getList()).thenReturn(List.of());
            excelUtilMock.when(() -> ExcelUtil.importExcel(any(java.io.InputStream.class), eq(StandardAddressImportVo.class), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    ExcelListener<StandardAddressImportVo> listener = invocation.getArgument(2);
                    assertNotNull(listener.getExcelResult());
                    return excelResult;
                });
            loginHelperMock.when(LoginHelper::getUsername).thenReturn("tester");
            when(standardAddressService.importStandardAddressData(List.of(), false, "tester", "标准地址导入.xlsx")).thenReturn(resultVo);

            controller.importStandardAddressData(multipartFile, false);
        }
    }

    private List<StandardAddressVo> buildRows(int size, int startIndex) {
        List<StandardAddressVo> rows = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            StandardAddressVo vo = new StandardAddressVo();
            vo.setSegmId("SEG" + (startIndex + index));
            vo.setStandName("标准地址" + (startIndex + index));
            rows.add(vo);
        }
        return rows;
    }
}
