package org.dromara.address.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.address.domain.SpcStation;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 管理站主数据表 `spc_station` Mapper。
 */
public interface SpcStationMapper extends BaseMapperPlus<SpcStation, SpcStation> {

    /**
     * 目的：按区域、类型与关键字查询管理站候选。
     * 入参：区域 ID、管理站类型与搜索关键字。
     * 出参：管理站候选列表。
     * 关键约束：管理站下拉不提供分页语义，但需优先按 `region_id + manage_type` 收敛范围。
     * 异常与副作用：只读查询，无写入副作用。
     */
    List<SpcStation> selectStationOptions(@Param("regionId") String regionId,
                                          @Param("manageType") String manageType,
                                          @Param("keyword") String keyword);
}
