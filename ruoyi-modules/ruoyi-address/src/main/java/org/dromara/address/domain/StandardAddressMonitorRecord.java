package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 标准地址监控记录实体。
 * 目的：保存异常地址预警记录与命中快照，支撑管理端治理闭环。
 * 入参/出参：作为记录 Mapper、Service 与 BO/VO 的核心持久化对象。
 * 关键约束：记录需要保留来源任务、规则与地址快照，避免后续主数据变化影响展示。
 * 异常与副作用：实体本身无异常与副作用，状态流转由服务层控制。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("address_standard_monitor_record")
public class StandardAddressMonitorRecord extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 标准地址ID
     */
    private Long standardAddressId;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 标准地址名称快照
     */
    private String standNameSnapshot;

    /**
     * 区域快照
     */
    private String regionIdSnapshot;

    /**
     * 规则名称快照
     */
    private String ruleNameSnapshot;

    /**
     * 规则模板快照
     */
    private String ruleTemplateSnapshot;

    /**
     * 来源任务ID
     */
    private Long taskId;

    /**
     * 来源任务运行日志ID
     */
    private Long taskRunLogId;

    /**
     * 来源任务名称快照
     */
    private String taskNameSnapshot;

    /**
     * 严重等级
     */
    private String severity;

    /**
     * 命中详情JSON
     */
    private String hitDetailJson;

    /**
     * 去重键
     */
    private String dedupKey;

    /**
     * 首次发现时间
     */
    private java.util.Date firstDetectedTime;

    /**
     * 最近发现时间
     */
    private java.util.Date lastDetectedTime;

    /**
     * 命中次数
     */
    private Integer hitCount;

    /**
     * 工单ID
     */
    private Long workOrderId;

    /**
     * 工单号
     */
    private String workOrderNo;

    /**
     * 工单状态
     */
    private String workOrderStatus;

    /**
     * 状态（0待处理 1已忽略 2已生成工单 3已修正 4已驳回）
     */
    private String status;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

    /**
     * 处理人
     */
    private String processBy;

    /**
     * 处理时间
     */
    private java.util.Date processTime;

}
