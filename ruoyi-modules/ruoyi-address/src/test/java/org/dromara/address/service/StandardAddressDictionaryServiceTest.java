package org.dromara.address.service;

import org.dromara.address.domain.PubRestriction;
import org.dromara.address.domain.SegmAddrType;
import org.dromara.address.domain.SpcStation;
import org.dromara.address.domain.bo.StandardAddressAdminBo;
import org.dromara.address.mapper.PubRestrictionMapper;
import org.dromara.address.mapper.SegmAddrTypeMapper;
import org.dromara.address.mapper.SpcStationMapper;
import org.dromara.address.support.AddressRegionContext;
import org.dromara.address.service.impl.StandardAddressDictionaryService;
import org.dromara.common.excel.core.DropDownOptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanWrapperImpl;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressDictionaryServiceTest {

    @Mock
    private SegmAddrTypeMapper segmAddrTypeMapper;

    @Mock
    private PubRestrictionMapper pubRestrictionMapper;

    @Mock
    private SpcStationMapper spcStationMapper;

    @Mock
    private AddressRegionContext addressRegionContext;

    @InjectMocks
    private StandardAddressDictionaryService dictionaryService;

    @Test
    void shouldResolveLevelIdAndAddrLevelBySegmType() {
        var building = new org.dromara.address.domain.SegmAddrType();
        building.setAddrTypeId("180003");
        building.setLevelId(30);

        when(segmAddrTypeMapper.selectList(any())).thenReturn(List.of(building));

        assertEquals(30, dictionaryService.resolveLevelId("180003"));
        assertEquals(1, dictionaryService.resolveAddrLevel("180003"));
    }

    @Test
    void shouldListLevelOptionsOrderedByLevelIdAndAddrTypeId() throws Exception {
        var room = new org.dromara.address.domain.SegmAddrType();
        room.setAddrTypeId("180007");
        room.setAddrTypeName("房间号");
        room.setLevelId(160);

        var province = new org.dromara.address.domain.SegmAddrType();
        province.setAddrTypeId("180000");
        province.setAddrTypeName("省、自治区");
        province.setLevelId(10);

        when(segmAddrTypeMapper.selectList(any())).thenReturn(List.of(room, province));

        Method method = StandardAddressDictionaryService.class.getMethod("listLevelOptions");
        List<?> result = (List<?>) method.invoke(dictionaryService);

        assertEquals(2, result.size());

        BeanWrapperImpl first = new BeanWrapperImpl(result.get(0));
        BeanWrapperImpl second = new BeanWrapperImpl(result.get(1));

        assertEquals("180000", first.getPropertyValue("addrTypeId"));
        assertEquals("省、自治区", first.getPropertyValue("name"));
        assertEquals(1, first.getPropertyValue("addrLevel"));
        assertEquals(10, first.getPropertyValue("levelId"));
        assertEquals("180007", second.getPropertyValue("addrTypeId"));
        assertEquals(2, second.getPropertyValue("addrLevel"));
        assertTrue(((Integer) second.getPropertyValue("levelId")) > ((Integer) first.getPropertyValue("levelId")));
        verify(segmAddrTypeMapper, times(1)).selectList(any());
    }

    @Test
    void shouldGroupStandardAddressFormOptionsByRestrictionKeyword() {
        PubRestriction status = new PubRestriction();
        status.setSerialNo("2140900");
        status.setKeyword("ADDR_SEGM_STATUS");
        status.setDescChina("有效");

        PubRestriction ftth = new PubRestriction();
        ftth.setSerialNo("2140760");
        ftth.setKeyword("ADDR_IN_TYPE_FTTH");
        ftth.setDescChina("FTTH_双纤");

        when(pubRestrictionMapper.selectStandardAddressFormRestrictions()).thenReturn(List.of(ftth, status));

        var result = dictionaryService.listFormOptions();

        assertEquals(1, result.getStatusOptions().size());
        assertEquals("2140900", result.getStatusOptions().get(0).getValue());
        assertEquals("有效", result.getStatusOptions().get(0).getLabel());
        assertEquals(1, result.getAddrInTypeFtthOptions().size());
        assertEquals("2140760", result.getAddrInTypeFtthOptions().get(0).getValue());
        assertEquals("FTTH_双纤", result.getAddrInTypeFtthOptions().get(0).getLabel());
        assertTrue(result.getAreaTypeOptions().isEmpty());
    }

    @Test
    void shouldFallbackToCurrentUserRegionWhenListingStationOptions() {
        StandardAddressAdminBo.StationOptionQueryBo bo = new StandardAddressAdminBo.StationOptionQueryBo();
        bo.setManageType("2017101");
        bo.setKeyword("洪武");
        bo.setLimit(10);

        SpcStation station = new SpcStation();
        station.setStationId("ST320100WX001");
        station.setStationName("洪武路维修站");
        station.setRegionId("320100");
        station.setManageType("2017101");

        when(addressRegionContext.resolveRegionId(null)).thenReturn("320100");
        when(spcStationMapper.selectStationOptions("320100", "2017101", "洪武")).thenReturn(List.of(station));

        var result = dictionaryService.listStationOptions(bo);

        assertEquals(1, result.size());
        assertEquals("320100", result.get(0).getRegionId());
        verify(addressRegionContext).resolveRegionId(null);
        verify(spcStationMapper).selectStationOptions(eq("320100"), eq("2017101"), eq("洪武"));
    }

    @Test
    void shouldBuildImportTemplateOptionsFromDynamicDictionaries() {
        SegmAddrType building = new SegmAddrType();
        building.setAddrTypeId("180010");
        building.setAddrTypeName("建筑、楼栋");
        building.setLevelId(100);

        PubRestriction ftth = new PubRestriction();
        ftth.setKeyword("ADDR_IN_TYPE_FTTH");
        ftth.setDescChina("FTTH_双纤");
        ftth.setSerialNo("2140760");

        PubRestriction lan = new PubRestriction();
        lan.setKeyword("ADDR_IN_TYPE_LAN");
        lan.setDescChina("LAN");
        lan.setSerialNo("2140784");

        PubRestriction pon = new PubRestriction();
        pon.setKeyword("FTTH_PON_TYPE");
        pon.setDescChina("1G-PON");
        pon.setSerialNo("2141301");

        PubRestriction areaType = new PubRestriction();
        areaType.setKeyword("AREA_TYPE");
        areaType.setDescChina("城区");
        areaType.setSerialNo("2140511");

        PubRestriction placeType = new PubRestriction();
        placeType.setKeyword("ADDR_PLACE_TYPE");
        placeType.setDescChina("普通住宅");
        placeType.setSerialNo("2140800");

        when(segmAddrTypeMapper.selectList(any())).thenReturn(List.of(building));
        when(pubRestrictionMapper.selectStandardAddressFormRestrictions()).thenReturn(List.of(ftth, lan, pon, areaType, placeType));

        List<DropDownOptions> result = dictionaryService.listImportTemplateOptions();

        assertEquals(7, result.size());
        assertEquals(0, result.get(0).getIndex());
        assertEquals(List.of("建筑、楼栋"), result.get(0).getOptions());
        assertEquals(1, result.get(1).getIndex());
        assertEquals(List.of("是", "否"), result.get(1).getOptions());
        assertEquals(List.of("FTTH_双纤"), result.get(2).getOptions());
        assertFalse(result.get(2).getOptions().contains("LAN"));
        assertEquals(11, result.get(6).getIndex());
        assertEquals(List.of("是", "否"), result.get(6).getOptions());
    }

    @Test
    void shouldResolveImportMappingsAndPickFirstMatchedStation() {
        SegmAddrType building = new SegmAddrType();
        building.setAddrTypeId("180010");
        building.setAddrTypeName("建筑、楼栋");
        building.setLevelId(100);

        PubRestriction ftth = new PubRestriction();
        ftth.setKeyword("ADDR_IN_TYPE_FTTH");
        ftth.setDescChina("FTTH_双纤");
        ftth.setSerialNo("2140760");

        SpcStation first = new SpcStation();
        first.setStationId("WX001");
        first.setStationName("洪武路维修站");
        first.setRegionId("320100");
        first.setManageType("2017101");

        SpcStation second = new SpcStation();
        second.setStationId("WX002");
        second.setStationName("洪武门维修站");
        second.setRegionId("320100");
        second.setManageType("2017101");

        when(segmAddrTypeMapper.selectList(any())).thenReturn(List.of(building));
        when(pubRestrictionMapper.selectStandardAddressFormRestrictions()).thenReturn(List.of(ftth));
        when(spcStationMapper.selectStationOptions("320100", "2017101", "洪武")).thenReturn(List.of(first, second));

        assertEquals("180010", dictionaryService.resolveSegmTypeByName("建筑、楼栋"));
        assertEquals("2140760", dictionaryService.resolveRestrictionValue("ADDR_IN_TYPE_FTTH", "FTTH_双纤"));
        assertEquals("WX001", dictionaryService.matchStationId("320100", "2017101", "洪武"));
        assertEquals(null, dictionaryService.matchStationId("320100", "2017101", "不存在"));
    }

    @Test
    void shouldFallbackUnitTypeWhenResolvingPlaceTypeImportOptions() {
        PubRestriction unitType = new PubRestriction();
        unitType.setKeyword("ADDR_UNIT_TYPE");
        unitType.setDescChina("普通住宅");
        unitType.setSerialNo("2140800");

        when(pubRestrictionMapper.selectStandardAddressFormRestrictions()).thenReturn(List.of(unitType));

        List<DropDownOptions> result = dictionaryService.listImportTemplateOptions();

        assertTrue(result.stream()
            .filter(item -> item.getIndex() == 10)
            .findFirst()
            .orElseThrow()
            .getOptions()
            .contains("普通住宅"));
        assertEquals("2140800", dictionaryService.resolveRestrictionValue("ADDR_PLACE_TYPE", "普通住宅"));
    }
}
