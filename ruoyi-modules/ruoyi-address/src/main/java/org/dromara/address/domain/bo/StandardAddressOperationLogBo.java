package org.dromara.address.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.address.domain.StandardAddressOperationLog;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = StandardAddressOperationLog.class, reverseConvertGenerate = false)
public class StandardAddressOperationLogBo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关联标准地址ID
     */
    private Long standardAddressId;

    /**
     * 操作类型（MERGE, SPLIT, DELETE, IMPORT, UPDATE, INSERT）
     */
    private String operationType;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作详情
     */
    private String details;
}
