package org.dromara.address.service;

import org.dromara.address.domain.SpcRegion;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.SpcRegionMapper;
import org.dromara.address.service.impl.StandardAddressDictionaryService;
import org.dromara.address.service.impl.StandardAddressQueryService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private StandardAddressQueryService queryService;

    @Test
    void shouldRouteLevel1AndLevel2ToSpcRegion() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setLevelId(1);
        when(spcRegionMapper.selectStandardAddressList(any())).thenReturn(List.of());

        queryService.queryPageList(bo, new PageQuery(20, 1));

        verify(spcRegionMapper).selectStandardAddressList(any());
        verify(addrSegmMapper, never()).selectStandardAddressList(any(), any());
    }

    @Test
    void shouldMarkSpcRegionRecordReadonly() {
        SpcRegion region = new SpcRegion();
        region.setRegionId("320100");
        region.setSuperRegionId("320000");
        region.setRegionName("南京市");
        region.setRegionNo("320100");
        when(spcRegionMapper.selectById("320100")).thenReturn(region);

        StandardAddressVo vo = queryService.getBySegmId("320100", 1);

        assertEquals("320100", vo.getSegmId());
        assertEquals("南京市", vo.getStandName());
        assertTrue(Boolean.TRUE.equals(vo.getReadOnlyFlag()));
        assertFalse(Boolean.TRUE.equals(vo.getCanEdit()));
        assertFalse(Boolean.TRUE.equals(vo.getCanDelete()));
    }
}
