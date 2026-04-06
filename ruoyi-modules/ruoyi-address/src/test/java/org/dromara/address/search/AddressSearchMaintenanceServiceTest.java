package org.dromara.address.search;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.address.config.AddressSearchProperties;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.AddrSetSegm;
import org.dromara.address.domain.AddressSearchRepairTask;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.AddrSetSegmMapper;
import org.dromara.address.mapper.AddressSearchRepairTaskMapper;
import org.dromara.address.search.support.AddressSearchMaintenanceProgressListener;
import org.dromara.address.search.service.AddressSearchMaintenanceService;
import org.dromara.address.search.service.InstallationAddressSearchGateway;
import org.dromara.address.search.service.StandardAddressSearchGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class AddressSearchMaintenanceServiceTest {

    @Mock
    private StandardAddressSearchGateway standardAddressSearchGateway;

    @Mock
    private InstallationAddressSearchGateway installationAddressSearchGateway;

    @Mock
    private AddressSearchRepairTaskMapper addressSearchRepairTaskMapper;

    @Mock
    private AddrSegmMapper addrSegmMapper;

    @Mock
    private AddrSetSegmMapper addrSetSegmMapper;

    private AddressSearchMaintenanceService maintenanceService;

    @BeforeEach
    void setUp() {
        AddressSearchProperties addressSearchProperties = new AddressSearchProperties();
        addressSearchProperties.setRebuildBatchSize(500);
        maintenanceService = new AddressSearchMaintenanceService(
            standardAddressSearchGateway,
            installationAddressSearchGateway,
            addressSearchRepairTaskMapper,
            addrSegmMapper,
            addrSetSegmMapper,
            addressSearchProperties
        );
    }

    @Test
    void shouldRebuildStandardIndexByBatchThenSwitchAlias() {
        when(standardAddressSearchGateway.prepareRebuildIndex()).thenReturn("address_standard_search_v2");
        Page<AddrSegm> page1 = new Page<>(1, 500, 1);
        page1.setRecords(List.of(buildAddrSegm("SEG001")));
        Page<AddrSegm> page2 = new Page<>(1, 500, 0);
        page2.setRecords(List.of());
        when(addrSegmMapper.selectPage(any(Page.class), any())).thenReturn(page1, page2);
        when(standardAddressSearchGateway.bulkIndex("address_standard_search_v2", page1.getRecords())).thenReturn(true);

        maintenanceService.rebuildStandardIndex();

        verify(standardAddressSearchGateway).bulkIndex(eq("address_standard_search_v2"), eq(page1.getRecords()));
        verify(standardAddressSearchGateway).switchAlias("address_standard_search_v2");
    }

    @Test
    void shouldRebuildInstallationIndexByBatchThenSwitchAlias() {
        when(installationAddressSearchGateway.prepareRebuildIndex()).thenReturn("address_installation_search_v2");
        Page<AddrSetSegm> page1 = new Page<>(1, 500, 1);
        page1.setRecords(List.of(buildAddrSetSegm("SET001")));
        Page<AddrSetSegm> page2 = new Page<>(1, 500, 0);
        page2.setRecords(List.of());
        when(addrSetSegmMapper.selectPage(any(Page.class), any())).thenReturn(page1, page2);
        when(installationAddressSearchGateway.bulkIndex("address_installation_search_v2", page1.getRecords())).thenReturn(true);

        maintenanceService.rebuildInstallationIndex();

        verify(installationAddressSearchGateway).bulkIndex(eq("address_installation_search_v2"), eq(page1.getRecords()));
        verify(installationAddressSearchGateway).switchAlias("address_installation_search_v2");
    }

    @Test
    void shouldReplayPendingRepairTask() {
        AddressSearchRepairTask task = buildPendingTask(1L, "STANDARD", "SEG001", "UPSERT_DOC");
        when(addressSearchRepairTaskMapper.selectById(1L)).thenReturn(task);
        when(standardAddressSearchGateway.repair(task)).thenReturn(true);

        maintenanceService.executeRepairTask(1L);

        verify(standardAddressSearchGateway).repair(task);
        ArgumentCaptor<AddressSearchRepairTask> taskCaptor = ArgumentCaptor.forClass(AddressSearchRepairTask.class);
        verify(addressSearchRepairTaskMapper).updateById(taskCaptor.capture());
        assertEquals("SUCCESS", taskCaptor.getValue().getStatus());
        assertEquals(1L, taskCaptor.getValue().getId());
    }

    @Test
    void shouldPublishProgressDuringStandardRebuild() {
        AddressSearchMaintenanceProgressListener listener = org.mockito.Mockito.mock(AddressSearchMaintenanceProgressListener.class);
        when(standardAddressSearchGateway.prepareRebuildIndex()).thenReturn("address_standard_search_v2");
        Page<AddrSegm> page1 = new Page<>(1, 500, 1);
        page1.setRecords(List.of(buildAddrSegm("SEG001")));
        Page<AddrSegm> page2 = new Page<>(1, 500, 0);
        page2.setRecords(List.of());
        when(addrSegmMapper.selectCount(any())).thenReturn(1L);
        when(addrSegmMapper.selectPage(any(Page.class), any())).thenReturn(page1, page2);
        when(standardAddressSearchGateway.bulkIndex("address_standard_search_v2", page1.getRecords())).thenReturn(true);

        maintenanceService.rebuildStandardIndex(listener);

        verify(listener).onTotalResolved(1L);
        verify(listener).onPhysicalIndexPrepared("address_standard_search_v2");
        verify(listener).onBatchCompleted(1L);
        verify(listener).onAliasSwitched();
    }

    private AddrSegm buildAddrSegm(String segmId) {
        AddrSegm entity = new AddrSegm();
        entity.setSegmId(segmId);
        entity.setSegmType("180010");
        entity.setDeleteState("0");
        entity.setCreateDate(new Date());
        return entity;
    }

    private AddrSetSegm buildAddrSetSegm(String setAddrId) {
        AddrSetSegm entity = new AddrSetSegm();
        entity.setSetAddrId(setAddrId);
        entity.setDeleteState("0");
        entity.setCreateDate(new Date());
        return entity;
    }

    private AddressSearchRepairTask buildPendingTask(Long taskId, String entityType, String entityId, String repairAction) {
        AddressSearchRepairTask task = new AddressSearchRepairTask();
        task.setId(taskId);
        task.setEntityType(entityType);
        task.setEntityId(entityId);
        task.setRepairAction(repairAction);
        task.setPayloadJson("{\"segmId\":\"SEG001\"}");
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setCreatedTime(new Date());
        task.setUpdatedTime(new Date());
        return task;
    }
}
