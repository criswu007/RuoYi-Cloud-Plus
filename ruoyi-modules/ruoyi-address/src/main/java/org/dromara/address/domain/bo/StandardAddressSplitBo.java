package org.dromara.address.domain.bo;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 标准地址拆分 BO
 * 目的：定义拆分请求的入参结构，避免 Map 传参。
 */
@Data
public class StandardAddressSplitBo {

    /**
     * 源标准地址ID
     */
    @NotNull(message = "源标准地址ID不能为空")
    private Long sourceStandardAddressId;

    /**
     * 拆分后的新地址列表
     */
    @NotEmpty(message = "新地址列表不能为空")
    @Valid
    private List<StandardAddressBo> newAddresses;
}
