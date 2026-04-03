package org.dromara.address.dubbo;

import org.dromara.address.api.domain.RemoteStandardAddressVo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.service.impl.StandardAddressQueryService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class RemoteStandardAddressServiceImplTest {

    @Mock
    private StandardAddressQueryService queryService;

    @InjectMocks
    private RemoteStandardAddressServiceImpl remoteService;

    @Test
    void shouldSearchWithRemainingLimitInsteadOfFullList() {
        when(queryService.searchRegionCandidates("鼓楼", 1, 3)).thenReturn(List.of(buildVo("320000", "江苏省鼓楼")));
        when(queryService.searchRegionCandidates("鼓楼", 2, 2)).thenReturn(List.of(buildVo("320100", "南京市鼓楼")));
        when(queryService.searchAddrSegmCandidates("鼓楼", null, null, 1)).thenReturn(List.of(buildVo("0001", "鼓楼区中央门")));

        List<RemoteStandardAddressVo> result = remoteService.searchStandardAddresses("鼓楼", 3);

        assertEquals(List.of(320000L, 320100L, 1L),
            result.stream().map(RemoteStandardAddressVo::getId).toList());
        verify(queryService).searchRegionCandidates("鼓楼", 1, 3);
        verify(queryService).searchRegionCandidates("鼓楼", 2, 2);
        verify(queryService).searchAddrSegmCandidates("鼓楼", null, null, 1);
    }

    private StandardAddressVo buildVo(String segmId, String name) {
        StandardAddressVo vo = new StandardAddressVo();
        vo.setSegmId(segmId);
        vo.setSegmName(name);
        vo.setStandName(name);
        vo.setAddrLevel(3);
        vo.setLevelId(30);
        return vo;
    }
}
