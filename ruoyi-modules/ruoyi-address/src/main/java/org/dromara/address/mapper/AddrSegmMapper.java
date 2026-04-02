package org.dromara.address.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.bo.StandardAddressBo;
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
    List<StandardAddressVo> selectStandardAddressList(@Param("bo") StandardAddressBo bo,
                                                      @Param("segmTypes") List<String> segmTypes);

    /**
     * 目的：按查询条件分页获取标准地址列表。
     * 入参：分页参数、标准地址查询条件与可选地址类型集合。
     * 出参：标准地址分页结果。
     * 关键约束：分页必须在数据库侧完成，严禁先查全集再内存切页；层级过滤作用于 `ADDR_SEGM.segm_type` 对应的 `segm_addr_type.level_id`。
     * 异常与副作用：无写入副作用。
     */
    Page<StandardAddressVo> selectStandardAddressPage(@Param("page") Page<StandardAddressVo> page,
                                                      @Param("bo") StandardAddressBo bo,
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
     * 目的：按完整标准地址名称精确查询有效地址。
     * 入参：标准地址全称。
     * 出参：命中的有效地址；未命中返回 `null`。
     * 关键约束：只匹配未删除记录，用于导入时父级地址解析。
     * 异常与副作用：无写入副作用。
     */
    AddrSegm selectActiveByStandName(@Param("standName") String standName);

    /**
     * 目的：按父级地址和当级名称精确查询有效地址。
     * 入参：父级 `segmId` 与当级名称。
     * 出参：命中的有效地址；未命中返回 `null`。
     * 关键约束：只匹配未删除记录，用于导入更新判定。
     * 异常与副作用：无写入副作用。
     */
    AddrSegm selectActiveByParentAndSegmName(@Param("parentSegmId") String parentSegmId,
                                             @Param("segmName") String segmName);

    /**
     * 目的：按关键字分页查询标准地址搜索候选。
     * 入参：分页参数、关键字、候选地址类型集合与状态过滤。
     * 出参：受限的标准地址候选分页结果。
     * 关键约束：候选限制必须依赖 MyBatis-Plus 分页插件，禁止在 XML 中写数据库方言 `limit`。
     * 异常与副作用：无写入副作用。
     */
    Page<StandardAddressVo> selectSearchCandidatePage(@Param("page") Page<StandardAddressVo> page,
                                                      @Param("keyword") String keyword,
                                                      @Param("segmTypes") List<String> segmTypes,
                                                      @Param("status") String status);

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
     * 目的：把多个源地址的直接子节点统一迁移到目标地址下。
     * 入参：源地址主键集合与目标地址主键。
     * 出参：受影响行数。
     * 关键约束：只处理未删除子节点，分页与循环由调用方控制，避免逐条更新。
     * 异常与副作用：会批量更新子节点 `parent_segm_id` 与修改时间。
     */
    int moveChildrenToTarget(@Param("sourceSegmIds") Collection<String> sourceSegmIds,
                             @Param("targetSegmId") String targetSegmId);

    /**
     * 目的：逻辑删除标准地址集合。
     * 入参：标准地址主键集合。
     * 出参：受影响行数。
     * 关键约束：仅更新未删除记录。
     * 异常与副作用：会把 `delete_state` 更新为已删除。
     */
    int logicalDeleteBySegmIds(@Param("segmIds") Collection<String> segmIds);
}
