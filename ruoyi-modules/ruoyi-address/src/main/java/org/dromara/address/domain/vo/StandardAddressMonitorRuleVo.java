package org.dromara.address.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.address.domain.StandardAddressMonitorRule;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = StandardAddressMonitorRule.class)
public class StandardAddressMonitorRuleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    /**
     * 创建时间
     */
    private Date createTime;
}
