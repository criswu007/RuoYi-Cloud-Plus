package org.dromara.address.domain.bo;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 标准地址合并 BO
 * 目的：定义合并请求的入参结构，避免 Map 传参。
 */
@Data
public class StandardAddressMergeBo {

    /**
     * 待合并的源标准地址ID集合
     */
    @NotEmpty(message = "源标准地址ID集合不能为空")
    private List<Long> sourceStandardAddressIds;

    /**
     * 目标标准地址ID
     */
    @NotNull(message = "目标标准地址ID不能为空")
    private Long targetStandardAddressId;
}
