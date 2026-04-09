package org.dromara.address.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 标准地址导入批次汇总视图。
 */
@Data
public class StandardAddressImportBatchVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long batchId;

    private String batchNo;

    private String fileName;

    private String status;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private Integer pendingCount;

    private Boolean updateSupport;

    private String errorMsg;

    private Long createBy;

    private Date createTime;
}
