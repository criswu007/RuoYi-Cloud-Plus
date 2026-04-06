package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 地址搜索运维任务实体。
 * <p>
 * 目的：持久化记录 ES 运维台触发的全量重建任务、执行进度和最终结果，支撑运维页查询与串行门禁。
 * 关键约束：仅承载运维任务状态，不承担具体 ES 执行逻辑；状态流转由任务编排服务统一维护。
 * 异常与副作用：仅作为表结构映射使用，不主动触发数据库或远程调用。
 * </p>
 */
@Data
@TableName("address_search_maintenance_task")
public class AddressSearchMaintenanceTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务主键。
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 任务类型。
     */
    private String taskType;

    /**
     * 目标别名。
     */
    private String targetAlias;

    /**
     * 当前物理索引名。
     */
    private String physicalIndexName;

    /**
     * 任务状态。
     */
    private String status;

    /**
     * 当前阶段。
     */
    private String currentPhase;

    /**
     * 总处理量。
     */
    private Long totalCount;

    /**
     * 已处理量。
     */
    private Long processedCount;

    /**
     * 进度百分比。
     */
    private Integer progressPercent;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 触发人。
     */
    private String triggerBy;

    /**
     * 开始时间。
     */
    private Date startedTime;

    /**
     * 完成时间。
     */
    private Date finishedTime;

    /**
     * 创建时间。
     */
    private Date createdTime;

    /**
     * 更新时间。
     */
    private Date updatedTime;
}
