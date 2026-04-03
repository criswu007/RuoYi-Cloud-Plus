package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableField;
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
     * 安装地址编号。
     */
    private String setAddrNo;

    /**
     * 安装地址类型。
     */
    private String setType;

    /**
     * 关联标准地址主键。
     */
    private String segmId;

    /**
     * 关联标准地址类型。
     */
    private String segmType;

    /**
     * 状态。
     */
    private String status;

    /**
     * 区域 ID。
     */
    private String regionId;

    /**
     * 组织 ID。
     */
    private String orgId;

    /**
     * 设备 ID（兼容字段）。
     * <p>
     * 历史库 `ADDR_SET_SEGM` 不存在 `device_id`，当前阶段仅保留入参/出参兼容，
     * 不参与持久化读写，避免线上库字段不一致导致 SQL 失败。
     * </p>
     */
    @TableField(exist = false)
    private String deviceId;

    /**
     * 备注。
     */
    private String notes;

    /**
     * BOSS 操作人。
     */
    private String bossOp;

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

    /**
     * 最近同步时间。
     */
    @TableField(value = "synchronous_date")
    private Date syncDate;
}
