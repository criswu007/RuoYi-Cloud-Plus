package org.dromara.address.domain.vo;

import lombok.Data;

@Data
public class StandardAddressSelectionResultVo {

    /**
     * 新创建的标准地址ID
     */
    private Long standardAddressId;

    /**
     * 新创建的安装地址ID
     */
    private Long installationAddressId;
}
