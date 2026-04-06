package org.dromara.address.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import org.dromara.address.config.AddressSearchProperties;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.search.model.SearchAfterBatch;
import org.dromara.address.search.service.StandardAddressSearchExportService;
import org.dromara.address.search.service.StandardAddressSearchGateway;
import org.dromara.address.service.IStandardAddressService;
import org.dromara.address.service.impl.StandardAddressDictionaryService;
import org.dromara.address.support.AddressRegionContext;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressSearchExportServiceTest {

    @Mock
    private StandardAddressSearchGateway standardAddressSearchGateway;

    @Mock
    private IStandardAddressService standardAddressService;

    @Mock
    private StandardAddressDictionaryService dictionaryService;

    @Mock
    private AddressRegionContext addressRegionContext;

    private AddressSearchProperties addressSearchProperties;

    private StandardAddressSearchExportService exportService;

    @BeforeEach
    void setUp() {
        addressSearchProperties = new AddressSearchProperties();
        addressSearchProperties.getStandard().setReadEnabled(Boolean.TRUE);
        exportService = new StandardAddressSearchExportService(
            standardAddressSearchGateway,
            standardAddressService,
            dictionaryService,
            addressRegionContext,
            addressSearchProperties
        );
    }

    @Test
    void shouldStreamExportRowsByPitSearchAfterBatchesWhenAddrSegmEsReadEnabled() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setAddrLevel(7);
        when(dictionaryService.resolveReadonlyRegionAddrLevel(null)).thenReturn(null);
        when(dictionaryService.resolveSegmTypesByAddrLevel(7)).thenReturn(List.of("180010"));
        when(addressRegionContext.resolveRegionId(null)).thenReturn("320100");
        when(standardAddressSearchGateway.openExportPointInTime()).thenReturn("pit-1");

        StandardAddressVo first = buildVo("SEG001");
        StandardAddressVo second = buildVo("SEG002");
        SearchAfterBatch<StandardAddressVo> batch1 = SearchAfterBatch.of(List.of(first), "pit-2", List.of(FieldValue.of("SEG001")), false);
        SearchAfterBatch<StandardAddressVo> batch2 = SearchAfterBatch.of(List.of(second), "pit-2", List.of(FieldValue.of("SEG002")), true);
        when(standardAddressSearchGateway.queryExportBatch(any(), eq(List.of("180010")), eq("pit-1"), isNull(), eq(500))).thenReturn(batch1);
        when(standardAddressSearchGateway.queryExportBatch(
            any(),
            eq(List.of("180010")),
            eq("pit-2"),
            argThat(searchAfter -> searchAfter != null && searchAfter.size() == 1),
            eq(500)
        )).thenReturn(batch2);

        List<String> exportedIds = new ArrayList<>();
        exportService.writeRows(bo, 500, rows -> exportedIds.addAll(rows.stream().map(StandardAddressVo::getSegmId).toList()));

        ArgumentCaptor<StandardAddressBo> boCaptor = ArgumentCaptor.forClass(StandardAddressBo.class);
        verify(standardAddressSearchGateway, times(2)).queryExportBatch(boCaptor.capture(), any(), any(), any(), eq(500));
        assertEquals(List.of("SEG001", "SEG002"), exportedIds);
        assertEquals("320100", boCaptor.getAllValues().get(0).getRegionId());
        verify(standardAddressSearchGateway).openExportPointInTime();
        verify(standardAddressSearchGateway).closeExportPointInTime("pit-2");
        verifyNoInteractions(standardAddressService);
    }

    @Test
    void shouldFallbackToDatabasePagingWhenQueryTargetsReadonlyRegion() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setAddrLevel(2);
        when(dictionaryService.resolveReadonlyRegionAddrLevel(null)).thenReturn(null);
        when(addressRegionContext.resolveRegionId(null)).thenReturn("320100");
        when(standardAddressService.queryStandardAddressPageList(any(), any()))
            .thenReturn(new TableDataInfo<>(List.of(buildVo("REG001")), 501), new TableDataInfo<>(List.of(buildVo("REG002")), 501));

        List<String> exportedIds = new ArrayList<>();
        exportService.writeRows(bo, 500, rows -> exportedIds.addAll(rows.stream().map(StandardAddressVo::getSegmId).toList()));

        ArgumentCaptor<StandardAddressBo> boCaptor = ArgumentCaptor.forClass(StandardAddressBo.class);
        ArgumentCaptor<PageQuery> pageQueryCaptor = ArgumentCaptor.forClass(PageQuery.class);
        verify(standardAddressService, times(2)).queryStandardAddressPageList(boCaptor.capture(), pageQueryCaptor.capture());
        assertEquals(List.of("REG001", "REG002"), exportedIds);
        assertEquals("320100", boCaptor.getValue().getRegionId());
        assertEquals(1, pageQueryCaptor.getAllValues().get(0).getPageNum());
        assertEquals(2, pageQueryCaptor.getAllValues().get(1).getPageNum());
        verifyNoInteractions(standardAddressSearchGateway);
    }

    @Test
    void shouldFallbackToDatabasePagingWhenEsReadSwitchDisabled() {
        addressSearchProperties.getStandard().setReadEnabled(Boolean.FALSE);
        StandardAddressBo bo = new StandardAddressBo();
        bo.setAddrLevel(7);
        when(addressRegionContext.resolveRegionId(null)).thenReturn("320100");
        when(standardAddressService.queryStandardAddressPageList(any(), any()))
            .thenReturn(new TableDataInfo<>(List.of(buildVo("SEG001")), 1));

        List<String> exportedIds = new ArrayList<>();
        exportService.writeRows(bo, 500, rows -> exportedIds.addAll(rows.stream().map(StandardAddressVo::getSegmId).toList()));

        verify(standardAddressService).queryStandardAddressPageList(any(), any());
        assertEquals(List.of("SEG001"), exportedIds);
        verifyNoInteractions(standardAddressSearchGateway);
    }

    private StandardAddressVo buildVo(String segmId) {
        StandardAddressVo vo = new StandardAddressVo();
        vo.setSegmId(segmId);
        vo.setStandName("标准地址" + segmId);
        return vo;
    }
}
