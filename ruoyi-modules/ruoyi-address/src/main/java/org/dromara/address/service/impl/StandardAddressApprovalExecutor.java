package org.dromara.address.service.impl;

import org.dromara.address.domain.StandardAddressApproval;

/**
 * 标准地址审批正式执行器。
 * 目的：在审批通过后根据申请单快照执行正式地址变更。
 * 入参/出参：输入审批申请单，输出无；执行结果通过异常与申请单状态回写体现。
 * 关键约束：所有正式写入都只能在审批通过后触发；执行必须复用现有标准地址命令服务与导入服务。
 * 异常与副作用：会修改正式地址表、导入记录和 ES 索引。
 */
public interface StandardAddressApprovalExecutor {

    /**
     * 目的：执行单个审批申请对应的正式变更。
     * 入参：审批申请单。
     * 出参：无。
     * 关键约束：调用方需保证申请单已经审批通过且尚未正式执行成功。
     * 异常与副作用：执行失败时抛出异常，由上层统一记录执行失败信息。
     */
    void execute(StandardAddressApproval approval);
}
