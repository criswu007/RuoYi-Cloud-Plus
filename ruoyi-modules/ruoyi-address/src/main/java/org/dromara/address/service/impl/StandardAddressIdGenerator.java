package org.dromara.address.service.impl;

import cn.hutool.core.util.IdUtil;
import org.springframework.stereotype.Component;

/**
 * 标准地址 ID 生成器。
 * 目的：统一封装标准地址 `segmId` 的应用侧生成策略，方便后续替换为数据库 sequence 或其他发号方式。
 * 入参/出参：无入参，输出 24 位字符串主键。
 * 关键约束：当前实现必须稳定输出 24 位数字字符串。
 * 异常与副作用：纯计算组件，无数据库写入副作用。
 */
@Component
public class StandardAddressIdGenerator {

    /**
     * 目的：生成新的标准地址 `segmId`。
     * 入参：无。
     * 出参：24 位数字字符串。
     * 关键约束：不足 24 位时左侧补零。
     * 异常与副作用：无写入副作用。
     */
    public String nextSegmId() {
        return String.format("%024d", IdUtil.getSnowflakeNextId());
    }
}
