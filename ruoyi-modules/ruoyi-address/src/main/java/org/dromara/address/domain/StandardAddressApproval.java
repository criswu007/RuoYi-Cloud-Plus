package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 标准地址审批申请单实体。
 * 目的：承载标准地址变更申请的业务快照、流程状态和审批结果，保证审批前不污染正式地址主表。
 * 入参/出参：作为 Mapper 持久化实体使用，和审批服务、workflow 监听器共享。
 * 关键约束：`id` 同时作为 workflow `businessId`；原地址、新地址和请求参数统一以 JSON 快照保存。
 * 异常与副作用：审批通过后会由监听器驱动正式地址写链路执行；实体本身只负责状态落库。
 */
@Data
@TableName("address_standard_approval")
@EqualsAndHashCode(callSuper = true)
public class StandardAddressApproval extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 申请单主键，同时作为 workflow 业务 ID。
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 申请单号。
     */
    private String applyNo;

    /**
     * 操作类型：ADD/IMPORT/UPDATE/DELETE/MERGE/SPLIT。
     */
    private String operationType;

    /**
     * workflow 流程编码。
     */
    private String flowCode;

    /**
     * workflow 业务状态。
     */
    private String businessStatus;

    /**
     * 页面归一化审批状态：WAITING/APPROVED/REJECTED/EXECUTING/EXECUTE_FAILED。
     */
    private String approvalStatus;

    /**
     * 流程实例 ID。
     */
    private Long instanceId;

    /**
     * 当前待办任务 ID。
     */
    private Long currentTaskId;

    /**
     * 业务标题。
     */
    private String bizTitle;

    /**
     * 原地址摘要。
     */
    private String sourceSummary;

    /**
     * 新地址摘要。
     */
    private String targetSummary;

    /**
     * 原地址快照 JSON。
     */
    private String sourceSnapshot;

    /**
     * 新地址快照 JSON。
     */
    private String targetSnapshot;

    /**
     * 原始请求参数 JSON。
     */
    private String requestPayload;

    /**
     * 提交内容指纹。
     */
    private String submitFingerprint;

    /**
     * 重复提交保护占位键；活跃审批统一为 ACTIVE，审批结束后释放为申请单号。
     */
    private String submitGuardKey;

    /**
     * 导入批次内容 JSON。
     */
    private String importBatchPayload;

    /**
     * 提交人 ID。
     */
    private Long submitUserId;

    /**
     * 提交人账号。
     */
    private String submitUserName;

    /**
     * 提交部门 ID。
     */
    private Long submitDeptId;

    /**
     * 提交部门名称。
     */
    private String submitDeptName;

    /**
     * 审批人 ID。
     */
    private Long approveUserId;

    /**
     * 审批人名称。
     */
    private String approveUserName;

    /**
     * 审批时间。
     */
    private Date approveTime;

    /**
     * 驳回原因。
     */
    private String rejectReason;

    /**
     * 正式执行信息。
     */
    private String executeMessage;

    /**
     * 删除标记。
     */
    private String delFlag;
}
