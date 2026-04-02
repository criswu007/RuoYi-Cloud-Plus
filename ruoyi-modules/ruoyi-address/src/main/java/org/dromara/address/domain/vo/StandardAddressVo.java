package org.dromara.address.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.address.domain.AddrSegm;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@AutoMapper(target = AddrSegm.class)
public class StandardAddressVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标准地址主键字符串表示，面向线上 `ADDR_SEGM.segm_id` 契约。
     */
    @ExcelProperty(value = "标准地址ID")
    private String id;

    /**
     * 父标准地址主键字符串表示，面向线上 `ADDR_SEGM.parent_segm_id` 契约。
     */
    @ExcelProperty(value = "父标准地址ID")
    private String parentId;

    /**
     * 标准地址主键，面向线上 `ADDR_SEGM.segm_id` 契约。
     */
    private String segmId;

    /**
     * 父标准地址主键，面向线上 `ADDR_SEGM.parent_segm_id` 契约。
     */
    private String parentSegmId;

    /**
     * 父级标准地址名称。
     */
    private String parentStandName;

    /**
     * 当级标准地址名称。
     */
    @ExcelProperty(value = "当级名称")
    private String name;

    /**
     * 标准地址全称。
     */
    @ExcelProperty(value = "标准地址")
    private String fullName;

    /**
     * 地址编码。
     */
    @ExcelProperty(value = "地址编码")
    private String code;

    /**
     * 地址层级
     */
    @ExcelProperty(value = "地址层级")
    private Integer level;

    /**
     * 地址业务级别（1-19），用于前后端展示与交互。
     */
    private Integer addrLevel;

    /**
     * 当级标准地址名称，面向线上 `ADDR_SEGM.segm_name` 契约。
     */
    private String segmName;

    /**
     * 标准地址全称，面向线上 `ADDR_SEGM.stand_name` 契约。
     */
    private String standName;

    /**
     * 地址编码，面向线上 `ADDR_SEGM.segm_no` 契约。
     */
    private String segmNo;

    /**
     * 标准地址编码，面向线上 `ADDR_SEGM.stand_no` 契约。
     */
    private String standNo;

    /**
     * 地址类型编码，映射 `ADDR_SEGM.segm_type`。
     */
    private String segmType;

    /**
     * 数据库真实层级 ID，来源于 `segm_addr_type.level_id`。
     */
    private Integer levelId;

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
    private String installStationId;

    /**
     * 营业站 ID。
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
     * 管理站名称。
     */
    private String stationName;

    /**
     * 安装站名称。
     */
    private String installStationName;

    /**
     * 营业站名称。
     */
    private String busStationName;

    /**
     * 是否只读。
     */
    private Boolean readOnlyFlag;

    /**
     * 是否允许编辑。
     */
    private Boolean canEdit;

    /**
     * 是否允许删除。
     */
    private Boolean canDelete;

    /**
     * 是否允许合并。
     */
    private Boolean canMerge;

    /**
     * 是否允许拆分。
     */
    private Boolean canSplit;

    /**
     * 线上备注。
     */
    private String notes;

    /**
     * 线上创建时间。
     */
    private Date createDate;

    /**
     * 省份编码
     */
    @ExcelProperty(value = "省份编码")
    private String provinceCode;

    /**
     * 城市编码
     */
    @ExcelProperty(value = "城市编码")
    private String cityCode;

    /**
     * 区县编码
     */
    @ExcelProperty(value = "区县编码")
    private String districtCode;

    /**
     * 街道编码
     */
    @ExcelProperty(value = "街道编码")
    private String streetCode;

    /**
     * 社区/村编码
     */
    @ExcelProperty(value = "社区/村编码")
    private String villageCode;

    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 标签名称集合
     */
    private List<String> tagNames;

    public String getSegmId() {
        return segmId != null ? segmId : id;
    }

    public void setSegmId(String segmId) {
        this.segmId = segmId;
        this.id = segmId;
    }

    public String getParentSegmId() {
        return parentSegmId != null ? parentSegmId : parentId;
    }

    public void setParentSegmId(String parentSegmId) {
        this.parentSegmId = parentSegmId;
        this.parentId = parentSegmId;
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

    public void setId(String id) {
        this.id = id;
        this.segmId = id;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
        this.parentSegmId = parentId;
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

    public String getNotes() {
        return notes != null ? notes : remark;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.remark = notes;
    }

    public void setRemark(String remark) {
        this.remark = remark;
        this.notes = remark;
    }

    public Date getCreateDate() {
        return createDate != null ? createDate : createTime;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
        this.createTime = createDate;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
        this.createDate = createTime;
    }
}
