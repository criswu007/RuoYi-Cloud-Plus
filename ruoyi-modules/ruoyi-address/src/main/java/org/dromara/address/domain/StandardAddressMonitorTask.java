package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 标准地址监控任务实体。
 * 目的：保存监控任务定义、调度信息与最近执行结果，为任务管理页面提供持久化模型。
 * 入参/出参：作为任务 Mapper、Service 与任务管理 VO 的核心持久化对象。
 * 关键约束：任务状态、执行规则与 snailjob 任务标识统一在该实体中维护，范围与规则关联由关系表承接。
 * 异常与副作用：实体本身无异常与副作用，状态流转与调度同步由服务层负责。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("address_standard_monitor_task")
public class StandardAddressMonitorTask extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 执行规则
     */
    private String executeRule;

    /**
     * 监控范围
     */
    private String monitorScope;

    /**
     * 任务状态
     */
    private String taskStatus;

    /**
     * 任务说明
     */
    private String taskDesc;

    /**
     * snailjob任务ID
     */
    private Long snailJobTaskId;

    /**
     * 最近执行时间
     */
    private Date lastExecuteTime;

    /**
     * 最近成功时间
     */
    private Date lastSuccessTime;

    /**
     * 最近失败原因
     */
    private String lastFailureReason;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;
}
