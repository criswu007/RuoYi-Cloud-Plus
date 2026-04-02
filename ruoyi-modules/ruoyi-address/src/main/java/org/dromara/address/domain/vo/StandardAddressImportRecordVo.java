package org.dromara.address.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 标准地址导入失败明细视图。
 */
@Data
public class StandardAddressImportRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 明细ID。
     */
    private Long id;

    /**
     * 批次ID。
     */
    @ExcelProperty("批次ID")
    private Long batchId;

    /**
     * 批次号。
     */
    @ExcelProperty("批次号")
    private String batchNo;

    /**
     * 文件名。
     */
    @ExcelProperty("导入文件")
    private String fileName;

    /**
     * Excel 行号。
     */
    @ExcelProperty("行号")
    private Integer rowNum;

    /**
     * 父级地址名称。
     */
    @ExcelProperty("父级地址")
    private String parentStandName;

    /**
     * 当级名称。
     */
    @ExcelProperty("当级名称")
    private String segmName;

    /**
     * 业务级别。
     */
    @ExcelProperty("地址级别")
    private Integer addrLevel;

    /**
     * 地址类型编码。
     */
    @ExcelProperty("地址类型")
    private String segmType;

    /**
     * 状态。
     */
    @ExcelProperty("状态")
    private String status;

    /**
     * 失败原因。
     */
    @ExcelProperty("失败原因")
    private String failReason;

    /**
     * 创建人。
     */
    private Long createBy;

    /**
     * 创建时间。
     */
    private Date createTime;
}
