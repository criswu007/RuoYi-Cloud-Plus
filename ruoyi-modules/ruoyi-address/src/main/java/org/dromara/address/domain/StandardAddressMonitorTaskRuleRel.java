package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 监控任务与规则关系实体。
 * 目的：保存监控任务绑定的规则集合，支撑任务管理页与执行链路按规则编排。
 * 入参/出参：作为任务规则关系表的持久化对象供 Mapper 与 Service 使用。
 * 关键约束：同一任务下 `taskId + ruleId` 应保持唯一。
 * 异常与副作用：实体本身无异常与副作用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("address_standard_monitor_task_rule_rel")
public class StandardAddressMonitorTaskRuleRel extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;
}
