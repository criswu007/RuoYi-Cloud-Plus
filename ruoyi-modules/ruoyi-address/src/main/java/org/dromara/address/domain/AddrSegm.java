package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
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
     * 单项工程编号，对应物理字段 `post_code`。
     */
    @TableField("post_code")
    private String singleProjectCode;

    /**
     * 城区/非城区标识，对应物理字段 `is_city`。
     */
    private String isCity;

    /**
     * 是否配套费小区，对应物理字段 `segm_name_fir`。
     */
    @TableField("segm_name_fir")
    private String supportingFeeCommunityFlag;

    /**
     * 场所性质，对应物理字段 `place_type`。
     */
    private Integer placeType;

    /**
     * 楼栋覆盖户数，对应物理字段 `cover_num`。
     */
    private Integer coverNum;

    /**
     * 备注。
     */
    private String notes;

    /**
     * 光纤接入方式，对应物理字段 `addr_in_type_ftth`。
     */
    private Integer addrInTypeFtth;

    /**
     * 光纤接入能力，对应物理字段 `ftth_pon_type`。
     */
    private Integer ftthPonType;

    /**
     * 电缆接入方式，对应物理字段 `addr_in_type_lan`。
     */
    private Integer addrInTypeLan;

    /**
     * 城乡属性，对应物理字段 `area_type`。
     */
    private Integer areaType;

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
