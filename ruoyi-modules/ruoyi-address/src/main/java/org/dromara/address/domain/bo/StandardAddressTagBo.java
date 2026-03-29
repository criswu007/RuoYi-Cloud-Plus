package org.dromara.address.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.address.domain.StandardAddressTag;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import jakarta.validation.constraints.NotBlank;

/**
 * 地址标签业务对象。
 * 目的：承载地址标签的查询、新增、修改参数。
 * 关键约束：标签名称不能为空；标签编码与颜色为可选扩展字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = StandardAddressTag.class, reverseConvertGenerate = false)
public class StandardAddressTagBo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 标签名称
     */
    @NotBlank(message = "标签名称不能为空")
    private String name;

    /**
     * 标签编码
     */
    private String code;

    /**
     * 标签颜色
     */
    private String color;

    /**
     * 备注
     */
    private String remark;
}
