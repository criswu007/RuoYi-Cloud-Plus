package org.dromara.address.mapper;

import org.dromara.address.domain.PubRestriction;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 字典与约束解释表 `pub_restriction` Mapper。
 */
public interface PubRestrictionMapper extends BaseMapperPlus<PubRestriction, PubRestriction> {

    /**
     * 目的：查询标准地址编辑页依赖的固定字典项。
     * 入参：无。
     * 出参：标准地址编辑页所需字典原始记录。
     * 关键约束：仅返回标准地址编辑弹窗使用的状态、接入方式、接入能力、城乡属性与房屋属性字典。
     * 异常与副作用：只读查询，无写入副作用。
     */
    List<PubRestriction> selectStandardAddressFormRestrictions();
}
