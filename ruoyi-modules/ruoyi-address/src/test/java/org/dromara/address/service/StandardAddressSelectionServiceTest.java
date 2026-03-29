package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressSelectionRoomBo;
import org.dromara.address.service.impl.StandardAddressSelectionServiceImpl;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressSelectionServiceTest {

    @Mock
    private IStandardAddressService standardAddressService;

    @InjectMocks
    private StandardAddressSelectionServiceImpl selectionService;

    @Test
    void shouldRejectLegacyLongParentRoomCreationBeforeSelectionModuleMigrates() {
        StandardAddressSelectionRoomBo bo = new StandardAddressSelectionRoomBo();
        bo.setParentId(1L);
        bo.setRoomName("101");

        assertThrows(ServiceException.class, () -> selectionService.createRoomStandardAddress(bo));
    }
}
