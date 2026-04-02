package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.bo.StandardAddressMergeBo;
import org.dromara.address.domain.bo.StandardAddressSplitBo;
import org.dromara.address.domain.bo.StandardAddressSplitItemBo;
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
        bo.setAddrLevel(3);
        bo.setLevelId(30);

        assertEquals("000102010000000011800001", bo.getSegmId());
        assertEquals("000102010000000011800000", bo.getParentSegmId());
        assertEquals("淮海路街道", bo.getSegmName());
        assertEquals(3, bo.getAddrLevel());
        assertEquals(3, bo.getLevel());
        assertEquals(30, bo.getLevelId());
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

    @Test
    void shouldUseStringSegmIdsForMergeContract() throws Exception {
        StandardAddressMergeBo bo = new StandardAddressMergeBo();
        bo.setSourceSegmIds(java.util.List.of("0001", "0002"));
        bo.setTargetSegmId("0003");

        assertEquals(java.util.List.of("0001", "0002"), bo.getSourceSegmIds());
        assertEquals("0003", bo.getTargetSegmId());

        Method method = IStandardAddressService.class.getMethod("mergeStandardAddresses", java.util.List.class, String.class);
        Type genericType = method.getGenericParameterTypes()[0];

        assertTrue(genericType instanceof ParameterizedType);
        assertEquals("java.util.List<java.lang.String>", genericType.getTypeName());
    }

    @Test
    void shouldUseMinimalSplitItemsForSplitContract() throws Exception {
        StandardAddressSplitBo bo = new StandardAddressSplitBo();
        bo.setSourceSegmId("0001");

        StandardAddressSplitItemBo splitItem = new StandardAddressSplitItemBo();
        splitItem.setSegmName("1单元");
        bo.setSplitItems(java.util.List.of(splitItem));

        assertEquals("0001", bo.getSourceSegmId());
        assertEquals("1单元", bo.getSplitItems().get(0).getSegmName());

        Method method = IStandardAddressService.class.getMethod("splitStandardAddress", String.class, java.util.List.class);
        Type genericType = method.getGenericParameterTypes()[1];

        assertTrue(genericType instanceof ParameterizedType);
        assertEquals("java.util.List<org.dromara.address.domain.bo.StandardAddressSplitItemBo>", genericType.getTypeName());
    }
}
