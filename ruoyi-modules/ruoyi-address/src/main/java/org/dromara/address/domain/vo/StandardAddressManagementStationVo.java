package org.dromara.address.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.address.domain.StandardAddressManagementStation;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 管理站管理视图对象。
 * 目的：输出管理站列表与详情，满足原型页面展示字段。
 */
@Data
@AutoMapper(target = StandardAddressManagementStation.class)
public class StandardAddressManagementStationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 管理站主键。
     */
    private Long id;

    /**
     * 管理站名称。
     */
    private String name;

    /**
     * 创建时间。
     */
    private Date createTime;
}
