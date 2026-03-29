package org.dromara.address.domain.bo;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.common.core.utils.StringUtils;

import java.io.Serializable;

/**
 * 标准地址批量新增 BO
 *
 * @author Lion Li
 */
@Data
public class StandardAddressBatchAddBo implements Serializable {

    /**
     * 父标准地址ID，沿用旧模块数值主键以兼容未迁移逻辑。
     */
    private Long parentId;

    /**
     * 父标准地址字符串主键，面向 `ADDR_SEGM.parent_segm_id` 契约。
     */
    private String parentSegmId;

    /**
     * 地址前缀。
     */
    @NotBlank(message = "地址前缀不能为空")
    private String prefix;

    /**
     * 起始编号。
     */
    @NotNull(message = "起始编号不能为空")
    private Integer startNum;

    /**
     * 结束编号。
     */
    @NotNull(message = "结束编号不能为空")
    private Integer endNum;

    /**
     * 地址后缀。
     */
    private String suffix;

    public String getParentSegmId() {
        if (parentSegmId != null) {
            return parentSegmId;
        }
        return parentId == null ? null : String.valueOf(parentId);
    }

    public void setParentSegmId(String parentSegmId) {
        this.parentSegmId = parentSegmId;
    }

    /**
     * 目的：统一校验父级地址标识是否已提供。
     * 入参：无。
     * 出参：`true` 表示至少提供了 `parentSegmId` 或 legacy `parentId` 之一。
     * 关键约束：标准地址核心链路优先使用 `parentSegmId`，但保留对未迁移调用方的兼容。
     * 异常与副作用：返回 `false` 时触发 Bean Validation 异常，无写入副作用。
     */
    @AssertTrue(message = "父标准地址ID不能为空")
    public boolean isParentSpecified() {
        return StringUtils.isNotBlank(parentSegmId) || parentId != null;
    }
}
