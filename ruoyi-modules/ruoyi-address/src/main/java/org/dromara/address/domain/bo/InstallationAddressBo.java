package org.dromara.address.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import jakarta.validation.constraints.NotBlank;

@Data
@EqualsAndHashCode(callSuper = true)
public class InstallationAddressBo extends BaseEntity {

    /**
     * 安装地址主键，对应 `ADDR_SET_SEGM.set_addr_id`。
     */
    private String setAddrId;

    /**
     * 安装地址名称，对应 `ADDR_SET_SEGM.set_addr_name`。
     */
    @NotBlank(message = "安装地址名称不能为空")
    private String setAddrName;

    /**
     * 安装地址编号，对应 `ADDR_SET_SEGM.set_addr_no`。
     */
    private String setAddrNo;

    /**
     * 安装地址类型，对应 `ADDR_SET_SEGM.set_type`。
     */
    private String setType;

    /**
     * 关联标准地址主键，对应 `ADDR_SET_SEGM.segm_id`。
     */
    private String segmId;

    /**
     * 关联标准地址类型，对应 `ADDR_SET_SEGM.segm_type`。
     */
    private String segmType;

    /**
     * 区域 ID，对应 `ADDR_SET_SEGM.region_id`。
     */
    private String regionId;

    /**
     * 组织 ID，对应 `ADDR_SET_SEGM.org_id`。
     */
    private String orgId;

    /**
     * 设备 ID（兼容字段）。
     * <p>
     * 线上历史表 `ADDR_SET_SEGM` 暂无 `device_id` 字段，
     * 当前阶段仅保留接口参数兼容，不参与 SQL 过滤与持久化写入。
     * </p>
     */
    private String deviceId;

    /**
     * 关联状态，`BOUND/UNBOUND`。
     */
    private String associationStatus;

    /**
     * BOSS 操作人，对应 `ADDR_SET_SEGM.boss_op`。
     */
    private String bossOp;

    /**
     * 备注
     */
    private String notes;

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
     * 原始兼容字段：关联资源ID（旧命名）。
     */
    @Deprecated
    public String getResourceId() {
        return deviceId;
    }

    /**
     * 原始兼容字段：关联资源ID（旧命名）。
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

}
