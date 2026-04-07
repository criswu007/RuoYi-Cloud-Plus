package org.dromara.address.mapper;

import org.dromara.address.domain.StandardAddressApproval;
import org.dromara.address.domain.vo.StandardAddressApprovalVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 标准地址审批申请单 Mapper。
 * 目的：提供审批申请单的基础持久化能力。
 * 入参/出参：输入申请单实体，输出审批展示 VO。
 * 关键约束：当前实现优先复用 MyBatis-Plus 通用能力，复杂聚合在服务层完成。
 * 异常与副作用：直接操作 `address_standard_approval` 表。
 */
public interface StandardAddressApprovalMapper extends BaseMapperPlus<StandardAddressApproval, StandardAddressApprovalVo> {
}
