package org.dromara.address.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.address.domain.SpcRegion;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 区域主数据表 `spc_region` Mapper。
 */
public interface SpcRegionMapper extends BaseMapperPlus<org.dromara.address.domain.SpcRegion, org.dromara.address.domain.SpcRegion> {

    /**
     * 目的：查询 `1/2` 级标准地址列表投影。
     * 入参：标准地址查询条件。
     * 出参：统一标准地址视图列表。
     * 关键约束：只用于 `spc_region` 的只读投影；一二级地址统一按 `grade_id=2000002/2000004` 判定。
     * 异常与副作用：无写入副作用。
     */
    List<StandardAddressVo> selectStandardAddressList(@Param("bo") StandardAddressBo bo);

    /**
     * 目的：分页查询 `1/2` 级标准地址列表投影。
     * 入参：分页参数、标准地址查询条件。
     * 出参：标准地址分页结果。
     * 关键约束：分页必须在数据库侧完成，且排序只能基于 `spc_region` 真实列；一二级地址统一按 `grade_id=2000002/2000004` 判定。
     * 异常与副作用：无写入副作用。
     */
    Page<StandardAddressVo> selectStandardAddressPage(@Param("page") Page<StandardAddressVo> page,
                                                      @Param("bo") StandardAddressBo bo);

    /**
     * 目的：根据区域主键查询 `1/2` 级标准地址详情投影。
     * 入参：区域主键。
     * 出参：统一标准地址视图；未命中时返回 `null`。
     * 关键约束：仅投影 `spc_region` 与父级区域名称，不参与写链路；层级与类型投影需与列表查询保持同一规则。
     * 异常与副作用：无写入副作用。
     */
    StandardAddressVo selectStandardAddressByRegionId(@Param("regionId") String regionId);

    /**
     * 目的：按区域名称精确查询有效区域。
     * 入参：区域名称。
     * 出参：命中的区域主数据；未命中返回 `null`。
     * 关键约束：仅匹配未删除区域，用于导入解析一二级父级地址。
     * 异常与副作用：无写入副作用。
     */
    SpcRegion selectActiveByRegionName(@Param("regionName") String regionName);

    /**
     * 目的：按关键字分页查询区域级标准地址候选。
     * 入参：分页参数、关键字、业务级别。
     * 出参：受限的区域级地址候选分页结果。
     * 关键约束：候选限制必须依赖 MyBatis-Plus 分页插件，禁止在 XML 中写数据库方言 `limit`；层级筛选需与只读区域投影规则保持一致。
     * 异常与副作用：无写入副作用。
     */
    Page<StandardAddressVo> selectSearchCandidatePage(@Param("page") Page<StandardAddressVo> page,
                                                      @Param("keyword") String keyword,
                                                      @Param("addrLevel") Integer addrLevel);
}
