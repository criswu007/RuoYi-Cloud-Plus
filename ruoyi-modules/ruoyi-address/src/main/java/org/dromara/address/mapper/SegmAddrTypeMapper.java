package org.dromara.address.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.address.domain.SegmAddrType;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 地址类型与层级解释表 `segm_addr_type` Mapper。
 */
public interface SegmAddrTypeMapper extends BaseMapperPlus<SegmAddrType, SegmAddrType> {

    /**
     * 目的：根据地址类型编码解析层级 ID。
     * 入参：地址类型编码。
     * 出参：层级 ID，不存在时返回 `null`。
     * 关键约束：以线上 `addr_type_id` 为唯一匹配键。
     * 异常与副作用：无写入副作用。
     */
    Integer selectLevelIdByAddrTypeId(@Param("addrTypeId") String addrTypeId);

    /**
     * 目的：根据层级 ID 查询可用地址类型编码集合。
     * 入参：层级 ID。
     * 出参：地址类型编码集合。
     * 关键约束：结果直接来源于线上 `segm_addr_type.level_id`。
     * 异常与副作用：无写入副作用。
     */
    List<String> selectAddrTypeIdsByLevelId(@Param("levelId") Integer levelId);

    /**
     * 目的：查询大于当前层级的下一个层级 ID。
     * 入参：当前层级 ID。
     * 出参：下一个层级 ID；不存在时返回 `null`。
     * 关键约束：按 `level_id` 升序取最小更大值。
     * 异常与副作用：无写入副作用。
     */
    Integer selectNextLevelId(@Param("currentLevelId") Integer currentLevelId);

    /**
     * 目的：查询指定层级下默认使用的首个地址类型编码。
     * 入参：层级 ID。
     * 出参：地址类型编码；不存在时返回 `null`。
     * 关键约束：按 `addr_type_id` 升序取第一条。
     * 异常与副作用：无写入副作用。
     */
    String selectFirstAddrTypeIdByLevelId(@Param("levelId") Integer levelId);
}
