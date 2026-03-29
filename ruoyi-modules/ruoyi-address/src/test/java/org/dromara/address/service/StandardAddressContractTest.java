package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class StandardAddressContractTest {

    @Test
    void shouldUseCanonicalStandardAddressFields() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setSegmId("000102010000000011800001");
        bo.setParentSegmId("000102010000000011800000");
        bo.setSegmName("淮海路街道");
        bo.setLevelId(3);

        assertEquals("000102010000000011800001", bo.getSegmId());
        assertEquals("000102010000000011800000", bo.getParentSegmId());
        assertEquals("淮海路街道", bo.getSegmName());
        assertEquals(3, bo.getLevelId());
    }

    @Test
    void shouldKeepDeprecatedAliasForNonCoreCallers() {
        StandardAddressVo vo = new StandardAddressVo();
        vo.setSegmId("000102010000000011800001");
        vo.setStandName("江苏省南京市主城区白下区淮海路街道");

        assertEquals("000102010000000011800001", vo.getId());
        assertEquals("江苏省南京市主城区白下区淮海路街道", vo.getFullName());
    }

    @Test
    void shouldExposeStringSegmIdServiceContract() throws Exception {
        Method method = IStandardAddressService.class.getMethod("getStandardAddressBySegmId", String.class);
        assertNotNull(method);
        assertEquals(StandardAddressVo.class, method.getReturnType());
    }

    @Test
    void shouldUseStringSegmIdsForDeleteContract() throws Exception {
        Method method = IStandardAddressService.class.getMethod("deleteStandardAddresses", java.util.Collection.class, boolean.class);
        Type genericType = method.getGenericParameterTypes()[0];

        assertTrue(genericType instanceof ParameterizedType);
        assertEquals("java.util.Collection<java.lang.String>", genericType.getTypeName());
    }
}
