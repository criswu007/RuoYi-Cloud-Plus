package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 标准地址操作日志表对象 address_standard_operation_log
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("address_standard_operation_log")
public class StandardAddressOperationLog extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 关联标准地址ID
     */
    private Long standardAddressId;

    /**
     * 操作类型（MERGE, SPLIT, DELETE, IMPORT）
     */
    private String operationType;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作时间
     */
    private Date operateTime;

    /**
     * 操作详情
     */
    private String details;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    // 屏蔽父类不存在的字段
    @TableField(exist = false)
    private Long createDept;

    @TableField(exist = false)
    private Long createBy;

    @TableField(exist = false)
    private Date createTime;

    @TableField(exist = false)
    private Long updateBy;

    @TableField(exist = false)
    private Date updateTime;

}
