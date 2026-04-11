package org.dromara.address.workflow;

import cn.hutool.core.convert.Convert;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.api.domain.RemoteCompleteTask;
import org.dromara.workflow.api.domain.RemoteStartProcess;
import org.dromara.workflow.api.domain.RemoteStartProcessReturn;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 标准地址 workflow HTTP 客户端。
 * 目的：封装地址模块对 workflow HTTP 接口的调用，支撑 standalone 联调与审批动作落地。
 * 入参/出参：输入 workflow 启动、通过、驳回参数，输出流程实例主键或执行结果。
 * 关键约束：仅调用 workflow 对外控制器契约；HTTP 返回非成功码时统一转为业务异常。
 * 异常与副作用：会请求本地/远程 workflow 服务，失败时抛出业务异常，不直接写地址业务表。
 */
@Component
@RequiredArgsConstructor
public class AddressWorkflowHttpClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient.Builder restClientBuilder;

    @Value("${address.workflow.base-url:http://127.0.0.1:8080/workflow}")
    private String workflowBaseUrl;

    /**
     * 目的：启动流程并自动提交申请节点。
     * 入参：流程启动参数。
     * 出参：流程实例与申请节点任务信息。
     * 关键约束：会先调用启动接口，再补提申请节点，确保审批流进入审批员待办。
     * 异常与副作用：workflow 调用失败时抛出业务异常。
     */
    public RemoteStartProcessReturn startCompleteTask(RemoteStartProcess startProcess) {
        RemoteStartProcessReturn startResult = startWorkFlow(startProcess);
        if (startResult == null || startResult.getTaskId() == null) {
            throw new ServiceException("标准地址审批流程发起失败：未返回申请节点任务");
        }
        RemoteCompleteTask completeTask = new RemoteCompleteTask();
        completeTask.setTaskId(startResult.getTaskId());
        completeTask.setMessage("提交审批");
        Map<String, Object> variables = new HashMap<>(2);
        variables.put("ignore", true);
        completeTask.setVariables(variables);
        if (!completeTask(completeTask)) {
            throw new ServiceException("标准地址审批流程提交失败");
        }
        return startResult;
    }

    /**
     * 目的：调用 workflow 启动流程接口。
     * 入参：流程启动参数。
     * 出参：启动结果。
     * 关键约束：workflow 地址必须可达。
     * 异常与副作用：workflow 调用失败时抛出业务异常。
     */
    public RemoteStartProcessReturn startWorkFlow(RemoteStartProcess startProcess) {
        String responseBody = client().post()
            .uri("/task/startWorkFlow")
            .contentType(MediaType.APPLICATION_JSON)
            .body(startProcess)
            .retrieve()
            .body(String.class);
        return readWorkflowResponseData(responseBody, "标准地址审批流程发起失败", RemoteStartProcessReturn.class);
    }

    /**
     * 目的：办理 workflow 通过任务。
     * 入参：办理任务参数。
     * 出参：是否成功。
     * 关键约束：`taskId` 必须是当前待办任务。
     * 异常与副作用：workflow 调用失败时抛出业务异常。
     */
    public boolean completeTask(RemoteCompleteTask completeTask) {
        String responseBody = client().post()
            .uri("/task/completeTask")
            .contentType(MediaType.APPLICATION_JSON)
            .body(completeTask)
            .retrieve()
            .body(String.class);
        validateWorkflowActionResponse(responseBody, "标准地址审批通过失败", false);
        return true;
    }

    /**
     * 目的：驳回 workflow 任务。
     * 入参：任务 ID 与驳回意见。
     * 出参：是否成功。
     * 关键约束：驳回意见不能为空。
     * 异常与副作用：workflow 调用失败时抛出业务异常。
     */
    public boolean backProcess(Long taskId, String nodeCode, String message) {
        Map<String, Object> payload = new HashMap<>(4);
        payload.put("taskId", taskId);
        payload.put("nodeCode", nodeCode);
        payload.put("message", message);
        Map<String, Object> variables = new HashMap<>(2);
        variables.put("ignore", true);
        payload.put("variables", variables);
        String responseBody = client().post()
            .uri("/task/backProcess")
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .body(String.class);
        validateWorkflowActionResponse(responseBody, "标准地址审批驳回失败", false);
        return true;
    }

    /**
     * 目的：终止 workflow 任务。
     * 入参：任务 ID 与终止说明。
     * 出参：是否成功。
     * 关键约束：`taskId` 必须指向当前有效待办；终止后流程不再继续流转，申请人需重新发起。
     * 异常与副作用：workflow 调用失败时抛出业务异常。
     *
     * @param taskId workflow 任务 ID
     * @param comment 终止说明
     * @return 是否成功
     */
    public boolean terminationTask(Long taskId, String comment) {
        Map<String, Object> payload = new HashMap<>(2);
        payload.put("taskId", taskId);
        payload.put("comment", comment);
        String responseBody = client().post()
            .uri("/task/terminationTask")
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .body(String.class);
        return validateWorkflowActionResponse(responseBody, "标准地址审批驳回失败", true);
    }

    /**
     * 目的：按业务 ID 查询 workflow 实例主键。
     * 入参：业务 ID。
     * 出参：流程实例主键。
     * 关键约束：业务 ID 必须与审批申请单主键一致。
     * 异常与副作用：workflow 调用失败时抛出业务异常。
     */
    public Long getInstanceIdByBusinessId(String businessId) {
        String responseBody = client().get()
            .uri("/instance/getInfo/{businessId}", businessId)
            .retrieve()
            .body(String.class);
        Map<String, Object> data = readWorkflowResponseData(
            responseBody,
            "标准地址审批实例查询失败",
            new TypeReference<Map<String, Object>>() {
            }
        );
        return data == null ? null : Convert.toLong(data.get("id"));
    }

    /**
     * 目的：判断指定 workflow 任务是否已办理通过。
     * 入参：业务 ID 与任务 ID。
     * 出参：若 workflow 历史任务中存在该任务且状态为 `pass`，返回 true。
     * 关键约束：仅用于地址侧审批通过接口的幂等恢复，不替代正常办理流程。
     * 异常与副作用：会请求 workflow 历史任务接口；查询失败时抛出业务异常，不写业务数据。
     *
     * @param businessId workflow 业务 ID
     * @param taskId workflow 任务 ID
     * @return 是否已通过
     */
    public boolean isTaskPassed(String businessId, Long taskId) {
        if (businessId == null || taskId == null) {
            return false;
        }
        String responseBody = client().get()
            .uri("/instance/flowHisTaskList/{businessId}", businessId)
            .retrieve()
            .body(String.class);
        Map<String, Object> data = readWorkflowResponseData(
            responseBody,
            "标准地址审批历史查询失败",
            new TypeReference<Map<String, Object>>() {
            }
        );
        if (data == null || !(data.get("list") instanceof Collection<?> records)) {
            return false;
        }
        for (Object record : records) {
            if (record instanceof Map<?, ?> task
                && taskId.equals(Convert.toLong(task.get("taskId")))
                && "pass".equalsIgnoreCase(Convert.toStr(task.get("flowStatus")))) {
                return true;
            }
        }
        return false;
    }

    private RestClient client() {
        RestClient.Builder builder = restClientBuilder.baseUrl(workflowBaseUrl);
        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null) {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            String clientId = request.getHeader(LoginHelper.CLIENT_KEY);
            builder.defaultHeaders(headers -> {
                if (StringUtils.isNotBlank(authorization)) {
                    headers.set(HttpHeaders.AUTHORIZATION, authorization);
                }
                if (StringUtils.isNotBlank(clientId)) {
                    headers.set(LoginHelper.CLIENT_KEY, clientId);
                }
            });
        }
        return builder.build();
    }

    /**
     * 目的：把 workflow 的统一响应体解析为指定类型的数据对象，兼容 `text/plain` 返回头下的 JSON 文本。
     * 入参：workflow 原始响应体、失败提示语与目标数据类型。
     * 出参：解析后的目标数据对象；若响应 data 为空则返回 null。
     * 关键约束：仅接受符合统一 `R` 语义的 JSON 对象响应，类型转换失败视为响应协议不兼容。
     * 异常与副作用：响应非 JSON、返回码失败或类型转换失败时抛业务异常，无远程调用以外副作用。
     *
     * @param responseBody workflow 原始响应体
     * @param message 失败提示语
     * @param targetType 目标数据类型
     * @return 转换后的目标数据对象
     * @param <T> 目标数据泛型
     */
    private <T> T readWorkflowResponseData(String responseBody, String message, Class<T> targetType) {
        Object data = readWorkflowResponseData(responseBody, message);
        if (data == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(data, targetType);
        } catch (IllegalArgumentException ex) {
            throw new ServiceException(message);
        }
    }

    /**
     * 目的：把 workflow 的统一响应体解析为指定泛型数据对象，兼容 `text/plain` 返回头下的 JSON 文本。
     * 入参：workflow 原始响应体、失败提示语与目标泛型类型。
     * 出参：解析后的目标数据对象；若响应 data 为空则返回 null。
     * 关键约束：仅接受符合统一 `R` 语义的 JSON 对象响应，类型转换失败视为响应协议不兼容。
     * 异常与副作用：响应非 JSON、返回码失败或类型转换失败时抛业务异常，无远程调用以外副作用。
     *
     * @param responseBody workflow 原始响应体
     * @param message 失败提示语
     * @param targetType 目标泛型类型
     * @return 转换后的目标数据对象
     * @param <T> 目标数据泛型
     */
    private <T> T readWorkflowResponseData(String responseBody, String message, TypeReference<T> targetType) {
        Object data = readWorkflowResponseData(responseBody, message);
        if (data == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(data, targetType);
        } catch (IllegalArgumentException ex) {
            throw new ServiceException(message);
        }
    }

    /**
     * 目的：校验 workflow 响应是否满足统一 `R` 成功语义，并提取其中的 data 字段。
     * 入参：workflow 原始响应体与失败提示语。
     * 出参：统一响应体中的 data 对象；若 data 为空则返回 null。
     * 关键约束：这里只接受 JSON 对象格式，不对纯文本成功标记做兼容。
     * 异常与副作用：响应为空、非 JSON 或返回码失败时抛业务异常，无数据库写入副作用。
     *
     * @param responseBody workflow 原始响应体
     * @param message 失败提示语
     * @return 响应体中的 data 对象
     */
    private Object readWorkflowResponseData(String responseBody, String message) {
        if (StringUtils.isBlank(responseBody)) {
            return null;
        }
        String payload = responseBody.trim();
        if (!looksLikeJson(payload)) {
            throw new ServiceException(StringUtils.defaultIfBlank(payload, message));
        }
        R<?> response = parseWorkflowResponse(payload, message);
        return requireSuccess(response, message);
    }

    /**
     * 目的：兼容 workflow 动作接口返回 `application/json` 或 `text/plain` 的成功响应。
     * 入参：原始响应体、失败提示语与是否要求 data 显式为 true。
     * 出参：当 `requireTrueData=true` 时返回动作是否成功；否则仅在校验通过后返回 true。
     * 关键约束：纯文本仅接受 `true/ok/success/操作成功` 等成功标记；JSON 包装体必须满足统一 `R` 成功语义。
     * 异常与副作用：响应格式无法识别、返回码失败或布尔结果为 false 时抛业务异常，无数据库写入副作用。
     *
     * @param responseBody workflow 原始响应体
     * @param message 失败提示语
     * @param requireTrueData 是否要求响应 data 显式为 true
     * @return 校验通过后的成功结果
     */
    private boolean validateWorkflowActionResponse(String responseBody, String message, boolean requireTrueData) {
        if (StringUtils.isBlank(responseBody)) {
            return true;
        }
        String payload = responseBody.trim();
        if ("true".equalsIgnoreCase(payload)
            || "ok".equalsIgnoreCase(payload)
            || "success".equalsIgnoreCase(payload)
            || "操作成功".equals(payload)) {
            return true;
        }
        if ("false".equalsIgnoreCase(payload)) {
            throw new ServiceException(message);
        }
        if (!looksLikeJson(payload)) {
            throw new ServiceException(StringUtils.defaultIfBlank(payload, message));
        }
        R<?> response = parseWorkflowResponse(payload, message);
        Object data = requireSuccess(response, message);
        if (!requireTrueData) {
            return true;
        }
        Boolean result = Convert.toBool(data);
        if (Boolean.TRUE.equals(result)) {
            return true;
        }
        throw new ServiceException(StringUtils.defaultIfBlank(response.getMsg(), message));
    }

    /**
     * 目的：把 workflow 文本响应解析为统一 `R` 包装对象。
     * 入参：workflow 原始 JSON 文本与失败提示语。
     * 出参：解析后的统一响应对象。
     * 关键约束：仅接受 JSON 对象格式，不对数组或其他文本做隐式容错。
     * 异常与副作用：JSON 解析失败时抛业务异常，无远程调用或数据库副作用。
     *
     * @param payload workflow 原始 JSON 文本
     * @param message 失败提示语
     * @return 解析后的统一响应对象
     */
    private R<?> parseWorkflowResponse(String payload, String message) {
        try {
            return OBJECT_MAPPER.readValue(payload, new TypeReference<R<Object>>() {
            });
        } catch (Exception ex) {
            throw new ServiceException(message);
        }
    }

    /**
     * 目的：快速判断 workflow 文本响应是否为 JSON 对象。
     * 入参：原始响应体文本。
     * 出参：若文本以 `{` 开头且以 `}` 结尾则返回 true，否则返回 false。
     * 关键约束：这里只做轻量级格式预判，真正结构校验由 JSON 解析承担。
     * 异常与副作用：无异常，无副作用。
     *
     * @param payload 原始响应体文本
     * @return 是否看起来像 JSON 对象
     */
    private boolean looksLikeJson(String payload) {
        return StringUtils.isNotBlank(payload) && payload.startsWith("{") && payload.endsWith("}");
    }

    private <T> T requireSuccess(R<T> response, String message) {
        if (response == null || !R.isSuccess(response)) {
            throw new ServiceException(response == null ? message : Convert.toStr(response.getMsg(), message));
        }
        return response.getData();
    }
}
