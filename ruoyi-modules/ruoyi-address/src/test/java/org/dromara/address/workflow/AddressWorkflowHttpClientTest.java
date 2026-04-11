package org.dromara.address.workflow;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.api.domain.RemoteCompleteTask;
import org.dromara.workflow.api.domain.RemoteStartProcess;
import org.dromara.workflow.api.domain.RemoteStartProcessReturn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * workflow HTTP 客户端单元测试。
 * 目的：验证地址模块在 workflow 接口错误返回 `text/plain` 响应头时，仍能正确识别成功 JSON 包装体。
 * 入参/出参：构造本地 HTTP Server 模拟 workflow 返回，调用客户端的审批通过/终止方法并断言结果。
 * 关键约束：测试只覆盖客户端解析与错误兼容逻辑，不触发真实 workflow 服务。
 * 异常与副作用：会在本地随机端口启动临时 HTTP Server，测试结束后立即释放。
 */
@Tag("dev")
class AddressWorkflowHttpClientTest {

    private static final String TEST_AUTHORIZATION = "Bearer workflow-token";
    private static final String TEST_CLIENT_ID = "workflow-client-id";

    private HttpServer server;

    private AddressWorkflowHttpClient workflowHttpClient;

    private String capturedAuthorization;

    private String capturedClientId;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        workflowHttpClient = new AddressWorkflowHttpClient(RestClient.builder());
        ReflectionTestUtils.setField(
            workflowHttpClient,
            "workflowBaseUrl",
            "http://127.0.0.1:" + server.getAddress().getPort()
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldCompleteTaskWhenWorkflowReturnsTextPlainJson() {
        server.createContext("/task/completeTask", exchange ->
            respond(exchange, "{\"code\":200,\"msg\":\"操作成功\",\"data\":null}"));

        RemoteCompleteTask completeTask = new RemoteCompleteTask();
        completeTask.setTaskId(9001L);
        completeTask.setMessage("审批通过");
        completeTask.setVariables(new HashMap<>(1));

        assertTrue(workflowHttpClient.completeTask(completeTask));
    }

    @Test
    void shouldForwardAuthorizationHeadersWhenCallingWorkflowThroughGateway() {
        server.createContext("/task/completeTask", exchange -> {
            capturedAuthorization = exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            capturedClientId = exchange.getRequestHeaders().getFirst(LoginHelper.CLIENT_KEY);
            respond(exchange, "{\"code\":200,\"msg\":\"操作成功\",\"data\":null}");
        });
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, TEST_AUTHORIZATION);
        request.addHeader(LoginHelper.CLIENT_KEY, TEST_CLIENT_ID);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RemoteCompleteTask completeTask = new RemoteCompleteTask();
        completeTask.setTaskId(9005L);
        completeTask.setMessage("审批通过");
        completeTask.setVariables(new HashMap<>(1));

        assertTrue(workflowHttpClient.completeTask(completeTask));
        assertEquals(TEST_AUTHORIZATION, capturedAuthorization);
        assertEquals(TEST_CLIENT_ID, capturedClientId);
    }

    @Test
    void shouldTerminateTaskWhenWorkflowReturnsTextPlainJson() {
        server.createContext("/task/terminationTask", exchange ->
            respond(exchange, "{\"code\":200,\"msg\":\"操作成功\",\"data\":true}"));

        assertTrue(workflowHttpClient.terminationTask(9002L, "驳回原因"));
    }

    @Test
    void shouldReadInstanceIdWhenWorkflowReturnsTextPlainJson() {
        server.createContext("/instance/getInfo/1001", exchange ->
            respond(exchange, "{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"id\":9003}}"));

        assertEquals(9003L, workflowHttpClient.getInstanceIdByBusinessId("1001"));
    }

    @Test
    void shouldDetectPassedTaskWhenWorkflowHistoryReturnsTextPlainJson() {
        server.createContext("/instance/flowHisTaskList/1001", exchange ->
            respond(exchange, "{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"list\":[{\"taskId\":9001,\"flowStatus\":\"pass\"}]}}"));

        assertTrue(workflowHttpClient.isTaskPassed("1001", 9001L));
    }

    @Test
    void shouldStartWorkflowWhenWorkflowReturnsTextPlainJson() {
        server.createContext("/task/startWorkFlow", exchange ->
            respond(exchange, "{\"code\":200,\"msg\":\"操作成功\",\"data\":{\"taskId\":9004,\"processInstanceId\":8001}}"));

        RemoteStartProcess startProcess = new RemoteStartProcess();
        RemoteStartProcessReturn result = workflowHttpClient.startWorkFlow(startProcess);

        assertNotNull(result);
        assertEquals(9004L, result.getTaskId());
        assertEquals(8001L, result.getProcessInstanceId());
    }

    @Test
    void shouldUseGatewayWorkflowBaseUrlByDefault() throws NoSuchFieldException {
        Field field = AddressWorkflowHttpClient.class.getDeclaredField("workflowBaseUrl");
        Value value = field.getAnnotation(Value.class);

        assertNotNull(value);
        assertEquals("${address.workflow.base-url:http://127.0.0.1:8080/workflow}", value.value());
    }

    /**
     * 目的：以 `text/plain` 形式返回 workflow 模拟响应，复现线上审批接口当前的响应头问题。
     * 入参：HTTP 交换对象与响应体文本。
     * 出参：无。
     * 关键约束：统一返回 200，且显式设置 `text/plain;charset=utf-8`。
     * 异常与副作用：会向测试 HTTP 连接写出响应内容并关闭输出流。
     *
     * @param exchange HTTP 交换对象
     * @param body 响应体文本
     * @throws IOException 写响应失败时抛出
     */
    private void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain;charset=utf-8");
        exchange.sendResponseHeaders(200, bodyBytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bodyBytes);
        } finally {
            exchange.close();
        }
    }
}
