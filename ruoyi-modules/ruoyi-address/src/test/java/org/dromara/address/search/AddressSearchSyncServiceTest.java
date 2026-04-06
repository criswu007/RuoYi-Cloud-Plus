package org.dromara.address.search;

import org.dromara.address.config.AddressSearchProperties;
import org.dromara.address.domain.AddressSearchRepairTask;
import org.dromara.address.domain.AddressSearchSyncLog;
import org.dromara.address.mapper.AddressSearchRepairTaskMapper;
import org.dromara.address.mapper.AddressSearchSyncLogMapper;
import org.dromara.address.search.document.InstallationAddressSearchDocument;
import org.dromara.address.search.document.StandardAddressSearchDocument;
import org.dromara.address.search.service.AddressSearchSyncService;
import org.dromara.address.search.service.InstallationAddressSearchGateway;
import org.dromara.address.search.service.StandardAddressSearchGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class AddressSearchSyncServiceTest {

    @Mock
    private StandardAddressSearchGateway standardAddressSearchGateway;

    @Mock
    private InstallationAddressSearchGateway installationAddressSearchGateway;

    @Mock
    private AddressSearchSyncLogMapper addressSearchSyncLogMapper;

    @Mock
    private AddressSearchRepairTaskMapper addressSearchRepairTaskMapper;

    private AddressSearchProperties addressSearchProperties;

    private AddressSearchSyncService addressSearchSyncService;

    @BeforeEach
    void setUp() {
        addressSearchProperties = new AddressSearchProperties();
        addressSearchProperties.getStandard().setWriteEnabled(Boolean.TRUE);
        addressSearchProperties.getInstallation().setWriteEnabled(Boolean.TRUE);
        addressSearchSyncService = new AddressSearchSyncService(
            standardAddressSearchGateway,
            installationAddressSearchGateway,
            addressSearchSyncLogMapper,
            addressSearchRepairTaskMapper,
            addressSearchProperties
        );
    }

    @Test
    void shouldWriteStandardDocumentToEsBeforeDatabaseAction() throws Exception {
        StandardAddressSearchDocument document = new StandardAddressSearchDocument();
        document.setSegmId("SEG001");
        AtomicBoolean dbExecuted = new AtomicBoolean(false);
        when(standardAddressSearchGateway.upsert(document)).thenReturn(true);

        boolean result = addressSearchSyncService.syncStandardCreate(document, () -> {
            dbExecuted.set(true);
            return true;
        });

        assertTrue(result);
        assertTrue(dbExecuted.get());
        verify(standardAddressSearchGateway).upsert(document);
        verify(addressSearchSyncLogMapper, times(2)).insert(any(AddressSearchSyncLog.class));
        verify(addressSearchRepairTaskMapper, never()).insert(any(AddressSearchRepairTask.class));
    }

    @Test
    void shouldDeleteEsDocumentWhenDatabaseInsertFailsAfterStandardCreate() {
        StandardAddressSearchDocument document = new StandardAddressSearchDocument();
        document.setSegmId("SEG001");
        when(standardAddressSearchGateway.upsert(document)).thenReturn(true);
        when(standardAddressSearchGateway.deleteByIds(List.of("SEG001"))).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> addressSearchSyncService.syncStandardCreate(document, () -> {
                throw new RuntimeException("db failed");
            }));

        assertEquals("db failed", ex.getMessage());
        verify(standardAddressSearchGateway).upsert(document);
        verify(standardAddressSearchGateway).deleteByIds(List.of("SEG001"));
        verify(addressSearchSyncLogMapper, times(3)).insert(any(AddressSearchSyncLog.class));
        verify(addressSearchRepairTaskMapper, never()).insert(any(AddressSearchRepairTask.class));
    }

    @Test
    void shouldCreateRepairTaskWhenStandardUpdateCompensationFails() {
        StandardAddressSearchDocument beforeDocument = new StandardAddressSearchDocument();
        beforeDocument.setSegmId("SEG001");
        beforeDocument.setStandName("旧标准地址");
        StandardAddressSearchDocument afterDocument = new StandardAddressSearchDocument();
        afterDocument.setSegmId("SEG001");
        afterDocument.setStandName("新标准地址");
        when(standardAddressSearchGateway.upsert(afterDocument)).thenReturn(true);
        when(standardAddressSearchGateway.restore(beforeDocument)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> addressSearchSyncService.syncStandardUpdate(beforeDocument, afterDocument, () -> {
                throw new RuntimeException("db failed");
            }));

        assertEquals("db failed", ex.getMessage());
        verify(standardAddressSearchGateway).upsert(afterDocument);
        verify(standardAddressSearchGateway).restore(beforeDocument);
        verify(addressSearchSyncLogMapper, times(3)).insert(any(AddressSearchSyncLog.class));
        ArgumentCaptor<AddressSearchRepairTask> repairTaskCaptor = ArgumentCaptor.forClass(AddressSearchRepairTask.class);
        verify(addressSearchRepairTaskMapper).insert(repairTaskCaptor.capture());
        assertEquals("STANDARD", repairTaskCaptor.getValue().getEntityType());
        assertEquals("SEG001", repairTaskCaptor.getValue().getEntityId());
        assertEquals("UPSERT_DOC", repairTaskCaptor.getValue().getRepairAction());
        assertEquals("PENDING", repairTaskCaptor.getValue().getStatus());
    }

    @Test
    void shouldRestoreInstallationDocumentsWhenDatabaseDeleteFails() {
        InstallationAddressSearchDocument first = new InstallationAddressSearchDocument();
        first.setSetAddrId("SET001");
        InstallationAddressSearchDocument second = new InstallationAddressSearchDocument();
        second.setSetAddrId("SET002");
        when(installationAddressSearchGateway.deleteByIds(List.of("SET001", "SET002"))).thenReturn(true);
        when(installationAddressSearchGateway.restore(first)).thenReturn(true);
        when(installationAddressSearchGateway.restore(second)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> addressSearchSyncService.syncInstallationDelete(List.of(first, second), () -> {
                throw new RuntimeException("db failed");
            }));

        assertEquals("db failed", ex.getMessage());
        verify(installationAddressSearchGateway).deleteByIds(List.of("SET001", "SET002"));
        verify(installationAddressSearchGateway).restore(first);
        verify(installationAddressSearchGateway).restore(second);
        verify(addressSearchRepairTaskMapper, never()).insert(any(AddressSearchRepairTask.class));
    }

    @Test
    void shouldSkipEsWriteWhenInstallationWriteSwitchDisabled() throws Exception {
        addressSearchProperties.getInstallation().setWriteEnabled(Boolean.FALSE);
        InstallationAddressSearchDocument document = new InstallationAddressSearchDocument();
        document.setSetAddrId("SET001");
        AtomicBoolean dbExecuted = new AtomicBoolean(false);

        boolean result = addressSearchSyncService.syncInstallationCreate(document, () -> {
            dbExecuted.set(true);
            return true;
        });

        assertTrue(result);
        assertTrue(dbExecuted.get());
        verify(installationAddressSearchGateway, never()).upsert(any());
        verify(addressSearchSyncLogMapper, never()).insert(any(AddressSearchSyncLog.class));
        verify(addressSearchRepairTaskMapper, never()).insert(any(AddressSearchRepairTask.class));
    }
}
