package org.dromara.address.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@HeadRowHeight(24)
@ContentRowHeight(22)
public class StandardAddressImportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标准地址类型名称。
     */
    @ColumnWidth(18)
    @ExcelProperty(value = "分段地址类型")
    private String segmTypeName;

    /**
     * 是否城区文本值。
     */
    @ColumnWidth(12)
    @ExcelProperty(value = "是否城区")
    private String isCityLabel;

    /**
     * 父级标准地址名称。
     */
    @ColumnWidth(48)
    @ExcelProperty(value = "父级地址")
    private String parentStandName;

    /**
     * 当级名称。
     */
    @ColumnWidth(20)
    @ExcelProperty(value = "当级名称")
    private String segmName;

    /**
     * 所属维修管理站名称。
     */
    @ColumnWidth(22)
    @ExcelProperty(value = "所属维修管理站")
    private String maintenanceStationName;

    /**
     * 所属安装管理站名称。
     */
    @ColumnWidth(22)
    @ExcelProperty(value = "所属安装管理站")
    private String installStationName;

    /**
     * 所属营业管理站名称。
     */
    @ColumnWidth(22)
    @ExcelProperty(value = "所属营业管理站")
    private String businessStationName;

    /**
     * 接入方式文本值，导入口径与详情页“光纤接入方式”一致。
     */
    @ColumnWidth(16)
    @ExcelProperty(value = "接入方式")
    private String accessModeName;

    /**
     * 接入能力文本值。
     */
    @ColumnWidth(16)
    @ExcelProperty(value = "接入能力")
    private String accessCapabilityName;

    /**
     * 城乡属性文本值。
     */
    @ColumnWidth(16)
    @ExcelProperty(value = "城乡属性")
    private String areaTypeName;

    /**
     * 房屋属性文本值。
     */
    @ColumnWidth(16)
    @ExcelProperty(value = "房屋属性")
    private String placeTypeName;

    /**
     * 是否配套小区文本值。
     */
    @ColumnWidth(16)
    @ExcelProperty(value = "是否配套小区")
    private String supportingFeeCommunityLabel;

    /**
     * 覆盖户数字符串。
     */
    @ColumnWidth(12)
    @ExcelProperty(value = "覆盖户数")
    private String coverNumText;

    /**
     * 工程编号。
     */
    @ColumnWidth(20)
    @ExcelProperty(value = "工程编号")
    private String singleProjectCode;

}
