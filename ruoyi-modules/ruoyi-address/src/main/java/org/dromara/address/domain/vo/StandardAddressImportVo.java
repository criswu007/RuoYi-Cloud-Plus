package org.dromara.address.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;

import java.io.Serial;
import java.io.Serializable;

/**
 * 标准地址导入VO
 *
 * @author Lion Li
 */
@Data
@NoArgsConstructor
public class StandardAddressImportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 地址名称
     */
    @ExcelProperty(value = "地址名称")
    private String name;

    /**
     * 地址编码
     */
    @ExcelProperty(value = "地址编码")
    private String code;

    /**
     * 地址层级
     */
    @ExcelProperty(value = "地址层级")
    private Integer level;

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
     * 父级地址名称 (用于导入时查找父ID)
     */
    @ExcelProperty(value = "父级地址名称")
    private String parentName;

}
