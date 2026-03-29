package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 线上标准地址主事实表 `ADDR_SEGM` 实体。
 */
@Data
@TableName("ADDR_SEGM")
public class AddrSegm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标准地址主键。
     */
    @TableId(value = "segm_id")
    private String segmId;

    /**
     * 父标准地址主键。
     */
    private String parentSegmId;

    /**
     * 当级标准地址名称。
     */
    private String segmName;

    /**
     * 当级地址简拼。
     */
    private String segmNo;

    /**
     * 标准地址全称。
     */
    private String standName;

    /**
     * 标准地址全称简拼。
     */
    private String standNo;

    /**
     * 地址类型编码。
     */
    private String segmType;

    /**
     * 区域 ID。
     */
    private String regionId;

    /**
     * 行政区 ID。
     */
    private String districtId;

    /**
     * 服务区域 ID。
     */
    private String serviceRegionId;

    /**
     * 管理站 ID。
     */
    private String stationId;

    /**
     * 安装站 ID。
     */
    private String installstationId;

    /**
     * 营业站 ID。
     */
    private String busstationId;

    /**
     * 状态编码。
     */
    private String status;

    /**
     * 备注。
     */
    private String notes;

    /**
     * 创建时间。
     */
    private Date createDate;

    /**
     * 删除状态。
     */
    private String deleteState;

    /**
     * 删除时间。
     */
    private Date deleteTime;

    /**
     * 修改时间。
     */
    private Date modifyDate;
}
