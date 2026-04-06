package org.dromara.address.search;

import org.dromara.address.config.AddressSearchProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("dev")
public class AddressSearchPropertiesTest {

    @Test
    void shouldUseTaskOneDefaultSearchConfiguration() {
        AddressSearchProperties properties = new AddressSearchProperties();

        assertEquals("wait_for", properties.getRefreshPolicy());
        assertEquals("address_standard_search", properties.getStandard().getAlias());
        assertFalse(properties.getStandard().getReadEnabled());
        assertFalse(properties.getStandard().getWriteEnabled());
        assertEquals("address_installation_search", properties.getInstallation().getAlias());
        assertFalse(properties.getInstallation().getReadEnabled());
        assertFalse(properties.getInstallation().getWriteEnabled());
        assertEquals("http://127.0.0.1:5601", properties.getKibanaUrl());
        assertEquals("address:search:maintenance:running", properties.getMaintenanceLockKey());
    }
}
