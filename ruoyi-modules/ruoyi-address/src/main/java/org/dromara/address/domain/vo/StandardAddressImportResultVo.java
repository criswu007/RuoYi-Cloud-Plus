package org.dromara.address.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 标准地址导入结果摘要视图。
 */
@Data
public class StandardAddressImportResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long batchId;

    private String batchNo;

    private String fileName;

    private String status;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private Boolean failureExportable;
}
