package org.dromara.address.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.Collection;
import java.util.List;

/**
 * 线上标准地址主事实表 `ADDR_SEGM` Mapper。
 */
public interface AddrSegmMapper extends BaseMapperPlus<AddrSegm, StandardAddressVo> {

    /**
     * 目的：按查询条件获取标准地址列表。
     * 入参：标准地址查询条件与可选地址类型集合。
     * 出参：统一标准地址视图列表。
     * 关键约束：层级过滤作用于 `ADDR_SEGM.segm_type`。
     * 异常与副作用：无写入副作用。
     */
    List<StandardAddressVo> selectStandardAddressList(@Param("bo") org.dromara.address.domain.bo.StandardAddressBo bo,
                                                      @Param("segmTypes") List<String> segmTypes);

    /**
     * 目的：根据 `segmId` 获取标准地址详情。
     * 入参：标准地址主键。
     * 出参：统一标准地址视图；未命中时返回 `null`。
     * 关键约束：仅查询 `ADDR_SEGM` 主事实表。
     * 异常与副作用：无写入副作用。
     */
    StandardAddressVo selectStandardAddressBySegmId(@Param("segmId") String segmId);

    /**
     * 目的：统计指定父节点集合下的未删除子地址数量。
     * 入参：父级标准地址主键集合。
     * 出参：子地址数量。
     * 关键约束：仅统计未删除记录。
     * 异常与副作用：无写入副作用。
     */
    Long countChildren(@Param("segmIds") Collection<String> segmIds);

    /**
     * 目的：查询指定父级下的未删除直接子节点。
     * 入参：父级标准地址主键。
     * 出参：子节点列表。
     * 关键约束：只返回未删除记录。
     * 异常与副作用：无写入副作用。
     */
    List<AddrSegm> selectChildrenByParentSegmId(@Param("parentSegmId") String parentSegmId);

    /**
     * 目的：逻辑删除标准地址集合。
     * 入参：标准地址主键集合。
     * 出参：受影响行数。
     * 关键约束：仅更新未删除记录。
     * 异常与副作用：会把 `delete_state` 更新为已删除。
     */
    int logicalDeleteBySegmIds(@Param("segmIds") Collection<String> segmIds);
}
