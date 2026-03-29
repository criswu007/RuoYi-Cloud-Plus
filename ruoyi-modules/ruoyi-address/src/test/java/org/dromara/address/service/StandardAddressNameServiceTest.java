package org.dromara.address.service;

import org.dromara.address.service.impl.StandardAddressIdGenerator;
import org.dromara.address.service.impl.StandardAddressNameService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class StandardAddressNameServiceTest {

    private final StandardAddressNameService nameService = new StandardAddressNameService();
    private final StandardAddressIdGenerator idGenerator = new StandardAddressIdGenerator();

    @Test
    void shouldGenerateStandNoWithUpperInitials() {
        assertEquals("JSSNJSZCQBXQHHLJD", nameService.buildStandNo("江苏省南京市主城区白下区淮海路街道"));
    }

    @Test
    void shouldGenerate24CharSegmId() {
        String segmId = idGenerator.nextSegmId();

        assertEquals(24, segmId.length());
    }
}
