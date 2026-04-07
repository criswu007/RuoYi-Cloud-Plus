package org.dromara.address.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 标准地址审批申请查询 BO。
 * 目的：承载“我提交的”审批单分页筛选条件。
 * 入参/出参：输入分页筛选字段，输出由服务层转换为查询条件。
 * 关键约束：当前版本只支持按操作类型、审批状态和关键字过滤。
 * 异常与副作用：无写入副作用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StandardAddressApprovalBo extends BaseEntity {

    /**
     * 操作类型。
     */
    private String operationType;

    /**
     * 审批状态。
     */
    private String approvalStatus;

    /**
     * 标题/摘要关键字。
     */
    private String keyword;
}
