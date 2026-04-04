package org.dromara.address.search.document;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 标准地址搜索文档。
 */
@Data
public class StandardAddressSearchDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标准地址主键。
     */
    private String segmId;

    /**
     * 父标准地址主键。
     */
    private String parentSegmId;

    /**
     * 当级名称。
     */
    private String segmName;

    /**
     * 标准地址全称。
     */
    private String standName;

    /**
     * 地址编码。
     */
    private String segmNo;

    /**
     * 标准地址编码。
     */
    private String standNo;

    /**
     * 区域 ID。
     */
    private String regionId;

    /**
     * 业务地址层级。
     */
    private Integer addrLevel;

    /**
     * 地址类型编码。
     */
    private String segmType;

    /**
     * 行政区 ID。
     */
    private String districtId;

    /**
     * 服务区域 ID。
     */
    private String serviceRegionId;

    /**
     * 状态（0正常 1停用）。
     */
    private String status;

    /**
     * 创建时间。
     */
    private Date createDate;
}
