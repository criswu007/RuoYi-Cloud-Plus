package org.dromara.address.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class StandardAddressControllerMappingTest {

    @Test
    void shouldUsePostMappingsForCoreEndpoints() throws Exception {
        assertHasPostMapping("listStandardAddresses");
        assertHasPostMapping("listStandardAddressLevelOptions");
        assertHasPostMapping("listStandardAddressFormOptions");
        assertHasPostMapping("listStandardAddressStationOptions");
        assertHasPostMapping("getStandardAddressInfo");
        assertHasPostMapping("addStandardAddress");
        assertHasPostMapping("editStandardAddress");
        assertHasPostMapping("removeStandardAddresses");
        assertHasPostMapping("mergeStandardAddresses");
        assertHasPostMapping("splitStandardAddress");
        assertHasPostMapping("previewStandardAddressChildren");
        assertHasPostMapping("batchAddStandardAddressChildren");
        assertHasPostMapping("exportStandardAddresses");
        assertHasPostMapping("downloadStandardAddressImportTemplate");
        assertHasPostMapping("importStandardAddressData");
    }

    @Test
    void shouldImplementAdminApiContract() {
        assertTrue(StandardAddressAdminApi.class.isAssignableFrom(StandardAddressController.class));
    }

    private void assertHasPostMapping(String methodName) throws Exception {
        Method method = findMethod(methodName);
        assertNotNull(method.getAnnotation(PostMapping.class), () -> methodName + " 应改为 PostMapping");
    }

    private Method findMethod(String methodName) {
        for (Method method : StandardAddressController.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new IllegalArgumentException("未找到方法: " + methodName);
    }
}
