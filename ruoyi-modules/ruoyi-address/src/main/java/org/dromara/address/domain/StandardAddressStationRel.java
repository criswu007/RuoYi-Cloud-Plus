package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 标准地址站点关联表对象 address_standard_station_rel
 *
 * @author Lion Li
 */
@Data
@TableName("address_standard_station_rel")
public class StandardAddressStationRel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标准地址ID
     */
    private Long standardAddressId;

    /**
     * 站点ID
     */
    private Long stationId;

}
