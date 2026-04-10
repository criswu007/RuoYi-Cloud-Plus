package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressImportDetailBo;
import org.dromara.address.domain.vo.StandardAddressImportBatchVo;
import org.dromara.address.domain.vo.StandardAddressImportDetailVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

public interface IStandardAddressImportBatchService {

    /**
     * 根据批次ID查询导入批次摘要。
     *
     * @param batchId 批次ID
     * @return 导入批次详情
     */
    StandardAddressImportBatchVo queryBatchById(Long batchId);

    /**
     * 分页查询导入记录。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    TableDataInfo<StandardAddressImportDetailVo> queryPageList(StandardAddressImportDetailBo bo, PageQuery pageQuery);

    /**
     * 查询指定批次失败明细。
     *
     * @param batchId 批次ID
     * @return 失败明细列表
     */
    List<StandardAddressImportDetailVo> listFailDetailsByBatchId(Long batchId);

    /**
     * 刷新导入批次聚合统计。
     *
     * @param batchId 批次ID
     */
    void refreshBatchSummary(Long batchId);
}
