package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 监控任务范围关系实体。
 * 目的：保存监控任务的区域或地址范围明细，避免主表直接承载集合字段。
 * 入参/出参：作为任务范围关系表的持久化对象供 Mapper 与 Service 使用。
 * 关键约束：`scopeType` 首版仅允许 `REGION/ADDRESS`，全量任务不写明细。
 * 异常与副作用：实体本身无异常与副作用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("address_standard_monitor_task_scope_rel")
public class StandardAddressMonitorTaskScopeRel extends TenantEntity {

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
     * 范围类型
     */
    private String scopeType;

    /**
     * 范围值
     */
    private String scopeValue;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;
}
