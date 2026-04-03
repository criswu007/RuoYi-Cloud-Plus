package org.dromara.address.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 标准地址拆分项 BO。
 * 目的：描述拆分后生成的一条同级新地址最小入参，只允许前端提交拆分后的当级名称。
 * 入参/出参：输入拆分后当级名称，输出给拆分服务做同级地址生成。
 * 关键约束：不承载父级、级别、管理站、接入方式等继承字段，这些字段统一由服务层从源地址复制。
 * 异常与副作用：参数校验失败时由控制层抛出校验异常，无写入副作用。
 */
@Data
public class StandardAddressSplitItemBo {

    /**
     * 拆分后当级名称。
     */
    @NotBlank(message = "拆分后当级名称不能为空")
    private String segmName;
}
