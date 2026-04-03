package org.dromara.address.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.address.domain.StandardAddressOperationLog;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = StandardAddressOperationLog.class)
public class StandardAddressOperationLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关联标准地址ID
     */
    private String standardAddressId;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作对象。
     */
    private String operationObject;

    /**
     * 操作结果。
     */
    private String operationResult;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作时间
     */
    private Date operateTime;

    /**
     * 操作详情
     */
    private String details;
}
