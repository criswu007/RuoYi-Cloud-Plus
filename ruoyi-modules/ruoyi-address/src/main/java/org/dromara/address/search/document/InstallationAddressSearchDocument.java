package org.dromara.address.search.document;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 安装地址搜索文档。
 */
@Data
public class InstallationAddressSearchDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 安装地址主键。
     */
    private String setAddrId;

    /**
     * 关联标准地址主键。
     */
    private String segmId;

    /**
     * 安装位置描述。
     */
    private String setAddrName;

    /**
     * 关联标准地址完整名称。
     */
    private String standName;

    /**
     * 安装地址编号。
     */
    private String setAddrNo;

    /**
     * 安装地址类型。
     */
    private String setType;

    /**
     * 组织 ID。
     */
    private String orgId;

    /**
     * 关联状态。
     */
    private String associationStatus;

    /**
     * 关联标准地址类型。
     */
    private String segmType;

    /**
     * 区域 ID。
     */
    private String regionId;

    /**
     * 创建时间。
     */
    private Date createDate;
}
