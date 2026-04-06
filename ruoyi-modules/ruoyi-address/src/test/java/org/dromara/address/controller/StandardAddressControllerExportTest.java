package org.dromara.address.controller;

import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.search.service.StandardAddressSearchExportService;
import org.dromara.address.service.IStandardAddressService;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.excel.utils.ExcelWriterWrapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
