package org.dromara.address.workflow;

import cn.hutool.core.convert.Convert;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.api.domain.RemoteCompleteTask;
import org.dromara.workflow.api.domain.RemoteStartProcess;
import org.dromara.workflow.api.domain.RemoteStartProcessReturn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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

    private final RestClient.Builder restClientBuilder;

    @Value("${address.workflow.base-url:http://127.0.0.1:9205}")
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
        R<RemoteStartProcessReturn> response = client().post()
            .uri("/task/startWorkFlow")
            .contentType(MediaType.APPLICATION_JSON)
            .body(startProcess)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });
        return requireSuccess(response, "标准地址审批流程发起失败");
    }

    /**
     * 目的：办理 workflow 通过任务。
     * 入参：办理任务参数。
     * 出参：是否成功。
     * 关键约束：`taskId` 必须是当前待办任务。
     * 异常与副作用：workflow 调用失败时抛出业务异常。
     */
    public boolean completeTask(RemoteCompleteTask completeTask) {
        R<Void> response = client().post()
            .uri("/task/completeTask")
            .contentType(MediaType.APPLICATION_JSON)
            .body(completeTask)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });
        requireSuccess(response, "标准地址审批通过失败");
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
        R<Void> response = client().post()
            .uri("/task/backProcess")
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });
        requireSuccess(response, "标准地址审批驳回失败");
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
        R<Boolean> response = client().post()
            .uri("/task/terminationTask")
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });
        Boolean result = requireSuccess(response, "标准地址审批驳回失败");
        return Boolean.TRUE.equals(result);
    }

    /**
     * 目的：按业务 ID 查询 workflow 实例主键。
     * 入参：业务 ID。
     * 出参：流程实例主键。
     * 关键约束：业务 ID 必须与审批申请单主键一致。
     * 异常与副作用：workflow 调用失败时抛出业务异常。
     */
    public Long getInstanceIdByBusinessId(String businessId) {
        R<Map<String, Object>> response = client().get()
            .uri("/instance/getInfo/{businessId}", businessId)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });
        Map<String, Object> data = requireSuccess(response, "标准地址审批实例查询失败");
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
        R<Map<String, Object>> response = client().get()
            .uri("/instance/flowHisTaskList/{businessId}", businessId)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });
        Map<String, Object> data = requireSuccess(response, "标准地址审批历史查询失败");
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
        return restClientBuilder.baseUrl(workflowBaseUrl).build();
    }

    private <T> T requireSuccess(R<T> response, String message) {
        if (response == null || !R.isSuccess(response)) {
            throw new ServiceException(response == null ? message : Convert.toStr(response.getMsg(), message));
        }
        return response.getData();
    }
}
