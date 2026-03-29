package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 线上安装地址主事实表 `ADDR_SET_SEGM` 实体。
 */
@Data
@TableName("ADDR_SET_SEGM")
public class AddrSetSegm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 安装地址主键。
     */
    @TableId(value = "set_addr_id")
    private String setAddrId;

    /**
     * 安装地址名称。
     */
    private String setAddrName;

    /**
     * 安装地址类型。
     */
    private String setType;

    /**
     * 关联标准地址主键。
     */
    private String segmId;

    /**
     * 状态。
     */
    private String status;

    /**
     * 区域 ID。
     */
    private String regionId;

    /**
     * 备注。
     */
    private String notes;

    /**
     * 删除状态。
     */
    private String deleteState;

    /**
     * 删除时间。
     */
    private Date deleteTime;

    /**
     * 创建时间。
     */
    private Date createDate;
}
