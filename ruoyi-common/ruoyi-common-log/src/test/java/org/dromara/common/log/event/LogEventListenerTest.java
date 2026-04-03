package org.dromara.common.log.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Tag("dev")
class LogEventListenerTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void saveLogShouldNotThrowWhenRemoteLogServiceUnavailable() {
        LogEventListener listener = new LogEventListener();
        OperLogEvent event = new OperLogEvent();
        event.setTitle("联调写操作");

        assertDoesNotThrow(() -> listener.saveLog(event));
    }

    @Test
    void saveLogininforShouldNotThrowWhenRemoteServicesUnavailable() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        LogEventListener listener = new LogEventListener();
        LogininforEvent event = new LogininforEvent();
        event.setUsername("standalone-dev");
        event.setStatus("0");
        event.setMessage("联调登录日志");
        event.setTenantId("000000");

        assertDoesNotThrow(() -> listener.saveLogininfor(event));
    }
}
