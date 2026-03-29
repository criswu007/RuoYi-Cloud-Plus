package org.dromara.address.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.address.domain.InstallationAddress;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = InstallationAddress.class)
public class InstallationAddressVo implements Serializable {

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
     * 是否关联标准地址
     */
    private Boolean hasStandardAddress;

    /**
     * 关联标准地址完整名称
     */
    private String standardAddressFullName;

    /**
     * 安装位置描述
     */
    private String installName;

    /**
     * 关联资源ID
     */
    private String resourceId;

    /**
     * 关联资源类型
     */
    private String resourceType;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;
}
