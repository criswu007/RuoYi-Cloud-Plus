package org.dromara.address.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class InstallationAddressVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private String setAddrId;

    /**
     * 标准地址ID
     */
    private String segmId;

    /**
     * 标准地址类型。
     */
    private String segmType;

    /**
     * 区域 ID。
     */
    private String regionId;

    /**
     * 是否关联标准地址
     */
    private Boolean hasStandardAddress;

    /**
     * 关联标准地址完整名称
     */
    private String standName;

    /**
     * 安装位置描述
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
     * 设备ID（兼容字段，当前阶段不落库）。
     */
    private String deviceId;

    /**
     * 组织ID。
     */
    private String orgId;

    /**
     * BOSS 操作人。
     */
    private String bossOp;

    /**
     * 关联状态。
     */
    private String associationStatus;

    /**
     * 备注。
     */
    private String notes;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 原始兼容字段：主键（旧命名）。
     */
    @Deprecated
    public String getId() {
        return setAddrId;
    }

    /**
     * 原始兼容字段：主键（旧命名）。
     *
     * @param id 安装地址主键
     */
    @Deprecated
    public void setId(String id) {
        this.setAddrId = id;
    }

    /**
     * 原始兼容字段：标准地址ID（旧命名）。
     */
    @Deprecated
    public String getStandardAddressId() {
        return segmId;
    }

    /**
     * 原始兼容字段：标准地址ID（旧命名）。
     *
     * @param standardAddressId 标准地址ID
     */
    @Deprecated
    public void setStandardAddressId(String standardAddressId) {
        this.segmId = standardAddressId;
    }

    /**
     * 原始兼容字段：关联标准地址名称（旧命名）。
     */
    @Deprecated
    public String getStandardAddressFullName() {
        return standName;
    }

    /**
     * 原始兼容字段：关联标准地址名称（旧命名）。
     *
     * @param standardAddressFullName 标准地址名称
     */
    @Deprecated
    public void setStandardAddressFullName(String standardAddressFullName) {
        this.standName = standardAddressFullName;
    }

    /**
     * 原始兼容字段：安装地址名称（旧命名）。
     */
    @Deprecated
    public String getInstallName() {
        return setAddrName;
    }

    /**
     * 原始兼容字段：安装地址名称（旧命名）。
     *
     * @param installName 安装地址名称
     */
    @Deprecated
    public void setInstallName(String installName) {
        this.setAddrName = installName;
    }

    /**
     * 原始兼容字段：资源ID（旧命名）。
     */
    @Deprecated
    public String getResourceId() {
        return deviceId;
    }

    /**
     * 原始兼容字段：资源ID（旧命名）。
     *
     * @param resourceId 设备ID
     */
    @Deprecated
    public void setResourceId(String resourceId) {
        this.deviceId = resourceId;
    }

    /**
     * 原始兼容字段：备注（旧命名）。
     */
    @Deprecated
    public String getRemark() {
        return notes;
    }

    /**
     * 原始兼容字段：备注（旧命名）。
     *
     * @param remark 备注
     */
    @Deprecated
    public void setRemark(String remark) {
        this.notes = remark;
    }

    /**
     * 原始兼容字段：创建时间（旧命名）。
     */
    @Deprecated
    public Date getCreateTime() {
        return createDate;
    }

    /**
     * 原始兼容字段：创建时间（旧命名）。
     *
     * @param createTime 创建时间
     */
    @Deprecated
    public void setCreateTime(Date createTime) {
        this.createDate = createTime;
    }
}
