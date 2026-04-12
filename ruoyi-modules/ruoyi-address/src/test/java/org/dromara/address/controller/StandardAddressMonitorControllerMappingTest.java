package org.dromara.address.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("dev")
class StandardAddressMonitorControllerMappingTest {

    @Test
    void shouldUsePostMappingsForMonitorRuleEndpoints() throws Exception {
        assertHasPostMapping(StandardAddressMonitorRuleController.class, "listMonitorRules");
        assertHasPostMapping(StandardAddressMonitorRuleController.class, "getMonitorRuleInfo");
        assertHasPostMapping(StandardAddressMonitorRuleController.class, "addMonitorRule");
        assertHasPostMapping(StandardAddressMonitorRuleController.class, "editMonitorRule");
        assertHasPostMapping(StandardAddressMonitorRuleController.class, "removeMonitorRule");
        assertHasPostMapping(StandardAddressMonitorRuleController.class, "enableMonitorRule");
        assertHasPostMapping(StandardAddressMonitorRuleController.class, "disableMonitorRule");
    }

    @Test
    void shouldUsePostMappingsForMonitorTaskEndpoints() throws Exception {
        assertHasPostMapping(StandardAddressMonitorTaskController.class, "summaryMonitorTasks");
        assertHasPostMapping(StandardAddressMonitorTaskController.class, "executeMonitorTask");
        assertHasPostMapping(StandardAddressMonitorTaskController.class, "listMonitorTasks");
        assertHasPostMapping(StandardAddressMonitorTaskController.class, "getMonitorTaskInfo");
        assertHasPostMapping(StandardAddressMonitorTaskController.class, "listMonitorTaskRunLogs");
        assertHasPostMapping(StandardAddressMonitorTaskController.class, "addMonitorTask");
        assertHasPostMapping(StandardAddressMonitorTaskController.class, "editMonitorTask");
        assertHasPostMapping(StandardAddressMonitorTaskController.class, "rerunMonitorTask");
        assertHasPostMapping(StandardAddressMonitorTaskController.class, "pauseMonitorTask");
        assertHasPostMapping(StandardAddressMonitorTaskController.class, "terminateMonitorTask");
    }

    @Test
    void shouldUsePostMappingsForMonitorRecordEndpoints() throws Exception {
        assertHasPostMapping(StandardAddressMonitorRecordController.class, "listMonitorRecords");
        assertHasPostMapping(StandardAddressMonitorRecordController.class, "getMonitorRecordInfo");
        assertHasPostMapping(StandardAddressMonitorRecordController.class, "editMonitorRecord");
        assertHasPostMapping(StandardAddressMonitorRecordController.class, "removeMonitorRecord");
        assertHasPostMapping(StandardAddressMonitorRecordController.class, "ignoreMonitorRecord");
    }

    @Test
    void shouldUsePostMappingsForMonitorWorkOrderEndpoints() throws Exception {
        assertHasPostMapping(StandardAddressMonitorWorkOrderController.class, "listWorkOrders");
        assertHasPostMapping(StandardAddressMonitorWorkOrderController.class, "getWorkOrderInfo");
        assertHasPostMapping(StandardAddressMonitorWorkOrderController.class, "createWorkOrder");
        assertHasPostMapping(StandardAddressMonitorWorkOrderController.class, "correctWorkOrder");
        assertHasPostMapping(StandardAddressMonitorWorkOrderController.class, "rejectWorkOrder");
    }

    private void assertHasPostMapping(Class<?> controllerClass, String methodName) throws Exception {
        Method method = findMethod(controllerClass, methodName);
        assertNotNull(method.getAnnotation(PostMapping.class), () -> controllerClass.getSimpleName() + "#" + methodName + " 应改为 PostMapping");
    }

    private Method findMethod(Class<?> controllerClass, String methodName) {
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new IllegalArgumentException("未找到方法: " + controllerClass.getSimpleName() + "#" + methodName);
    }
}
