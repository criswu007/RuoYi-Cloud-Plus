package org.dromara.address.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.address.domain.AddrSetSegm;
import org.dromara.address.domain.bo.InstallationAddressBo;
import org.dromara.address.domain.vo.InstallationAddressVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.Collection;
import java.util.List;

/**
 * 线上安装地址主事实表 `ADDR_SET_SEGM` Mapper。
 */
public interface AddrSetSegmMapper extends BaseMapperPlus<AddrSetSegm, AddrSetSegm> {

    /**
     * 目的：分页查询安装地址列表。
     * 入参：分页参数与安装地址查询条件。
     * 出参：安装地址分页结果。
     * 关键约束：主查询只访问 `ADDR_SET_SEGM` 主事实表，不直接关联标准地址大表。
     * 异常与副作用：无写入副作用。
     */
    Page<InstallationAddressVo> selectInstallationPage(@Param("page") Page<InstallationAddressVo> page,
                                                       @Param("bo") InstallationAddressBo bo);

    /**
     * 目的：查询安装地址列表（不分页）。
     * 入参：安装地址查询条件。
     * 出参：安装地址列表。
     * 关键约束：仅查询当前交互所需字段，禁止额外关联大表。
     * 异常与副作用：无写入副作用。
     */
    List<InstallationAddressVo> selectInstallationList(@Param("bo") InstallationAddressBo bo);

    /**
     * 目的：按安装地址主键查询详情。
     * 入参：安装地址主键。
     * 出参：安装地址详情。
     * 关键约束：详情查询按唯一主键命中，不拼接数据库方言 `limit`。
     * 异常与副作用：无写入副作用。
     */
    InstallationAddressVo selectInstallationBySetAddrId(@Param("setAddrId") String setAddrId);

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
