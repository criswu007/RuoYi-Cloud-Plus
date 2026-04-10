package org.dromara.address.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.address.domain.StandardAddressMonitorRecord;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = StandardAddressMonitorRecord.class, reverseConvertGenerate = false)
public class StandardAddressMonitorRecordBo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 标准地址ID
     */
    private Long standardAddressId;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 来源任务ID
     */
    private Long taskId;

    /**
     * 严重等级
     */
    private String severity;

    /**
     * 标准地址名称快照
     */
    private String standNameSnapshot;

    /**
     * 规则名称快照
     */
    private String ruleNameSnapshot;

    /**
     * 状态（0待处理 1已忽略 2已处理）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
