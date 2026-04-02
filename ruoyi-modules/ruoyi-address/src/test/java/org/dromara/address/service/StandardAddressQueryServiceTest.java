package org.dromara.address.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.address.domain.SpcRegion;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.SpcRegionMapper;
import org.dromara.address.support.AddressRegionContext;
import org.dromara.address.service.impl.StandardAddressDictionaryService;
import org.dromara.address.service.impl.StandardAddressQueryService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressQueryServiceTest {

    @Mock
    private AddrSegmMapper addrSegmMapper;

    @Mock
    private SpcRegionMapper spcRegionMapper;

    @Mock
    private StandardAddressDictionaryService dictionaryService;

    @Mock
    private AddressRegionContext addressRegionContext;

    private StandardAddressQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new StandardAddressQueryService(addrSegmMapper, spcRegionMapper, dictionaryService, addressRegionContext);
    }

    @Test
    void shouldRouteLevel1AndLevel2ToSpcRegionPageQuery() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setAddrLevel(1);
        when(dictionaryService.resolveReadonlyRegionAddrLevel(eq(null))).thenReturn(null);
        when(spcRegionMapper.selectStandardAddressPage(any(), any())).thenReturn(new Page<>(1, 20, 0));

        queryService.queryPageList(bo, new PageQuery(20, 1));

        verify(spcRegionMapper).selectStandardAddressPage(any(), any());
        verify(spcRegionMapper, never()).selectStandardAddressList(any());
        verify(addrSegmMapper, never()).selectStandardAddressList(any(), any());
    }

    @Test
    void shouldRouteProvinceAndCityAddrTypeToSpcRegionPageQuery() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setSegmType("180001");
        when(dictionaryService.resolveReadonlyRegionAddrLevel(eq("180001"))).thenReturn(2);
        when(spcRegionMapper.selectStandardAddressPage(any(), any())).thenReturn(new Page<>(1, 20, 0));

        queryService.queryPageList(bo, new PageQuery(20, 1));

        verify(spcRegionMapper).selectStandardAddressPage(any(), any());
        verify(spcRegionMapper, never()).selectStandardAddressList(any());
        verify(addrSegmMapper, never()).selectStandardAddressPage(any(), any(), any());
    }

    @Test
    void shouldUseDatabasePaginationAndAvoidPerRowLevelLookupForAddrSegmRows() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setAddrLevel(3);
        when(dictionaryService.resolveReadonlyRegionAddrLevel(null)).thenReturn(null);
        when(dictionaryService.resolveSegmTypesByAddrLevel(3)).thenReturn(List.of("0301"));

        StandardAddressVo row = new StandardAddressVo();
        row.setSegmId("A1001");
        row.setAddrLevel(3);
        row.setLevelId(30);
        Page<StandardAddressVo> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(row));
        when(addrSegmMapper.selectStandardAddressPage(any(), any(), any())).thenReturn(page);

        queryService.queryPageList(bo, new PageQuery(20, 1));

        verify(addrSegmMapper).selectStandardAddressPage(any(), any(), any());
        verify(addrSegmMapper, never()).selectStandardAddressList(any(), any());
        verify(dictionaryService, never()).resolveLevelId(any());
    }

    @Test
    void shouldFillParentStandNameByBatchLookupAfterAddrSegmPageQuery() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setSegmType("180004");
        when(dictionaryService.resolveReadonlyRegionAddrLevel("180004")).thenReturn(null);

        StandardAddressVo row = new StandardAddressVo();
        row.setSegmId("A1001");
        row.setParentSegmId("P1001");
        row.setSegmType("180004");
        row.setAddrLevel(7);
        row.setLevelId(70);
        Page<StandardAddressVo> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(row));

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("P1001");
        parent.setStandName("江苏省南京市鼓楼区");

        when(addrSegmMapper.selectStandardAddressPage(any(), any(), any())).thenReturn(page);
        when(addrSegmMapper.selectByIds(any())).thenReturn(List.of(parent));
        when(spcRegionMapper.selectByIds(any())).thenReturn(List.of());

        TableDataInfo<StandardAddressVo> result = queryService.queryPageList(bo, new PageQuery(20, 1));

        assertEquals("江苏省南京市鼓楼区", result.getRows().get(0).getParentStandName());
        verify(addrSegmMapper).selectByIds(any());
        verify(spcRegionMapper).selectByIds(any());
    }

    @Test
    void shouldMarkSpcRegionRecordReadonly() {
        StandardAddressVo region = new StandardAddressVo();
        region.setSegmId("320100");
        region.setParentSegmId("320000");
        region.setSegmName("南京市");
        region.setStandName("南京市");
        region.setSegmNo("320100");
        region.setAddrLevel(2);
        region.setLevelId(20);
        when(spcRegionMapper.selectStandardAddressByRegionId("320100")).thenReturn(region);

        StandardAddressVo vo = queryService.getBySegmId("320100", 1);

        assertEquals("320100", vo.getSegmId());
        assertEquals("南京市", vo.getStandName());
        assertTrue(Boolean.TRUE.equals(vo.getReadOnlyFlag()));
        assertFalse(Boolean.TRUE.equals(vo.getCanEdit()));
        assertFalse(Boolean.TRUE.equals(vo.getCanDelete()));
    }
}
