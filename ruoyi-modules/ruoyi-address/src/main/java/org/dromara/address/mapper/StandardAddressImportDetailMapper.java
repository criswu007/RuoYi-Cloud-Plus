package org.dromara.address.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.address.domain.StandardAddressImportDetail;
import org.dromara.address.domain.bo.StandardAddressImportDetailBo;
import org.dromara.address.domain.vo.StandardAddressImportDetailVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;
import java.util.Map;

/**
 * 标准地址导入失败明细 Mapper。
 */
public interface StandardAddressImportDetailMapper extends BaseMapperPlus<StandardAddressImportDetail, StandardAddressImportDetailVo> {

    /**
     * 目的：分页查询导入失败明细。
     * 入参：分页参数与查询条件。
     * 出参：失败明细分页结果。
     * 关键约束：分页必须由数据库执行，并联带批次信息。
     * 异常与副作用：无写入副作用。
     */
    Page<StandardAddressImportDetailVo> selectFailDetailPage(@Param("page") Page<StandardAddressImportDetailVo> page,
                                                             @Param("bo") StandardAddressImportDetailBo bo);

    /**
     * 目的：按批次查询全部失败明细。
     * 入参：批次ID。
     * 出参：失败明细列表。
     * 关键约束：仅返回未删除失败明细，供失败导出使用。
     * 异常与副作用：无写入副作用。
     */
    List<StandardAddressImportDetailVo> selectFailDetailListByBatchId(@Param("batchId") Long batchId);

    /**
     * 目的：按批次聚合导入行状态数量。
     * 入参：批次ID。
     * 出参：状态与数量映射列表。
     * 关键约束：仅统计未删除导入行结果。
     * 异常与副作用：无写入副作用。
     */
    List<Map<String, Object>> selectStatusSummaryByBatchId(@Param("batchId") Long batchId);
}
