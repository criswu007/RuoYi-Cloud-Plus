package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 监控任务运行日志实体。
 * 目的：记录任务每次执行的触发方式、运行状态和统计结果，支撑任务详情与后续重跑审计。
 * 入参/出参：作为运行日志表持久化对象供 Mapper 与 Service 使用。
 * 关键约束：单次执行创建一条日志，状态流转统一由任务执行链路维护。
 * 异常与副作用：实体本身无异常与副作用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("address_standard_monitor_task_run_log")
public class StandardAddressMonitorTaskRunLog extends TenantEntity {

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
     * 触发方式
     */
    private String triggerMode;

    /**
     * 执行状态
     */
    private String executeStatus;

    /**
     * 执行消息
     */
    private String executeMessage;

    /**
     * 扫描数量
     */
    private Long scannedCount;

    /**
     * 命中数量
     */
    private Long hitCount;

    /**
     * 新增异常数量
     */
    private Long createdCount;

    /**
     * 开始时间
     */
    private Date startedTime;

    /**
     * 结束时间
     */
    private Date finishedTime;
}
