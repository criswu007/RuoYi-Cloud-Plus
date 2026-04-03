package org.dromara.address.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.address.domain.StandardAddressImportFailDetail;
import org.dromara.address.domain.bo.StandardAddressImportRecordBo;
import org.dromara.address.domain.vo.StandardAddressImportRecordVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 标准地址导入失败明细 Mapper。
 */
public interface StandardAddressImportFailDetailMapper extends BaseMapperPlus<StandardAddressImportFailDetail, StandardAddressImportRecordVo> {

    /**
     * 目的：分页查询导入失败明细。
     * 入参：分页参数与查询条件。
     * 出参：失败明细分页结果。
     * 关键约束：分页必须由数据库执行，并联带批次信息。
     * 异常与副作用：无写入副作用。
     */
    Page<StandardAddressImportRecordVo> selectFailDetailPage(@Param("page") Page<StandardAddressImportRecordVo> page,
                                                             @Param("bo") StandardAddressImportRecordBo bo);

    /**
     * 目的：按批次查询全部失败明细。
     * 入参：批次ID。
     * 出参：失败明细列表。
     * 关键约束：仅返回未删除失败明细，供失败导出使用。
     * 异常与副作用：无写入副作用。
     */
    List<StandardAddressImportRecordVo> selectFailDetailListByBatchId(@Param("batchId") Long batchId);
}
