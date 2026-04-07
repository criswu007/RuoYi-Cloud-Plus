package org.dromara.address.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 标准地址审批动作参数。
 * 目的：承载标准地址审批通过/驳回动作所需的 workflow 上下文。
 * 入参/出参：输入 workflow 任务标识、业务标识、当前节点编码与审批意见，输出供控制器传递到服务层。
 * 关键约束：`taskId`、`businessId` 必填；驳回时必须提供非空 `message`；`nodeCode` 仅保留给兼容场景透传。
 * 异常与副作用：无写入副作用，仅用于参数校验与跨层传递。
 */
@Data
public class StandardAddressApprovalActionBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * workflow 任务 ID。
     */
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    /**
     * 审批业务 ID，对应审批申请单主键。
     */
    @NotBlank(message = "业务ID不能为空")
    private String businessId;

    /**
     * 当前 workflow 节点编码，兼容保留字段。
     */
    private String nodeCode;

    /**
     * 审批意见；通过时可为空，驳回时必须填写。
     */
    private String message;
}
