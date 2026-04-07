package org.dromara.address.workflow;

import lombok.RequiredArgsConstructor;
import org.dromara.workflow.api.RemoteWorkflowService;
import org.dromara.workflow.api.domain.RemoteCompleteTask;
import org.dromara.workflow.api.domain.RemoteStartProcess;
import org.dromara.workflow.api.domain.RemoteStartProcessReturn;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 标准地址 standalone workflow 远程服务适配器。
 * 目的：在 `standalone` 模式下以 HTTP 方式实现 `RemoteWorkflowService`，复用现有审批提交服务逻辑。
 * 入参/出参：输入 workflow 远程服务接口参数，输出 HTTP 调用结果。
 * 关键约束：当前仅保证标准地址审批链路所需方法可用，其余未使用方法维持最小实现。
 * 异常与副作用：会请求 workflow HTTP 接口，失败时抛出业务异常。
 */
@Primary
@Service
@Profile("standalone")
@RequiredArgsConstructor
public class StandaloneRemoteWorkflowService implements RemoteWorkflowService {

    private final AddressWorkflowHttpClient workflowHttpClient;

    @Override
    public boolean deleteInstance(List<String> businessIds) {
        throw new UnsupportedOperationException("standalone 模式未实现 deleteInstance");
    }

    @Override
    public String getBusinessStatusByTaskId(Long taskId) {
        throw new UnsupportedOperationException("standalone 模式未实现 getBusinessStatusByTaskId");
    }

    @Override
    public String getBusinessStatus(String businessId) {
        throw new UnsupportedOperationException("standalone 模式未实现 getBusinessStatus");
    }

    @Override
    public void setVariable(Long instanceId, Map<String, Object> variable) {
        throw new UnsupportedOperationException("standalone 模式未实现 setVariable");
    }

    @Override
    public Map<String, Object> instanceVariable(Long instanceId) {
        throw new UnsupportedOperationException("standalone 模式未实现 instanceVariable");
    }

    @Override
    public Long getInstanceIdByBusinessId(String businessId) {
        return workflowHttpClient.getInstanceIdByBusinessId(businessId);
    }

    @Override
    public void syncDef(String tenantId) {
    }

    @Override
    public RemoteStartProcessReturn startWorkFlow(RemoteStartProcess startProcess) {
        return workflowHttpClient.startWorkFlow(startProcess);
    }

    @Override
    public boolean completeTask(RemoteCompleteTask completeTask) {
        return workflowHttpClient.completeTask(completeTask);
    }

    @Override
    public boolean completeTask(Long taskId, String message) {
        RemoteCompleteTask completeTask = new RemoteCompleteTask();
        completeTask.setTaskId(taskId);
        completeTask.setMessage(message);
        Map<String, Object> variables = new java.util.HashMap<>(2);
        variables.put("ignore", true);
        completeTask.setVariables(variables);
        return workflowHttpClient.completeTask(completeTask);
    }

    @Override
    public boolean startCompleteTask(RemoteStartProcess startProcess) {
        workflowHttpClient.startCompleteTask(startProcess);
        return true;
    }
}
