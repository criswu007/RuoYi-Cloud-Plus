package org.dromara.address.service;

import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.bo.StandardAddressBatchAddBo;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.AddrSetSegmMapper;
import org.dromara.address.service.impl.StandardAddressCommandService;
import org.dromara.address.service.impl.StandardAddressDictionaryService;
import org.dromara.address.service.impl.StandardAddressIdGenerator;
import org.dromara.address.service.impl.StandardAddressNameService;
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
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressCommandServiceTest {

    @Mock
    private AddrSegmMapper addrSegmMapper;

    @Mock
    private AddrSetSegmMapper addrSetSegmMapper;

    @Mock
    private StandardAddressDictionaryService dictionaryService;

    @Mock
    private StandardAddressNameService nameService;

    @Mock
    private StandardAddressIdGenerator idGenerator;

    @InjectMocks
    private StandardAddressCommandService commandService;

    @Test
    void shouldRejectWriteForLevel1AndLevel2() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setSegmType("180000");
        when(dictionaryService.resolveLevelId("180000")).thenReturn(1);

        assertThrows(ServiceException.class, () -> commandService.addStandardAddress(bo));
    }

    @Test
    void shouldRequireConfirmWhenInstallationAddressExists() {
        when(addrSegmMapper.countChildren(List.of("segm-1"))).thenReturn(0L);
        when(addrSetSegmMapper.countBySegmIds(List.of("segm-1"))).thenReturn(2L);
        AddrSegm current = new AddrSegm();
        current.setSegmId("segm-1");
        current.setSegmType("180007");
        when(addrSegmMapper.selectBatchIds(List.of("segm-1"))).thenReturn(List.of(current));
        when(dictionaryService.resolveLevelId("180007")).thenReturn(10);

        assertThrows(ServiceException.class, () -> commandService.deleteStandardAddresses(List.of("segm-1"), false));
    }

    @Test
    void shouldPreviewBatchChildrenByParentAndRange() {
        StandardAddressBatchAddBo bo = new StandardAddressBatchAddBo();
        bo.setParentSegmId("segm-parent");
        bo.setPrefix("A");
        bo.setStartNum(1);
        bo.setEndNum(3);

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("segm-parent");
        parent.setStandName("江苏省南京市");
        when(addrSegmMapper.selectById("segm-parent")).thenReturn(parent);
        when(nameService.buildSegmNo("A1")).thenReturn("A1");
        when(nameService.buildStandNo("江苏省南京市A1")).thenReturn("JSSNJSA1");

        List<StandardAddressAdminVo.BatchPreviewVo> preview = commandService.previewChildren(bo);

        assertEquals(3, preview.size());
        assertEquals("A1", preview.get(0).getSegmName());
        assertEquals("江苏省南京市A1", preview.get(0).getStandName());
    }
}
