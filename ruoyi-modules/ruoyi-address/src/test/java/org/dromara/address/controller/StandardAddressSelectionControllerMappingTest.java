package org.dromara.address.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("dev")
class StandardAddressSelectionControllerMappingTest {

    @Test
    void shouldUsePostMappingsForSelectionEndpoints() throws Exception {
        assertHasPostMapping("searchStandardAddresses");
        assertHasPostMapping("createRoomStandardAddress");
    }

    private void assertHasPostMapping(String methodName) throws Exception {
        Method method = findMethod(methodName);
        assertNotNull(method.getAnnotation(PostMapping.class), () -> methodName + " 应改为 PostMapping");
    }

    private Method findMethod(String methodName) {
        for (Method method : StandardAddressSelectionController.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new IllegalArgumentException("未找到方法: " + methodName);
    }
}
