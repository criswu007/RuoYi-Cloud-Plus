package org.dromara.address.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.address.domain.InstallationAddress;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = InstallationAddress.class, reverseConvertGenerate = false)
public class InstallationAddressBo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 标准地址ID
     */
    private Long standardAddressId;

    /**
     * 安装位置描述
     */
    @NotBlank(message = "安装位置描述不能为空")
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
}
