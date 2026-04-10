package org.dromara.address.listener;

import cn.hutool.core.convert.Convert;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.address.domain.StandardAddressApproval;
import org.dromara.address.domain.StandardAddressImportBatch;
import org.dromara.address.domain.StandardAddressImportDetail;
import org.dromara.address.mapper.StandardAddressApprovalMapper;
import org.dromara.address.mapper.StandardAddressImportBatchMapper;
import org.dromara.address.mapper.StandardAddressImportDetailMapper;
import org.dromara.address.service.IStandardAddressImportBatchService;
import org.dromara.address.service.impl.StandardAddressApprovalExecutor;
import org.dromara.address.service.impl.StandardAddressApprovalService;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.workflow.api.event.ProcessDeleteEvent;
import org.dromara.workflow.api.event.ProcessEvent;
import org.dromara.workflow.api.event.ProcessTaskEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * 标准地址 workflow 监听器。
 * 目的：监听地址审批流程的提交、待办创建、通过、驳回和删除事件，同步申请单状态并驱动正式执行。
 * 入参/出参：输入 workflow 远程事件，输出为申请单状态回写和正式执行。
 * 关键约束：只处理 `address_standard_approve_v1` 流程；审批通过后才允许触发正式地址写链路。
 * 异常与副作用：会更新审批申请单；审批通过时会进一步修改正式地址表与 ES。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StandardAddressWorkflowListener {

    public static final String FLOW_CODE = StandardAddressApprovalService.FLOW_CODE;

    private final StandardAddressApprovalMapper approvalMapper;
    private final StandardAddressApprovalExecutor approvalExecutor;
    private final StandardAddressImportDetailMapper importDetailMapper;
    private final StandardAddressImportBatchMapper importBatchMapper;
    private final IStandardAddressImportBatchService importBatchService;

    /**
     * 目的：处理流程状态变化事件。
     * 入参：workflow 流程事件。
     * 出参：无。
     * 关键约束：仅处理地址审批流程；流程通过时必须先执行正式写链路，再回写审批通过状态。
     * 异常与副作用：会更新申请单状态，审批通过时会触发正式地址写入。
     */
    @EventListener(condition = "#processEvent.flowCode == 'address_standard_approve_v1'")
    public void processHandler(ProcessEvent processEvent) {
        TenantHelper.dynamic(processEvent.getTenantId(), () -> {
            StandardAddressApproval approval = approvalMapper.selectById(Convert.toLong(processEvent.getBusinessId()));
            if (approval == null) {
                return;
            }
            approval.setBusinessStatus(processEvent.getStatus());
            Map<String, Object> params = processEvent.getParams();
            if (processEvent.getSubmit()) {
                approval.setApprovalStatus(StandardAddressApprovalService.APPROVAL_WAITING);
                approvalMapper.updateById(approval);
                syncImportRowResult(approval, StandardAddressImportDetail.STATUS_WAITING_APPROVAL, null);
                return;
            }
            if (BusinessStatusEnum.FINISH.getStatus().equals(processEvent.getStatus())) {
                handleApproved(approval, params);
                return;
            }
            if (BusinessStatusEnum.BACK.getStatus().equals(processEvent.getStatus())
                || BusinessStatusEnum.TERMINATION.getStatus().equals(processEvent.getStatus())) {
                approval.setApprovalStatus(StandardAddressApprovalService.APPROVAL_REJECTED);
                approval.setRejectReason(Convert.toStr(params == null ? null : params.get("message")));
                approval.setApproveUserId(Convert.toLong(params == null ? null : params.get("handler")));
                approval.setApproveUserName(Convert.toStr(params == null ? null : params.get("handler")));
                approval.setApproveTime(new Date());
                approvalMapper.updateById(approval);
                syncImportRowResult(approval, StandardAddressImportDetail.STATUS_REJECTED_FAILED, approval.getRejectReason());
                return;
            }
            approvalMapper.updateById(approval);
        });
    }

    /**
     * 目的：处理待办任务创建事件。
     * 入参：workflow 任务事件。
     * 出参：无。
     * 关键约束：当前版本只回填当前待办任务 ID 和审批状态。
     * 异常与副作用：会更新申请单当前任务字段。
     */
    @EventListener(condition = "#processTaskEvent.flowCode == 'address_standard_approve_v1'")
    public void processTaskHandler(ProcessTaskEvent processTaskEvent) {
        TenantHelper.dynamic(processTaskEvent.getTenantId(), () -> {
            StandardAddressApproval approval = approvalMapper.selectById(Convert.toLong(processTaskEvent.getBusinessId()));
            if (approval == null) {
                return;
            }
            approval.setCurrentTaskId(processTaskEvent.getTaskId());
            approval.setApprovalStatus(StandardAddressApprovalService.APPROVAL_WAITING);
            approvalMapper.updateById(approval);
            syncImportRowResult(approval, StandardAddressImportDetail.STATUS_WAITING_APPROVAL, null);
        });
    }

    /**
     * 目的：处理流程实例删除事件。
     * 入参：workflow 删除事件。
     * 出参：无。
     * 关键约束：流程删除不删除审批申请单，只清空实例关联字段。
     * 异常与副作用：会更新申请单实例、待办字段。
     */
    @EventListener(condition = "#processDeleteEvent.flowCode == 'address_standard_approve_v1'")
    public void processDeleteHandler(ProcessDeleteEvent processDeleteEvent) {
        TenantHelper.dynamic(processDeleteEvent.getTenantId(), () -> {
            StandardAddressApproval approval = approvalMapper.selectById(Convert.toLong(processDeleteEvent.getBusinessId()));
            if (approval == null) {
                return;
            }
            approval.setInstanceId(null);
            approval.setCurrentTaskId(null);
            approvalMapper.updateById(approval);
        });
    }

    /**
     * 目的：处理审批通过后的正式执行与状态回写。
     * 入参：审批申请单与流程扩展参数。
     * 出参：无。
     * 关键约束：正式执行失败时必须保留失败信息，便于后续人工排查，不允许误标审批通过。
     * 异常与副作用：会修改正式地址表；同时回写申请单审批结果。
     */
    private void handleApproved(StandardAddressApproval approval, Map<String, Object> params) {
        try {
            approval.setApprovalStatus(StandardAddressApprovalService.APPROVAL_EXECUTING);
            approvalMapper.updateById(approval);
            syncImportRowResult(approval, StandardAddressImportDetail.STATUS_WAITING_APPROVAL, null);
            approvalExecutor.execute(approval);
            approval.setApprovalStatus(StandardAddressApprovalService.APPROVAL_APPROVED);
            approval.setApproveUserId(Convert.toLong(params == null ? null : params.get("handler")));
            approval.setApproveUserName(Convert.toStr(params == null ? null : params.get("handler")));
            approval.setApproveTime(new Date());
            approval.setExecuteMessage("正式地址变更已生效");
            syncImportRowResult(approval, StandardAddressImportDetail.STATUS_APPROVED_SUCCESS, null);
        } catch (Exception ex) {
            log.error("标准地址审批执行失败，approvalId={}", approval.getId(), ex);
            approval.setApprovalStatus(StandardAddressApprovalService.APPROVAL_EXECUTE_FAILED);
            approval.setExecuteMessage(StringUtils.defaultIfBlank(ex.getMessage(), "正式地址变更执行失败"));
            syncImportRowResult(approval, StandardAddressImportDetail.STATUS_EXECUTE_FAILED, approval.getExecuteMessage());
        }
        approvalMapper.updateById(approval);
    }

    private void syncImportRowResult(StandardAddressApproval approval, String rowStatus, String failReason) {
        StandardAddressApprovalService.ImportApprovalPayload payload = resolveImportPayload(approval);
        if (payload == null || payload.getItemId() == null) {
            return;
        }
        StandardAddressImportDetail detail = importDetailMapper.selectById(payload.getItemId());
        if (detail == null) {
            return;
        }
        detail.setApprovalId(approval.getId());
        detail.setApprovalNo(approval.getApplyNo());
        detail.setApprovalStatus(approval.getApprovalStatus());
        detail.setStatus(rowStatus);
        if (StringUtils.isNotBlank(failReason)) {
            detail.setFailReason(failReason);
        } else if (StandardAddressImportDetail.STATUS_WAITING_APPROVAL.equals(rowStatus)
            || StandardAddressImportDetail.STATUS_APPROVED_SUCCESS.equals(rowStatus)) {
            detail.setFailReason(null);
        }
        importDetailMapper.updateById(detail);
        if (payload.getBatchId() != null) {
            if (StringUtils.isNotBlank(failReason)) {
                StandardAddressImportBatch update = new StandardAddressImportBatch();
                update.setId(payload.getBatchId());
                update.setErrorMsg(failReason);
                importBatchMapper.updateById(update);
            }
            importBatchService.refreshBatchSummary(payload.getBatchId());
        }
    }

    private StandardAddressApprovalService.ImportApprovalPayload resolveImportPayload(StandardAddressApproval approval) {
        if (!StandardAddressApprovalService.OPERATION_IMPORT.equals(approval.getOperationType())
            || StringUtils.isBlank(approval.getRequestPayload())) {
            return null;
        }
        try {
            StandardAddressApprovalService.ImportApprovalPayload payload = new ObjectMapper()
                .readValue(approval.getRequestPayload(), StandardAddressApprovalService.ImportApprovalPayload.class);
            return payload != null && payload.getItemId() != null ? payload : null;
        } catch (Exception ex) {
            log.warn("解析导入审批负载失败，approvalId={}", approval.getId(), ex);
            return null;
        }
    }
}
