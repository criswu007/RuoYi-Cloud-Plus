package org.dromara.address.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.address.domain.StandardAddressManagementStation;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 管理站管理业务对象。
 * 目的：承载管理站列表查询与新增/修改参数。
 * 关键约束：管理站名称不能为空，列表查询按名称模糊匹配。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = StandardAddressManagementStation.class, reverseConvertGenerate = false)
public class StandardAddressManagementStationBo extends BaseEntity {

    /**
     * 管理站主键。
     */
    private Long id;

    /**
     * 管理站名称。
     */
    @NotBlank(message = "管理站名称不能为空")
    private String name;
}
