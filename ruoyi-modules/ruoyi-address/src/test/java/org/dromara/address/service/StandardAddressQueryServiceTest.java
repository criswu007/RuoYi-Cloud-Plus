package org.dromara.address.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.SpcStation;
import org.dromara.address.domain.StandardAddressTag;
import org.dromara.address.domain.StandardAddressTagRel;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.SpcRegionMapper;
import org.dromara.address.mapper.SpcStationMapper;
import org.dromara.address.mapper.StandardAddressTagMapper;
import org.dromara.address.mapper.StandardAddressTagRelMapper;
import org.dromara.address.support.AddressRegionContext;
import org.dromara.address.service.impl.StandardAddressDictionaryService;
import org.dromara.address.service.impl.StandardAddressQueryService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Mock
    private SpcStationMapper spcStationMapper;

    @Mock
    private StandardAddressTagRelMapper standardAddressTagRelMapper;

    @Mock
    private StandardAddressTagMapper standardAddressTagMapper;

    private StandardAddressQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new StandardAddressQueryService(
            addrSegmMapper,
            spcRegionMapper,
            dictionaryService,
            addressRegionContext,
            spcStationMapper,
            standardAddressTagRelMapper,
            standardAddressTagMapper
        );
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
    void shouldIgnoreExplicitSortWhenReadonlyRegionPageQueryIsRequested() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setSegmType("180001");
        PageQuery pageQuery = new PageQuery(20, 1);
        pageQuery.setOrderByColumn("createDate");
        pageQuery.setIsAsc("desc");
        when(dictionaryService.resolveReadonlyRegionAddrLevel(eq("180001"))).thenReturn(2);
        when(spcRegionMapper.selectStandardAddressPage(any(), any())).thenReturn(new Page<>(1, 20, 0));

        queryService.queryPageList(bo, pageQuery);

        ArgumentCaptor<Page<StandardAddressVo>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(spcRegionMapper, times(1)).selectStandardAddressPage(pageCaptor.capture(), any());
        Page<StandardAddressVo> capturedPage = pageCaptor.getValue();
        assertNotNull(capturedPage);
        assertTrue(capturedPage.orders().isEmpty(), "一二级标准地址分页查询不应继续携带前端传入的排序字段");
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

    @Test
    void shouldResolveReadonlyRegionFieldsByGradeIdInsteadOfParentRelation() throws Exception {
        StandardAddressVo region = new StandardAddressVo();
        region.setSegmId("320100");
        region.setParentSegmId(null);
        region.setSegmName("南京市");
        region.setStandName("南京市");
        region.setSegmNo("320100");
        Method setGradeId = StandardAddressVo.class.getMethod("setGradeId", Integer.class);
        setGradeId.invoke(region, 2000004);

        when(addrSegmMapper.selectStandardAddressBySegmId("320100")).thenReturn(null);
        when(spcRegionMapper.selectStandardAddressByRegionId("320100")).thenReturn(region);
        when(dictionaryService.resolveLevelIdMap(List.of("180001"))).thenReturn(java.util.Map.of("180001", 20));
        when(dictionaryService.resolveAddrLevelMap(List.of("180001"))).thenReturn(java.util.Map.of("180001", 2));

        StandardAddressVo vo = queryService.getBySegmId("320100", null);

        assertNotNull(vo);
        assertEquals("180001", vo.getSegmType());
        assertEquals(2, vo.getAddrLevel());
    }

    @Test
    void shouldEnrichStationNamesAndTagNamesWhenLoadingAddrSegmDetail() {
        StandardAddressVo detail = new StandardAddressVo();
        detail.setSegmId("A1001");
        detail.setParentSegmId("P1001");
        detail.setStandName("江苏省南京市鼓楼区中央路100号");
        detail.setSegmName("中央路100号");
        detail.setSegmType("180010");
        detail.setStationId("ST-WX-001");
        detail.setInstallStationId("ST-AZ-001");
        detail.setBusStationId("ST-BY-001");

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("P1001");
        parent.setStandName("江苏省南京市鼓楼区中央路");

        SpcStation repairStation = new SpcStation();
        repairStation.setStationId("ST-WX-001");
        repairStation.setStationName("洪武路维修站");
        SpcStation installStation = new SpcStation();
        installStation.setStationId("ST-AZ-001");
        installStation.setStationName("鼓楼安装站");
        SpcStation businessStation = new SpcStation();
        businessStation.setStationId("ST-BY-001");
        businessStation.setStationName("中央门营业站");

        StandardAddressTagRel firstRel = new StandardAddressTagRel();
        firstRel.setStandardAddressId("A1001");
        firstRel.setTagId(11L);
        StandardAddressTagRel secondRel = new StandardAddressTagRel();
        secondRel.setStandardAddressId("A1001");
        secondRel.setTagId(12L);

        StandardAddressTag firstTag = new StandardAddressTag();
        firstTag.setId(11L);
        firstTag.setName("高价值");
        StandardAddressTag secondTag = new StandardAddressTag();
        secondTag.setId(12L);
        secondTag.setName("商客重点");

        when(addrSegmMapper.selectStandardAddressBySegmId("A1001")).thenReturn(detail);
        when(addrSegmMapper.selectByIds(any())).thenReturn(List.of(parent));
        when(spcRegionMapper.selectByIds(any())).thenReturn(List.of());
        when(dictionaryService.resolveLevelIdMap(List.of("180010"))).thenReturn(java.util.Map.of("180010", 150));
        when(dictionaryService.resolveAddrLevelMap(List.of("180010"))).thenReturn(java.util.Map.of("180010", 15));
        when(spcStationMapper.selectByIds(any())).thenReturn(List.of(repairStation, installStation, businessStation));
        when(standardAddressTagRelMapper.selectList(any())).thenReturn(List.of(firstRel, secondRel));
        when(standardAddressTagMapper.selectBatchIds(List.of(11L, 12L))).thenReturn(List.of(firstTag, secondTag));

        StandardAddressVo vo = queryService.getBySegmId("A1001", null);

        assertNotNull(vo);
        assertEquals("洪武路维修站", vo.getStationName());
        assertEquals("鼓楼安装站", vo.getInstallStationName());
        assertEquals("中央门营业站", vo.getBusStationName());
        assertEquals(List.of("高价值", "商客重点"), vo.getTagNames());
        assertEquals("江苏省南京市鼓楼区中央路", vo.getParentStandName());
    }
}
