package org.dromara.address.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.address.domain.SpcRegion;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 区域主数据表 `spc_region` Mapper。
 */
public interface SpcRegionMapper extends BaseMapperPlus<SpcRegion, SpcRegion> {

    /**
     * 目的：查询 `1/2` 级标准地址列表投影。
     * 入参：标准地址查询条件。
     * 出参：统一标准地址视图列表。
     * 关键约束：只用于 `spc_region` 的只读投影。
     * 异常与副作用：无写入副作用。
     */
    List<StandardAddressVo> selectStandardAddressList(@Param("bo") StandardAddressBo bo);
}
