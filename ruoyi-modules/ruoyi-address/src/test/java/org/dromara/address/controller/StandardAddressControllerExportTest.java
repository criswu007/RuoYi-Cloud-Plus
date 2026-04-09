package org.dromara.address.controller;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressImportResultVo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.search.service.StandardAddressSearchExportService;
import org.dromara.address.service.IStandardAddressService;
import org.dromara.common.core.service.DictService;
import org.dromara.common.excel.core.DropDownOptions;
import org.dromara.common.excel.core.ExcelListener;
import org.dromara.common.excel.core.ExcelResult;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.excel.utils.ExcelWriterWrapper;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    @Test
    void shouldExportImportTemplateWorkbookWithLatestHeadersAndDropdownColumns() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        List<DropDownOptions> options = List.of(
            new DropDownOptions(0, List.of("建筑、楼栋")),
            new DropDownOptions(1, List.of("是", "否")),
            new DropDownOptions(7, List.of("FTTH_双纤")),
            new DropDownOptions(8, List.of("1G-PON")),
            new DropDownOptions(9, List.of("城区")),
            new DropDownOptions(10, List.of("普通住宅")),
            new DropDownOptions(11, List.of("是", "否"))
        );
        when(standardAddressService.listStandardAddressImportTemplateOptions()).thenReturn(options);

        try (var springUtilMock = org.mockito.Mockito.mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean(DictService.class)).thenReturn(org.mockito.Mockito.mock(DictService.class));
            controller.downloadStandardAddressImportTemplate(response);
        }

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertEquals("分段地址类型", header.getCell(0).getStringCellValue());
            assertEquals("是否城区", header.getCell(1).getStringCellValue());
            assertEquals("父级地址", header.getCell(2).getStringCellValue());
            assertEquals("当级名称", header.getCell(3).getStringCellValue());
            assertEquals("所属维修管理站", header.getCell(4).getStringCellValue());
            assertEquals("所属安装管理站", header.getCell(5).getStringCellValue());
            assertEquals("所属营业管理站", header.getCell(6).getStringCellValue());
            assertEquals("接入方式", header.getCell(7).getStringCellValue());
            assertEquals("接入能力", header.getCell(8).getStringCellValue());
            assertEquals("城乡属性", header.getCell(9).getStringCellValue());
            assertEquals("房屋属性", header.getCell(10).getStringCellValue());
            assertEquals("是否配套小区", header.getCell(11).getStringCellValue());
            assertEquals("覆盖户数", header.getCell(12).getStringCellValue());
            assertEquals("工程编号", header.getCell(13).getStringCellValue());

            assertEquals(24f, header.getHeightInPoints());
            assertEquals(22f, sheet.getDefaultRowHeightInPoints());
            assertEquals(18 * 256, sheet.getColumnWidth(0));
            assertEquals(12 * 256, sheet.getColumnWidth(1));
            assertEquals(48 * 256, sheet.getColumnWidth(2));
            assertEquals(20 * 256, sheet.getColumnWidth(3));
            assertEquals(22 * 256, sheet.getColumnWidth(4));
            assertEquals(22 * 256, sheet.getColumnWidth(5));
            assertEquals(22 * 256, sheet.getColumnWidth(6));
            assertEquals(16 * 256, sheet.getColumnWidth(7));
            assertEquals(16 * 256, sheet.getColumnWidth(8));
            assertEquals(16 * 256, sheet.getColumnWidth(9));
            assertEquals(16 * 256, sheet.getColumnWidth(10));
            assertEquals(16 * 256, sheet.getColumnWidth(11));
            assertEquals(12 * 256, sheet.getColumnWidth(12));
            assertEquals(20 * 256, sheet.getColumnWidth(13));

            Set<Integer> validatedColumns = new HashSet<>();
            for (DataValidation validation : sheet.getDataValidations()) {
                for (var region : validation.getRegions().getCellRangeAddresses()) {
                    for (int column = region.getFirstColumn(); column <= region.getLastColumn(); column++) {
                        validatedColumns.add(column);
                    }
                }
            }
            assertTrue(validatedColumns.containsAll(Set.of(0, 1, 7, 8, 9, 10, 11)));
        }
    }

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
