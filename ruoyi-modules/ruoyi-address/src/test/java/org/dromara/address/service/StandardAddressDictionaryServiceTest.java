package org.dromara.address.service;

import org.dromara.address.mapper.SegmAddrTypeMapper;
import org.dromara.address.service.impl.StandardAddressDictionaryService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressDictionaryServiceTest {

    @Mock
    private SegmAddrTypeMapper segmAddrTypeMapper;

    @InjectMocks
    private StandardAddressDictionaryService dictionaryService;

    @Test
    void shouldResolveLevelIdBySegmType() {
        when(segmAddrTypeMapper.selectLevelIdByAddrTypeId("180003")).thenReturn(3);

        assertEquals(3, dictionaryService.resolveLevelId("180003"));
    }
}
