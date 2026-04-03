package org.dromara.address.domain.bo;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 地址标签绑定业务对象。
 * 目的：承载标准地址与标签的批量绑定/解绑参数。
 * 关键约束：标准地址ID集合与标签ID集合都不能为空。
 */
@Data
public class StandardAddressTagBindBo {

    /**
     * 标准地址ID集合
     */
    @NotEmpty(message = "标准地址不能为空")
    private List<String> standardAddressIds;

    /**
     * 标签ID集合
     */
    @NotEmpty(message = "标签不能为空")
    private List<Long> tagIds;
}
