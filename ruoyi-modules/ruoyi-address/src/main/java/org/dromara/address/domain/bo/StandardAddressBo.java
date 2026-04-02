package org.dromara.address.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.address.domain.AddrSegm;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import jakarta.validation.constraints.NotBlank;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = AddrSegm.class, reverseConvertGenerate = false)
public class StandardAddressBo extends BaseEntity {

    /**
     * 标准地址主键，沿用旧模块数值主键以兼容未迁移逻辑。
     */
    private Long id;

    /**
     * 标准地址主键字符串表示，面向线上 `ADDR_SEGM.segm_id` 契约。
     */
    private String segmId;

    /**
     * 父标准地址主键，沿用旧模块数值主键以兼容未迁移逻辑。
     */
    private Long parentId;

    /**
     * 父标准地址字符串主键，面向线上 `ADDR_SEGM.parent_segm_id` 契约。
     */
    private String parentSegmId;

    /**
     * 当级标准地址名称。
     */
    @NotBlank(message = "地址名称不能为空")
    private String name;

    /**
     * 当级标准地址名称，面向线上 `ADDR_SEGM.segm_name` 契约。
     */
    private String segmName;

    /**
     * 标准地址全称。
     */
    private String fullName;

    /**
     * 标准地址全称，面向线上 `ADDR_SEGM.stand_name` 契约。
     */
    private String standName;

    /**
     * 旧模块地址编码。
     */
    private String code;

    /**
     * 地址编码，面向线上 `ADDR_SEGM.segm_no` 契约。
     */
    private String segmNo;

    /**
     * 标准地址编码，面向线上 `ADDR_SEGM.stand_no` 契约。
     */
    private String standNo;

    /**
     * 旧模块层级。
     */
    private Integer level;

    /**
     * 地址业务级别（1-19），用于前后端交互与层级校验。
     */
    private Integer addrLevel;

    /**
     * 数据库真实层级 ID，来源于 `segm_addr_type.level_id`。
     */
    private Integer levelId;

    /**
     * 线上地址类型编码，映射 `ADDR_SEGM.segm_type`。
     */
    private String segmType;

    /**
     * 省份编码。
     */
    private String provinceCode;

    /**
     * 城市编码。
     */
    private String cityCode;

    /**
     * 区县编码。
     */
    private String districtCode;

    /**
     * 街道编码。
     */
    private String streetCode;

    /**
     * 社区/村编码。
     */
    private String villageCode;

    /**
     * 状态（0正常 1停用）。
     */
    private String status;

    /**
     * 区域 ID，对应线上 `ADDR_SEGM.region_id`。
     */
    private String regionId;

    /**
     * 行政区 ID，对应线上 `ADDR_SEGM.district_id`。
     */
    private String districtId;

    /**
     * 服务区域 ID，对应线上 `ADDR_SEGM.service_region_id`。
     */
    private String serviceRegionId;

    /**
     * 管理站 ID，对应线上 `ADDR_SEGM.station_id`。
     */
    private String stationId;

    /**
     * 安装站 ID，对应线上 `ADDR_SEGM.installstation_id`。
     */
    private String installStationId;

    /**
     * 营业站 ID，对应线上 `ADDR_SEGM.busstation_id`。
     */
    private String busStationId;

    /**
     * 单项工程编号，对应线上 `ADDR_SEGM.post_code`。
     */
    private String singleProjectCode;

    /**
     * 是否配套费小区，对应线上 `ADDR_SEGM.segm_name_fir`。
     */
    private String supportingFeeCommunityFlag;

    /**
     * 城区/非城区标识，对应线上 `ADDR_SEGM.is_city`。
     */
    private String isCity;

    /**
     * 光纤接入方式，对应线上 `ADDR_SEGM.addr_in_type_ftth`。
     */
    private Integer addrInTypeFtth;

    /**
     * 光纤接入能力，对应线上 `ADDR_SEGM.ftth_pon_type`。
     */
    private Integer ftthPonType;

    /**
     * 电缆接入方式，对应线上 `ADDR_SEGM.addr_in_type_lan`。
     */
    private Integer addrInTypeLan;

    /**
     * 城乡属性，对应线上 `ADDR_SEGM.area_type`。
     */
    private Integer areaType;

    /**
     * 场所性质，对应线上 `ADDR_SEGM.place_type`。
     */
    private Integer placeType;

    /**
     * 楼栋覆盖户数，对应线上 `ADDR_SEGM.cover_num`。
     */
    private Integer coverNum;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 备注，面向线上 `ADDR_SEGM.notes` 契约。
     */
    private String notes;

    public String getSegmId() {
        if (segmId != null) {
            return segmId;
        }
        return id == null ? null : String.valueOf(id);
    }

    public void setSegmId(String segmId) {
        this.segmId = segmId;
    }

    public String getParentSegmId() {
        if (parentSegmId != null) {
            return parentSegmId;
        }
        return parentId == null ? null : String.valueOf(parentId);
    }

    public void setParentSegmId(String parentSegmId) {
        this.parentSegmId = parentSegmId;
    }

    public String getSegmName() {
        return segmName != null ? segmName : name;
    }

    public void setSegmName(String segmName) {
        this.segmName = segmName;
        this.name = segmName;
    }

    public String getStandName() {
        return standName != null ? standName : fullName;
    }

    public void setStandName(String standName) {
        this.standName = standName;
        this.fullName = standName;
    }

    public String getSegmNo() {
        return segmNo != null ? segmNo : code;
    }

    public void setSegmNo(String segmNo) {
        this.segmNo = segmNo;
        this.code = segmNo;
    }

    public String getStandNo() {
        return standNo;
    }

    public void setStandNo(String standNo) {
        this.standNo = standNo;
    }

    public Integer getAddrLevel() {
        return addrLevel != null ? addrLevel : level;
    }

    public void setAddrLevel(Integer addrLevel) {
        this.addrLevel = addrLevel;
        this.level = addrLevel;
    }

    public Integer getLevelId() {
        return levelId;
    }

    public void setLevelId(Integer levelId) {
        this.levelId = levelId;
    }

    public String getNotes() {
        return notes != null ? notes : remark;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.remark = notes;
    }

    public void setName(String name) {
        this.name = name;
        this.segmName = name;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
        this.standName = fullName;
    }

    public void setCode(String code) {
        this.code = code;
        this.segmNo = code;
    }

    public void setLevel(Integer level) {
        this.level = level;
        this.addrLevel = level;
    }

    public void setRemark(String remark) {
        this.remark = remark;
        this.notes = remark;
    }
}
