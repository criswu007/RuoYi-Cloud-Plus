package org.dromara.address.service.impl;

import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("dev")
class StandardAddressApprovalServiceDubboReferenceTest {

    @Test
    void shouldDeclareRemoteWorkflowServiceAsDubboReference() throws NoSuchFieldException {
        Field field = StandardAddressApprovalService.class.getDeclaredField("remoteWorkflowService");

        assertNotNull(
            field.getAnnotation(DubboReference.class),
            "microservice 模式下必须通过 @DubboReference 注入 RemoteWorkflowService"
        );
    }
}
