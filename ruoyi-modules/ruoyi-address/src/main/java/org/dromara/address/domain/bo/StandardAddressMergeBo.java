package org.dromara.address.domain.bo;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 标准地址合并 BO
 * 目的：定义合并请求的入参结构，避免 Map 传参。
 */
@Data
public class StandardAddressMergeBo {

    /**
     * 待合并的源标准地址 `segmId` 集合
     */
    @NotEmpty(message = "待合并地址不能为空")
    private List<String> sourceSegmIds;

    /**
     * 合并目标地址 `segmId`
     */
    @jakarta.validation.constraints.NotBlank(message = "目标地址不能为空")
    private String targetSegmId;
}
