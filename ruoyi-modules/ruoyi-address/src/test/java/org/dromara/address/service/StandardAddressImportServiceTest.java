package org.dromara.address.service;

import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.SpcRegion;
import org.dromara.address.domain.StandardAddressImportBatch;
import org.dromara.address.domain.StandardAddressImportDetail;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressImportResultVo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.SpcRegionMapper;
import org.dromara.address.mapper.StandardAddressImportBatchMapper;
import org.dromara.address.mapper.StandardAddressImportDetailMapper;
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
    private StandardAddressImportBatchMapper importBatchMapper;

    @Mock
    private StandardAddressImportDetailMapper importDetailMapper;

    @Mock
    private StandardAddressOperationLogRecorder operationLogRecorder;

    @InjectMocks
    private StandardAddressImportService importService;

    @Test
    void shouldPersistBatchSummaryAndFailureDetailsWhenPartialFailuresOccur() {
        StandardAddressImportVo successRow = new StandardAddressImportVo();
        successRow.setParentStandName("江苏省南京市鼓楼区中央路");
        successRow.setSegmName("紫峰大厦");
        successRow.setSegmTypeName("建筑、楼栋");

        StandardAddressImportVo failRow = new StandardAddressImportVo();
        failRow.setParentStandName("不存在的父级地址");
        failRow.setSegmName("失败地址");
        failRow.setSegmTypeName("建筑、楼栋");

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("parent-1");
        parent.setStandName("江苏省南京市鼓楼区中央路");
        parent.setRegionId("320100");

        when(addrSegmMapper.selectActiveByStandName("江苏省南京市鼓楼区中央路")).thenReturn(parent);
        when(addrSegmMapper.selectActiveByStandName("不存在的父级地址")).thenReturn(null);
        when(spcRegionMapper.selectActiveByRegionName("不存在的父级地址")).thenReturn(null);
        when(addrSegmMapper.selectActiveByParentAndSegmName("parent-1", "紫峰大厦")).thenReturn(null);
        when(dictionaryService.resolveSegmTypeByName("建筑、楼栋")).thenReturn("180005");
        when(dictionaryService.resolveAddrLevel("180005")).thenReturn(9);
        when(commandService.addStandardAddressForImport(any(StandardAddressBo.class))).thenAnswer(invocation -> {
            StandardAddressBo bo = invocation.getArgument(0);
            bo.setSegmId("segm-new");
            bo.setStandName("江苏省南京市鼓楼区中央路紫峰大厦");
            return true;
        });
        when(importBatchMapper.insert(any(StandardAddressImportBatch.class))).thenReturn(1);
        when(importDetailMapper.insert(any(StandardAddressImportDetail.class))).thenReturn(1);

        StandardAddressImportResultVo result = importService.importStandardAddressData(
            List.of(successRow, failRow), false, "tester", "demo.xlsx");

        assertNotNull(result.getBatchId());
        assertEquals("demo.xlsx", result.getFileName());
        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertEquals("2", result.getStatus());
        assertTrue(Boolean.TRUE.equals(result.getFailureExportable()));

        ArgumentCaptor<StandardAddressImportBatch> recordCaptor = ArgumentCaptor.forClass(StandardAddressImportBatch.class);
        verify(importBatchMapper).insert(recordCaptor.capture());
        assertEquals(2, recordCaptor.getValue().getTotalCount());
        assertEquals(1, recordCaptor.getValue().getSuccessCount());
        assertEquals(1, recordCaptor.getValue().getFailCount());

        ArgumentCaptor<StandardAddressImportDetail> failCaptor = ArgumentCaptor.forClass(StandardAddressImportDetail.class);
        verify(importDetailMapper).insert(failCaptor.capture());
        assertEquals(2, failCaptor.getValue().getRowNum());
        assertEquals("失败地址", failCaptor.getValue().getSegmName());
        verify(commandService).addStandardAddressForImport(any(StandardAddressBo.class));
        verify(commandService, never()).updateStandardAddressForImport(any(StandardAddressBo.class));
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
        updateRow.setSegmTypeName("建筑、楼栋");
        updateRow.setSingleProjectCode("GC-2026-001");

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
        when(dictionaryService.resolveSegmTypeByName("建筑、楼栋")).thenReturn("180005");
        when(dictionaryService.resolveAddrLevel("180005")).thenReturn(9);
        when(commandService.updateStandardAddressForImport(any(StandardAddressBo.class))).thenAnswer(invocation -> {
            StandardAddressBo bo = invocation.getArgument(0);
            bo.setStandName("江苏省南京市鼓楼区中央路紫峰大厦");
            return true;
        });
        when(importBatchMapper.insert(any(StandardAddressImportBatch.class))).thenReturn(1);

        StandardAddressImportResultVo result = importService.importStandardAddressData(
            List.of(updateRow), true, "tester", "update.xlsx");

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertEquals("1", result.getStatus());
        verify(commandService).updateStandardAddressForImport(any(StandardAddressBo.class));
        verify(commandService, never()).addStandardAddressForImport(any(StandardAddressBo.class));
        verify(importDetailMapper, never()).insert(any(StandardAddressImportDetail.class));
        verify(addrSegmMapper).selectActiveByParentAndSegmName(eq("320100"), eq("紫峰大厦"));
        verify(operationLogRecorder).record(
            eq("segm-existing"),
            eq("IMPORT"),
            eq("江苏省南京市鼓楼区中央路紫峰大厦"),
            contains("导入更新标准地址成功")
        );
    }

    @Test
    void shouldMapLatestTemplateFieldsAndFallbackCoverNumToOne() {
        StandardAddressImportVo row = new StandardAddressImportVo();
        row.setParentStandName("江苏省南京市鼓楼区中央路");
        row.setSegmName("紫峰大厦");
        row.setSegmTypeName("建筑、楼栋");
        row.setIsCityLabel("是");
        row.setMaintenanceStationName("洪武路维修站");
        row.setInstallStationName("不存在的安装站");
        row.setBusinessStationName("中央路营业站");
        row.setAccessModeName("FTTH_双纤");
        row.setAccessCapabilityName("1G-PON");
        row.setAreaTypeName("城区");
        row.setPlaceTypeName("普通住宅");
        row.setSupportingFeeCommunityLabel("否");
        row.setCoverNumText("abc");
        row.setSingleProjectCode("GC-2026-001");

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("parent-1");
        parent.setStandName("江苏省南京市鼓楼区中央路");
        parent.setRegionId("320100");

        when(addrSegmMapper.selectActiveByStandName("江苏省南京市鼓楼区中央路")).thenReturn(parent);
        when(addrSegmMapper.selectActiveByParentAndSegmName("parent-1", "紫峰大厦")).thenReturn(null);
        when(dictionaryService.resolveSegmTypeByName("建筑、楼栋")).thenReturn("180005");
        when(dictionaryService.resolveAddrLevel("180005")).thenReturn(9);
        when(dictionaryService.resolveRestrictionValue("ADDR_IN_TYPE_FTTH", "FTTH_双纤")).thenReturn("2140760");
        when(dictionaryService.resolveRestrictionValue("FTTH_PON_TYPE", "1G-PON")).thenReturn("2141301");
        when(dictionaryService.resolveRestrictionValue("AREA_TYPE", "城区")).thenReturn("2140511");
        when(dictionaryService.resolveRestrictionValue("ADDR_PLACE_TYPE", "普通住宅")).thenReturn("2140800");
        when(dictionaryService.matchStationId("320100", "2017101", "洪武路维修站")).thenReturn("WX001");
        when(dictionaryService.matchStationId("320100", "2017102", "不存在的安装站")).thenReturn(null);
        when(dictionaryService.matchStationId("320100", "2017103", "中央路营业站")).thenReturn("BUS001");
        when(commandService.addStandardAddressForImport(any(StandardAddressBo.class))).thenAnswer(invocation -> {
            StandardAddressBo bo = invocation.getArgument(0);
            bo.setSegmId("segm-new");
            bo.setStandName("江苏省南京市鼓楼区中央路紫峰大厦");
            return true;
        });
        when(importBatchMapper.insert(any(StandardAddressImportBatch.class))).thenReturn(1);

        StandardAddressImportResultVo result = importService.importStandardAddressData(
            List.of(row), false, "tester", "latest-template.xlsx");

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailCount());

        ArgumentCaptor<StandardAddressBo> boCaptor = ArgumentCaptor.forClass(StandardAddressBo.class);
        verify(commandService).addStandardAddressForImport(boCaptor.capture());
        StandardAddressBo bo = boCaptor.getValue();
        assertEquals("parent-1", bo.getParentSegmId());
        assertEquals("紫峰大厦", bo.getSegmName());
        assertEquals(9, bo.getAddrLevel());
        assertEquals("180005", bo.getSegmType());
        assertEquals("Y", bo.getIsCity());
        assertEquals("WX001", bo.getStationId());
        assertEquals(null, bo.getInstallStationId());
        assertEquals("BUS001", bo.getBusStationId());
        assertEquals(2140760, bo.getAddrInTypeFtth());
        assertEquals(2141301, bo.getFtthPonType());
        assertEquals(2140511, bo.getAreaType());
        assertEquals(2140800, bo.getPlaceType());
        assertEquals("N", bo.getSupportingFeeCommunityFlag());
        assertEquals(1, bo.getCoverNum());
        assertEquals("GC-2026-001", bo.getSingleProjectCode());
        assertEquals("2140900", bo.getStatus());
    }

    @Test
    void shouldRejectLanAccessModeDuringImport() {
        StandardAddressImportVo row = new StandardAddressImportVo();
        row.setParentStandName("江苏省南京市鼓楼区中央路");
        row.setSegmName("紫峰大厦");
        row.setSegmTypeName("建筑、楼栋");
        row.setAccessModeName("LAN");

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("parent-1");
        parent.setStandName("江苏省南京市鼓楼区中央路");
        parent.setRegionId("320100");

        when(addrSegmMapper.selectActiveByStandName("江苏省南京市鼓楼区中央路")).thenReturn(parent);
        when(addrSegmMapper.selectActiveByParentAndSegmName("parent-1", "紫峰大厦")).thenReturn(null);
        when(dictionaryService.resolveSegmTypeByName("建筑、楼栋")).thenReturn("180005");
        when(dictionaryService.resolveAddrLevel("180005")).thenReturn(9);
        when(dictionaryService.resolveRestrictionValue("ADDR_IN_TYPE_FTTH", "LAN")).thenReturn(null);
        when(importBatchMapper.insert(any(StandardAddressImportBatch.class))).thenReturn(1);
        when(importDetailMapper.insert(any(StandardAddressImportDetail.class))).thenReturn(1);

        StandardAddressImportResultVo result = importService.importStandardAddressData(
            List.of(row), false, "tester", "lan-import.xlsx");

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        verify(commandService, never()).addStandardAddressForImport(any(StandardAddressBo.class));
        verify(commandService, never()).updateStandardAddressForImport(any(StandardAddressBo.class));
    }

    @Test
    void shouldValidateRowWithoutWritingBusinessDataDuringUploadPhase() {
        StandardAddressImportVo row = new StandardAddressImportVo();
        row.setParentStandName("江苏省南京市鼓楼区中央路");
        row.setSegmName("紫峰大厦");
        row.setSegmTypeName("建筑、楼栋");
        row.setAccessModeName("FTTH_双纤");

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("parent-1");
        parent.setStandName("江苏省南京市鼓楼区中央路");
        parent.setRegionId("320100");

        when(addrSegmMapper.selectActiveByStandName("江苏省南京市鼓楼区中央路")).thenReturn(parent);
        when(addrSegmMapper.selectActiveByParentAndSegmName("parent-1", "紫峰大厦")).thenReturn(null);
        when(dictionaryService.resolveSegmTypeByName("建筑、楼栋")).thenReturn("180005");
        when(dictionaryService.resolveAddrLevel("180005")).thenReturn(9);
        when(dictionaryService.resolveRestrictionValue("ADDR_IN_TYPE_FTTH", "FTTH_双纤")).thenReturn("2140760");

        importService.validateImportRow(row, false);

        verify(commandService, never()).addStandardAddressForImport(any(StandardAddressBo.class));
        verify(commandService, never()).updateStandardAddressForImport(any(StandardAddressBo.class));
        verify(importBatchMapper, never()).insert(any(StandardAddressImportBatch.class));
        verify(importDetailMapper, never()).insert(any(StandardAddressImportDetail.class));
        verify(operationLogRecorder, never()).record(any(), any(), any(), any());
    }

    @Test
    void shouldExecuteApprovedRowWithoutCreatingNewBatchRecord() {
        StandardAddressImportVo row = new StandardAddressImportVo();
        row.setParentStandName("江苏省南京市鼓楼区中央路");
        row.setSegmName("紫峰大厦");
        row.setSegmTypeName("建筑、楼栋");

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("parent-1");
        parent.setStandName("江苏省南京市鼓楼区中央路");
        parent.setRegionId("320100");

        when(addrSegmMapper.selectActiveByStandName("江苏省南京市鼓楼区中央路")).thenReturn(parent);
        when(addrSegmMapper.selectActiveByParentAndSegmName("parent-1", "紫峰大厦")).thenReturn(null);
        when(dictionaryService.resolveSegmTypeByName("建筑、楼栋")).thenReturn("180005");
        when(dictionaryService.resolveAddrLevel("180005")).thenReturn(9);
        when(commandService.addStandardAddressForImport(any(StandardAddressBo.class))).thenAnswer(invocation -> {
            StandardAddressBo bo = invocation.getArgument(0);
            bo.setSegmId("segm-new");
            bo.setStandName("江苏省南京市鼓楼区中央路紫峰大厦");
            return true;
        });

        importService.executeApprovedImportRow(row, 12, false, "tester", "approved.xlsx");

        verify(commandService).addStandardAddressForImport(any(StandardAddressBo.class));
        verify(commandService, never()).updateStandardAddressForImport(any(StandardAddressBo.class));
        verify(importBatchMapper, never()).insert(any(StandardAddressImportBatch.class));
        verify(importDetailMapper, never()).insert(any(StandardAddressImportDetail.class));
        verify(operationLogRecorder).record(
            eq("segm-new"),
            eq("IMPORT"),
            eq("江苏省南京市鼓楼区中央路紫峰大厦"),
            contains("行号：12")
        );
    }
}
