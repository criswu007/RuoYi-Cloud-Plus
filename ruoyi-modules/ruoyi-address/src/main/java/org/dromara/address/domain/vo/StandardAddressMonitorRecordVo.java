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
     * 标准地址完整名称
     */
    private String standardAddressFullName;

    /**
     * 监控规则名称
     */
    private String ruleName;

    /**
     * 状态（0待处理 1已忽略 2已处理）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;
}
