package org.dromara.address.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.address.domain.AddrSetSegm;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.Collection;

/**
 * 线上安装地址主事实表 `ADDR_SET_SEGM` Mapper。
 */
public interface AddrSetSegmMapper extends BaseMapperPlus<AddrSetSegm, AddrSetSegm> {

    /**
     * 目的：统计标准地址集合关联的安装地址数量。
     * 入参：标准地址主键集合。
     * 出参：关联安装地址数量。
     * 关键约束：只统计未删除的安装地址记录。
     * 异常与副作用：无写入副作用。
     */
    Long countBySegmIds(@Param("segmIds") Collection<String> segmIds);

    /**
     * 目的：把多个源标准地址关联的安装地址统一改绑到目标标准地址。
     * 入参：源标准地址集合与目标标准地址。
     * 出参：受影响行数。
     * 关键约束：只处理未删除安装地址关系，避免逐条更新放大写入成本。
     * 异常与副作用：会批量更新 `ADDR_SET_SEGM.segm_id`。
     */
    int rebindSegmIds(@Param("sourceSegmIds") Collection<String> sourceSegmIds,
                      @Param("targetSegmId") String targetSegmId);
}
