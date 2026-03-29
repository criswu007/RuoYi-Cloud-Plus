package org.dromara.address.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("dev")
class LegacyStandardAddressCleanupTest {

    @Test
    void shouldRemoveLegacyStandardAddressClassesFromClasspath() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.dromara.address.domain.StandardAddress"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.dromara.address.mapper.StandardAddressMapper"));
    }
}
