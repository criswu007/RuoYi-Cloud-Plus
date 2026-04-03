package org.dromara.address.service;

import org.dromara.address.domain.StandardAddressTagRel;
import org.dromara.address.mapper.StandardAddressTagMapper;
import org.dromara.address.mapper.StandardAddressTagRelMapper;
import org.dromara.address.service.impl.StandardAddressTagServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressTagServiceImplTest {

    @Mock
    private StandardAddressTagMapper baseMapper;

    @Mock
    private StandardAddressTagRelMapper addressTagRelMapper;

    @Mock
    private IStandardAddressService standardAddressService;

    @InjectMocks
    private StandardAddressTagServiceImpl service;

    @Test
    void shouldBatchValidateStandardAddressIdsInsteadOfPerRowDetailLookup() {
        when(standardAddressService.listStandardAddressStandNameMapBySegmIds(List.of("101", "102")))
            .thenReturn(Map.of("101", "南京市鼓楼区中央路1号", "102", "南京市鼓楼区中央路2号"));
        when(baseMapper.selectCount(any())).thenReturn(2L);
        when(addressTagRelMapper.selectList(any())).thenReturn(List.of());
        when(addressTagRelMapper.insert(any(StandardAddressTagRel.class))).thenReturn(1);

        boolean result = service.bindTagsToStandardAddresses(List.of("101", "102"), List.of(1L, 2L));

        assertTrue(result);
        verify(standardAddressService).listStandardAddressStandNameMapBySegmIds(List.of("101", "102"));
        verify(standardAddressService, never()).getStandardAddressBySegmId(any());
    }
}
