package org.dromara.address.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.address.domain.StandardAddressMonitorRecord;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = StandardAddressMonitorRecord.class)
public class StandardAddressMonitorRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 标准地址名称快照
     */
    private String standNameSnapshot;

    /**
     * 区域快照
     */
    private String regionIdSnapshot;

    /**
     * 规则名称快照
     */
    private String ruleNameSnapshot;

    /**
     * 规则模板快照
     */
    private String ruleTemplateSnapshot;

    /**
     * 来源任务ID
     */
    private Long taskId;

    /**
     * 来源任务运行日志ID
     */
    private Long taskRunLogId;

    /**
     * 来源任务名称快照
     */
    private String taskNameSnapshot;

    /**
     * 严重等级
     */
    private String severity;

    /**
     * 命中详情JSON
     */
    private String hitDetailJson;

    /**
     * 标准地址完整名称
     */
    private String standardAddressFullName;

    /**
     * 监控规则名称
     */
    private String ruleName;

    /**
     * 工单ID
     */
    private Long workOrderId;

    /**
     * 工单号
     */
    private String workOrderNo;

    /**
     * 工单状态
     */
    private String workOrderStatus;

    /**
     * 状态（0待处理 1已忽略 2已生成工单 3已修正 4已驳回）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 首次发现时间
     */
    private Date firstDetectedTime;

    /**
     * 最近发现时间
     */
    private Date lastDetectedTime;

    /**
     * 命中次数
     */
    private Integer hitCount;

    /**
     * 创建时间
     */
    private Date createTime;
}
