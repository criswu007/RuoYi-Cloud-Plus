package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 区域主数据表 `spc_region` 实体。
 */
@Data
@TableName("spc_region")
public class SpcRegion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 区域 ID。
     */
    @TableId(value = "region_id")
    private String regionId;

    /**
     * 上级区域 ID。
     */
    private String superRegionId;

    /**
     * 区域名称。
     */
    private String regionName;

    /**
     * 区域编码。
     */
    private String regionNo;

    /**
     * 备注。
     */
    private String notes;
}
