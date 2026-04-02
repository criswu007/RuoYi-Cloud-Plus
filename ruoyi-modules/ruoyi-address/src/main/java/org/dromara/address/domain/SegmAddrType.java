package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 地址类型与层级解释表 `segm_addr_type` 实体。
 */
@Data
@TableName("segm_addr_type")
public class SegmAddrType implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 地址类型编码。
     */
    @TableId(value = "addr_type_id")
    private String addrTypeId;

    /**
     * 地址类型名称。
     */
    @TableField("name")
    private String addrTypeName;

    /**
     * 父类型编码。
     */
    @TableField(exist = false)
    private String parentAddrTypeId;

    /**
     * 层级 ID。
     */
    private Integer levelId;
}
