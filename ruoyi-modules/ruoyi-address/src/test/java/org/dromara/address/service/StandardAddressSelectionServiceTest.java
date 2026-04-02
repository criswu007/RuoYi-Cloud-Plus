package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressSelectionRoomBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.service.impl.StandardAddressQueryService;
import org.dromara.address.service.impl.StandardAddressSelectionServiceImpl;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressSelectionServiceTest {

    @Mock
    private StandardAddressQueryService queryService;

    @InjectMocks
    private StandardAddressSelectionServiceImpl selectionService;

    @Test
    void shouldSearchWithDatabaseLimitedCandidatesInsteadOfFullList() {
        when(queryService.searchRegionCandidates("鼓楼", 1, 5)).thenReturn(List.of(buildVo("320000", 1)));
        when(queryService.searchRegionCandidates("鼓楼", 2, 4)).thenReturn(List.of(buildVo("320100", 2)));
        when(queryService.searchAddrSegmCandidates("鼓楼", 10, "2140900", 3))
            .thenReturn(List.of(buildVo("0001", 3), buildVo("320100", 2)));

        List<StandardAddressVo> result = selectionService.searchStandardAddresses("鼓楼", 10, 5);

        assertEquals(List.of("320000", "320100", "0001"),
            result.stream().map(StandardAddressVo::getSegmId).toList());
        verify(queryService).searchRegionCandidates("鼓楼", 1, 5);
        verify(queryService).searchRegionCandidates("鼓楼", 2, 4);
        verify(queryService).searchAddrSegmCandidates("鼓楼", 10, "2140900", 3);
    }

    @Test
    void shouldRejectLegacyLongParentRoomCreationBeforeSelectionModuleMigrates() {
        StandardAddressSelectionRoomBo bo = new StandardAddressSelectionRoomBo();
        bo.setParentId(1L);
        bo.setRoomName("101");

        assertThrows(ServiceException.class, () -> selectionService.createRoomStandardAddress(bo));
    }

    private StandardAddressVo buildVo(String segmId, Integer addrLevel) {
        StandardAddressVo vo = new StandardAddressVo();
        vo.setSegmId(segmId);
        vo.setAddrLevel(addrLevel);
        vo.setStandName(segmId);
        return vo;
    }
}
