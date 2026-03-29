package org.dromara.address.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.address.domain.StandardAddressAttribute;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = StandardAddressAttribute.class, reverseConvertGenerate = false)
public class StandardAddressAttributeBo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 标准地址ID
     */
    @NotNull(message = "标准地址ID不能为空")
    private Long standardAddressId;

    /**
     * 属性键
     */
    @NotBlank(message = "属性键不能为空")
    private String attrKey;

    /**
     * 属性值
     */
    @NotBlank(message = "属性值不能为空")
    private String attrValue;

}
