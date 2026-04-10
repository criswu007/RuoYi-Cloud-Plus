package org.dromara.address.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("dev")
class StandardAddressImportBatchControllerMappingTest {

    @Test
    void shouldUseCamelCaseOrHierarchicalPathForImportBatchController() {
        RequestMapping requestMapping = StandardAddressImportBatchController.class.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping, "StandardAddressImportBatchController 应声明 RequestMapping");
        assertEquals("/address/import/batch", requestMapping.value()[0]);
    }

    @Test
    void shouldUsePostMappingsForImportBatchEndpoints() throws Exception {
        assertHasPostMapping("list");
        assertHasPostMapping("getBatchInfo");
        assertHasPostMapping("exportFailureDetails");
    }

    private void assertHasPostMapping(String methodName) throws Exception {
        Method method = findMethod(methodName);
        assertNotNull(method.getAnnotation(PostMapping.class), () -> methodName + " 应改为 PostMapping");
    }

    private Method findMethod(String methodName) {
        for (Method method : StandardAddressImportBatchController.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new IllegalArgumentException("未找到方法: " + methodName);
    }
}
