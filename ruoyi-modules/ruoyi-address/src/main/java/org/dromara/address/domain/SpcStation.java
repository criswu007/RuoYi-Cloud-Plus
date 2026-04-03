package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理站主数据表 `spc_station` 实体。
 */
@Data
@TableName("spc_station")
public class SpcStation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 管理站 ID。
     */
    @TableId(value = "station_id")
    private String stationId;

    /**
     * 管理站名称，对应线上 `spc_station.china_name`。
     */
    @TableField("china_name")
    private String stationName;

    /**
     * 区域 ID。
     */
    private String regionId;

    /**
     * 管理站类型，对应 `pub_restriction.keyword='MANAGE_TYPE'` 的字典值。
     */
    @TableField("manage_type")
    private String manageType;
}
