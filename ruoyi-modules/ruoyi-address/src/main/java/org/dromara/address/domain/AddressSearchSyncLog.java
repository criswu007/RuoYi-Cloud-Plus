package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 地址搜索同步日志实体。
 * <p>
 * 目的：记录地址 ES 双写过程中每个阶段的执行结果，便于追踪“立即一致”链路是否在 ES、DB、补偿阶段出现异常。
 * 关键约束：一条日志仅描述单个实体在单个阶段的执行结果，不承载修复动作；日志写入失败不得阻断主业务异常传播。
 * 异常与副作用：仅作为表结构映射使用，不主动访问数据库或远程服务。
 * </p>
 */
@Data
@TableName("address_search_sync_log")
public class AddressSearchSyncLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID。
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 业务动作类型。
     */
    private String businessType;

    /**
     * 实体类型。
     */
    private String entityType;

    /**
     * 实体主键。
     */
    private String entityId;

    /**
     * 执行阶段。
     */
    private String phase;

    /**
     * 是否成功。
     */
    private String successFlag;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 创建时间。
     */
    private Date createdTime;
}
