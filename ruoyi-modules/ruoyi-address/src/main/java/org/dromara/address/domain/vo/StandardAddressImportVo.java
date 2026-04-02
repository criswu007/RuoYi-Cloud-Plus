package org.dromara.address.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

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
     * 父级标准地址名称。
     */
    @ExcelProperty(value = "父级地址")
    private String parentStandName;

    /**
     * 当级名称。
     */
    @ExcelProperty(value = "当级名称")
    private String segmName;

    /**
     * 地址业务级别。
     */
    @ExcelProperty(value = "地址级别")
    private Integer addrLevel;

    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String notes;

}
