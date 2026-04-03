package org.dromara.address.service;

import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.SpcRegion;
import org.dromara.address.domain.StandardAddressImportFailDetail;
import org.dromara.address.domain.StandardAddressImportRecord;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressImportResultVo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.SpcRegionMapper;
import org.dromara.address.mapper.StandardAddressImportFailDetailMapper;
import org.dromara.address.mapper.StandardAddressImportRecordMapper;
import org.dromara.address.support.StandardAddressOperationLogRecorder;
import org.dromara.address.service.impl.StandardAddressCommandService;
import org.dromara.address.service.impl.StandardAddressDictionaryService;
import org.dromara.address.service.impl.StandardAddressImportService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressImportServiceTest {

    @Mock
    private StandardAddressCommandService commandService;

    @Mock
    private StandardAddressDictionaryService dictionaryService;

    @Mock
    private AddrSegmMapper addrSegmMapper;

    @Mock
    private SpcRegionMapper spcRegionMapper;

    @Mock
    private StandardAddressImportRecordMapper importRecordMapper;

    @Mock
    private StandardAddressImportFailDetailMapper importFailDetailMapper;

    @Mock
    private StandardAddressOperationLogRecorder operationLogRecorder;

    @InjectMocks
    private StandardAddressImportService importService;

    @Test
    void shouldPersistBatchSummaryAndFailureDetailsWhenPartialFailuresOccur() {
        StandardAddressImportVo successRow = new StandardAddressImportVo();
        successRow.setParentStandName("江苏省南京市鼓楼区中央路");
        successRow.setSegmName("紫峰大厦");
        successRow.setAddrLevel(9);
        successRow.setStatus("2140900");

        StandardAddressImportVo failRow = new StandardAddressImportVo();
        failRow.setParentStandName("江苏省南京市");
        failRow.setSegmName("江苏省");
        failRow.setAddrLevel(1);

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("parent-1");
        parent.setStandName("江苏省南京市鼓楼区中央路");

        when(addrSegmMapper.selectActiveByStandName("江苏省南京市鼓楼区中央路")).thenReturn(parent);
        when(addrSegmMapper.selectActiveByParentAndSegmName("parent-1", "紫峰大厦")).thenReturn(null);
        when(dictionaryService.resolveDefaultSegmTypeByAddrLevel(9)).thenReturn("180005");
        when(commandService.addStandardAddressForImport(any(StandardAddressBo.class))).thenAnswer(invocation -> {
            StandardAddressBo bo = invocation.getArgument(0);
            bo.setSegmId("segm-new");
            bo.setStandName("江苏省南京市鼓楼区中央路紫峰大厦");
            return true;
        });
        when(importRecordMapper.insert(any(StandardAddressImportRecord.class))).thenReturn(1);
        when(importFailDetailMapper.insert(any(StandardAddressImportFailDetail.class))).thenReturn(1);

        StandardAddressImportResultVo result = importService.importStandardAddressData(
            List.of(successRow, failRow), false, "tester", "demo.xlsx");

        assertNotNull(result.getBatchId());
        assertEquals("demo.xlsx", result.getFileName());
        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertEquals("2", result.getStatus());
        assertTrue(Boolean.TRUE.equals(result.getFailureExportable()));

        ArgumentCaptor<StandardAddressImportRecord> recordCaptor = ArgumentCaptor.forClass(StandardAddressImportRecord.class);
        verify(importRecordMapper).insert(recordCaptor.capture());
        assertEquals(2, recordCaptor.getValue().getTotalCount());
        assertEquals(1, recordCaptor.getValue().getSuccessCount());
        assertEquals(1, recordCaptor.getValue().getFailCount());

        ArgumentCaptor<StandardAddressImportFailDetail> failCaptor = ArgumentCaptor.forClass(StandardAddressImportFailDetail.class);
        verify(importFailDetailMapper).insert(failCaptor.capture());
        assertEquals(2, failCaptor.getValue().getRowNum());
        assertEquals("江苏省", failCaptor.getValue().getSegmName());
        verify(operationLogRecorder).record(
            eq("segm-new"),
            eq("IMPORT"),
            eq("江苏省南京市鼓楼区中央路紫峰大厦"),
            contains("导入新增标准地址成功")
        );
    }

    @Test
    void shouldUpdateExistingAddressWhenUpdateSupportEnabled() {
        StandardAddressImportVo updateRow = new StandardAddressImportVo();
        updateRow.setParentStandName("江苏省南京市鼓楼区中央路");
        updateRow.setSegmName("紫峰大厦");
        updateRow.setAddrLevel(9);
        updateRow.setStatus("2140900");
        updateRow.setNotes("更新备注");

        SpcRegion regionParent = new SpcRegion();
        regionParent.setRegionId("320100");
        regionParent.setRegionName("江苏省南京市鼓楼区中央路");

        AddrSegm existing = new AddrSegm();
        existing.setSegmId("segm-existing");
        existing.setParentSegmId("320100");
        existing.setSegmName("紫峰大厦");
        existing.setSegmType("180005");
        existing.setDeleteState("0");

        when(addrSegmMapper.selectActiveByStandName("江苏省南京市鼓楼区中央路")).thenReturn(null);
        when(spcRegionMapper.selectActiveByRegionName("江苏省南京市鼓楼区中央路")).thenReturn(regionParent);
        when(addrSegmMapper.selectActiveByParentAndSegmName("320100", "紫峰大厦")).thenReturn(existing);
        when(dictionaryService.resolveDefaultSegmTypeByAddrLevel(9)).thenReturn("180005");
        when(commandService.updateStandardAddressForImport(any(StandardAddressBo.class))).thenAnswer(invocation -> {
            StandardAddressBo bo = invocation.getArgument(0);
            bo.setStandName("江苏省南京市鼓楼区中央路紫峰大厦");
            return true;
        });
        when(importRecordMapper.insert(any(StandardAddressImportRecord.class))).thenReturn(1);

        StandardAddressImportResultVo result = importService.importStandardAddressData(
            List.of(updateRow), true, "tester", "update.xlsx");

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertEquals("1", result.getStatus());
        verify(commandService).updateStandardAddressForImport(any(StandardAddressBo.class));
        verify(commandService, never()).addStandardAddressForImport(any(StandardAddressBo.class));
        verify(importFailDetailMapper, never()).insert(any(StandardAddressImportFailDetail.class));
        verify(addrSegmMapper).selectActiveByParentAndSegmName(eq("320100"), eq("紫峰大厦"));
        verify(operationLogRecorder).record(
            eq("segm-existing"),
            eq("IMPORT"),
            eq("江苏省南京市鼓楼区中央路紫峰大厦"),
            contains("导入更新标准地址成功")
        );
    }
}
