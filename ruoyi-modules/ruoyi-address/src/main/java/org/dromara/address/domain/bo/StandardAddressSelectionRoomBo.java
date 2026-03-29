package org.dromara.address.domain.bo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class StandardAddressSelectionRoomBo {

    /**
     * 楼栋级标准地址ID
     * 约束：必须存在且层级不高于楼栋级
     */
    @NotNull(message = "楼栋标准地址ID不能为空")
    private Long parentId;

    /**
     * 房间号或房间名称
     * 约束：同一楼栋下唯一
     */
    @NotBlank(message = "房间号不能为空")
    private String roomName;

    /**
     * 安装位置描述（可选）
     * 为空时默认使用 roomName
     */
    private String installName;
}
