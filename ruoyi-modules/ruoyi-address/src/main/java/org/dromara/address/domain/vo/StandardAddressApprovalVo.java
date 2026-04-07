package org.dromara.address.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 标准地址审批申请展示 VO。
 * 目的：输出审批记录列表和详情所需的展示字段。
 * 入参/出参：由审批服务聚合实体、workflow 状态与快照后返回前端。
 * 关键约束：快照仍以 JSON 原文保留，便于前端详情抽屉按需渲染。
 * 异常与副作用：无写入副作用。
 */
@Data
@ExcelIgnoreUnannotated
public class StandardAddressApprovalVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @ExcelProperty("申请单号")
    private String applyNo;

    @ExcelProperty("操作类型")
    private String operationType;

    private String approvalStatus;

    @ExcelProperty("审批结果")
    private String approvalStatusName;

    private String businessStatus;

    @ExcelProperty("业务标题")
    private String bizTitle;

    @ExcelProperty("原地址摘要")
    private String sourceSummary;

    @ExcelProperty("新地址摘要")
    private String targetSummary;

    private Long instanceId;

    private Long currentTaskId;

    @ExcelProperty("提交人")
    private String submitUserName;

    @ExcelProperty("提交部门")
    private String submitDeptName;

    private Long approveUserId;

    @ExcelProperty("审批人")
    private String approveUserName;

    @ExcelProperty("审批时间")
    private Date approveTime;

    @ExcelProperty("驳回原因")
    private String rejectReason;

    @ExcelProperty("执行信息")
    private String executeMessage;

    private String sourceSnapshot;

    private String targetSnapshot;

    private String requestPayload;

    private String importBatchPayload;

    @ExcelProperty("提交时间")
    private Date createTime;
}
