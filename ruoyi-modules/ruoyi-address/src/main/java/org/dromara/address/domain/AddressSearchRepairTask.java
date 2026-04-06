package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 地址搜索修复任务实体。
 * <p>
 * 目的：持久化记录 ES 补偿失败后待执行的 repair 动作，支撑后续维护入口按“删除脏文档”或“重建文档”重放。
 * 关键约束：修复任务只在同步补偿失败时创建，状态流转由维护服务统一推进；`payloadJson` 仅保存 repair 所需最小上下文。
 * 异常与副作用：仅作为表结构映射使用，不主动访问数据库或远程服务。
 * </p>
 */
@Data
@TableName("address_search_repair_task")
public class AddressSearchRepairTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID。
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 实体类型。
     */
    private String entityType;

    /**
     * 实体主键。
     */
    private String entityId;

    /**
     * 修复动作。
     */
    private String repairAction;

    /**
     * 修复负载 JSON。
     */
    private String payloadJson;

    /**
     * 任务状态。
     */
    private String status;

    /**
     * 重试次数。
     */
    private Integer retryCount;

    /**
     * 创建时间。
     */
    private Date createdTime;

    /**
     * 更新时间。
     */
    private Date updatedTime;
}
