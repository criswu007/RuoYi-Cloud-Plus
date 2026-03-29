package org.dromara.address.api.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 远程地址VO
 *
 * @author Lion Li
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoteStandardAddressVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 地址名称
     */
    private String name;

    /**
     * 完整地址名称
     */
    private String fullName;

    /**
     * 地址层级
     */
    private Integer level;

    /**
     * 地址编码
     */
    private String code;

    /**
     * 经度
     */
    private BigDecimal lng;

    /**
     * 纬度
     */
    private BigDecimal lat;

}
