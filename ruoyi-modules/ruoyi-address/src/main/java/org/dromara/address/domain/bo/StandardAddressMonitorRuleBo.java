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
     * 规则编码
     */
    private String ruleCode;

    /**
     * 规则模板
     */
    private String ruleTemplate;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 严重等级
     */
    private String severity;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 去重窗口小时数
     */
    private Integer dedupHours;

    /**
     * 模板配置JSON
     */
    private String configJson;

    /**
     * 备注
     */
    private String remark;
}
