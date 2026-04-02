package org.dromara.address.domain.bo;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 标准地址拆分 BO
 * 目的：定义拆分请求的入参结构，避免 Map 传参。
 */
@Data
public class StandardAddressSplitBo {

    /**
     * 源标准地址 `segmId`
     */
    @NotBlank(message = "待拆分地址不能为空")
    private String sourceSegmId;

    /**
     * 拆分后的新地址最小配置项列表
     */
    @NotEmpty(message = "拆分地址项不能为空")
    @Valid
    private List<StandardAddressSplitItemBo> splitItems;
}
