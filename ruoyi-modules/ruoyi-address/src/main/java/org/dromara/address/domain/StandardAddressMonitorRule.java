package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 标准地址监控规则实体。
 * 目的：承载模板化规则配置持久化字段，支撑非标地址监控规则管理。
 * 入参/出参：作为规则 Mapper、Service 与 BO/VO 的核心持久化对象。
 * 关键约束：规则以模板为中心，编码需稳定，模板参数统一落在 `configJson`。
 * 异常与副作用：实体本身无异常与副作用，写入合法性由服务层校验。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("address_standard_monitor_rule")
public class StandardAddressMonitorRule extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
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
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

}
