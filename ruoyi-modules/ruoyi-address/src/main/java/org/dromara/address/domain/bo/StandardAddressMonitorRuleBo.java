package org.dromara.address.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.address.domain.StandardAddressMonitorRule;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = StandardAddressMonitorRule.class, reverseConvertGenerate = false)
public class StandardAddressMonitorRuleBo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则类型（REGEX, DICT, CUSTOM）
     */
    private String ruleType;

    /**
     * 规则内容
     */
    private String ruleContent;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
