package org.dromara.address.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.address.config.AddressSearchProperties;
import org.dromara.address.domain.AddrSetSegm;
import org.dromara.address.domain.bo.InstallationAddressBo;
import org.dromara.address.domain.vo.InstallationAddressVo;
import org.dromara.address.mapper.AddrSetSegmMapper;
import org.dromara.address.search.service.AddressSearchSyncService;
import org.dromara.address.search.service.InstallationAddressSearchGateway;
import org.dromara.address.search.support.CheckedBooleanSupplier;
import org.dromara.address.service.impl.InstallationAddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class InstallationAddressServiceImplTest {

    @Mock
    private AddrSetSegmMapper addrSetSegmMapper;

    @Mock
    private IStandardAddressService standardAddressService;

    @Mock
    private InstallationAddressSearchGateway installationAddressSearchGateway;

    @Mock
    private AddressSearchSyncService addressSearchSyncService;

    private AddressSearchProperties addressSearchProperties;

    private InstallationAddressServiceImpl installationAddressService;

    @BeforeEach
    void setUp() {
        addressSearchProperties = new AddressSearchProperties();
        addressSearchProperties.getInstallation().setReadEnabled(Boolean.FALSE);
        installationAddressService = new InstallationAddressServiceImpl(
            addrSetSegmMapper,
            standardAddressService,
            addressSearchProperties,
            installationAddressSearchGateway,
            addressSearchSyncService
        );
        lenient().when(addressSearchSyncService.syncInstallationCreate(any(), any()))
            .thenAnswer(invocation -> invocation.<CheckedBooleanSupplier>getArgument(1).getAsBoolean());
        lenient().when(addressSearchSyncService.syncInstallationUpdate(any(), any(), any()))
            .thenAnswer(invocation -> invocation.<CheckedBooleanSupplier>getArgument(2).getAsBoolean());
        lenient().when(addressSearchSyncService.syncInstallationDelete(anyList(), any()))
            .thenAnswer(invocation -> invocation.<CheckedBooleanSupplier>getArgument(1).getAsBoolean());
    }

    @Test
    void shouldBatchFillStandardAddressNamesInsteadOfPerRowLookup() {
        InstallationAddressVo first = new InstallationAddressVo();
        first.setSetAddrId("SET001");
        first.setSegmId("000000000000000000000101");

        InstallationAddressVo second = new InstallationAddressVo();
        second.setSetAddrId("SET002");
        second.setSegmId("000000000000000000000102");

        Page<InstallationAddressVo> page = new Page<>(1, 10, 2);
        page.setRecords(List.of(first, second));

        when(addrSetSegmMapper.selectInstallationPage(any(Page.class), any(InstallationAddressBo.class))).thenReturn(page);
        when(standardAddressService.listStandardAddressStandNameMapBySegmIds(
            List.of("000000000000000000000101", "000000000000000000000102")
        )).thenReturn(Map.of(
            "000000000000000000000101", "南京市鼓楼区中央路1号",
            "000000000000000000000102", "南京市鼓楼区中央路2号"
        ));

        TableDataInfo<InstallationAddressVo> result = installationAddressService.queryPageList(new InstallationAddressBo(), new PageQuery(10, 1));

        assertEquals("南京市鼓楼区中央路1号", result.getRows().get(0).getStandName());
        assertEquals("南京市鼓楼区中央路2号", result.getRows().get(1).getStandName());
        verify(standardAddressService).listStandardAddressStandNameMapBySegmIds(
            List.of("000000000000000000000101", "000000000000000000000102")
        );
        verify(standardAddressService, never()).getStandardAddressBySegmId(any());
    }

    @Test
    void shouldUseEsPageResultThenBatchFillStandardAddressNames() {
        addressSearchProperties.getInstallation().setReadEnabled(Boolean.TRUE);

        InstallationAddressVo first = new InstallationAddressVo();
        first.setSetAddrId("SET001");
        first.setSegmId("SEG001");
        InstallationAddressVo second = new InstallationAddressVo();
        second.setSetAddrId("SET002");
        second.setSegmId("SEG002");

        TableDataInfo<InstallationAddressVo> esPage = new TableDataInfo<>(List.of(first, second), 2);
        when(installationAddressSearchGateway.queryPage(any(), any())).thenReturn(esPage);
        when(standardAddressService.listStandardAddressStandNameMapBySegmIds(List.of("SEG001", "SEG002")))
            .thenReturn(Map.of("SEG001", "标准地址1", "SEG002", "标准地址2"));

        TableDataInfo<InstallationAddressVo> result = installationAddressService.queryPageList(new InstallationAddressBo(), new PageQuery(10, 1));

        assertEquals("标准地址1", result.getRows().get(0).getStandName());
        assertEquals("标准地址2", result.getRows().get(1).getStandName());
        verify(installationAddressSearchGateway).queryPage(any(), any());
        verify(addrSetSegmMapper, never()).selectInstallationPage(any(), any());
    }

    @Test
    void shouldSyncInstallationAddressCreateThroughAddressSearchSyncService() {
        InstallationAddressBo bo = new InstallationAddressBo();
        bo.setSetAddrName("机房1排1列");
        bo.setSegmId("SEG001");
        bo.setSegmType("180010");

        when(addrSetSegmMapper.insert(any(AddrSetSegm.class))).thenReturn(1);

        Boolean result = installationAddressService.insertByBo(bo);

        assertEquals(Boolean.TRUE, result);
        verify(addressSearchSyncService).syncInstallationCreate(any(), any());
        verify(addrSetSegmMapper).insert(any(AddrSetSegm.class));
    }

    @Test
    void shouldSyncInstallationAddressUpdateThroughAddressSearchSyncService() {
        InstallationAddressBo bo = new InstallationAddressBo();
        bo.setSetAddrId("SET001");
        bo.setSetAddrName("机房1排2列");
        bo.setSegmId("SEG001");
        bo.setSegmType("180010");

        AddrSetSegm existing = new AddrSetSegm();
        existing.setSetAddrId("SET001");
        existing.setSetAddrName("机房1排1列");
        existing.setSegmId("SEG001");
        existing.setDeleteState("0");

        when(addrSetSegmMapper.selectById("SET001")).thenReturn(existing);
        when(addrSetSegmMapper.updateById(any(AddrSetSegm.class))).thenReturn(1);

        Boolean result = installationAddressService.updateByBo(bo);

        assertEquals(Boolean.TRUE, result);
        verify(addressSearchSyncService).syncInstallationUpdate(any(), any(), any());
        verify(addrSetSegmMapper).updateById(any(AddrSetSegm.class));
    }

    @Test
    void shouldCaptureOldSnapshotBeforeInstallationAddressDelete() {
        AddrSetSegm existing = new AddrSetSegm();
        existing.setSetAddrId("SET001");
        existing.setSegmId("SEG001");
        existing.setDeleteState("0");

        when(addrSetSegmMapper.selectBatchIds(List.of("SET001"))).thenReturn(List.of(existing));
        doReturn(true).when(addressSearchSyncService).syncInstallationDelete(anyList(), any());

        Boolean result = installationAddressService.deleteWithValidByIds(List.of("SET001"), true);

        assertEquals(Boolean.TRUE, result);
        verify(addrSetSegmMapper).selectBatchIds(List.of("SET001"));
        verify(addressSearchSyncService).syncInstallationDelete(anyList(), any());
    }
}
