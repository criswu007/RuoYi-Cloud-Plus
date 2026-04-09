package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressApprovalBo;
import org.dromara.address.domain.bo.StandardAddressApprovalActionBo;
import org.dromara.address.domain.StandardAddressApproval;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.bo.StandardAddressSplitItemBo;
import org.dromara.address.domain.vo.StandardAddressApprovalVo;
import org.dromara.address.domain.vo.StandardAddressImportResultVo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * 标准地址审批申请服务接口。
 * 目的：统一标准地址各类写操作的审批提交流程与审批记录查询能力。
 * 入参/出参：输入标准地址写操作参数，输出审批提交结果、导入摘要或分页审批记录。
 * 关键约束：提交审批只生成申请单和流程，不直接修改正式地址表。
 * 异常与副作用：会写入审批申请单并发起 workflow。
 */
public interface IStandardAddressApprovalService {

    Boolean submitAddApproval(StandardAddressBo bo);

    Boolean submitUpdateApproval(StandardAddressBo bo);

    Boolean submitDeleteApproval(Collection<String> segmIds, boolean confirm);

    Boolean submitMergeApproval(List<String> sourceSegmIds, String targetSegmId);

    Boolean submitSplitApproval(String sourceSegmId, List<StandardAddressSplitItemBo> splitItems);

    StandardAddressImportResultVo submitImportApproval(List<StandardAddressImportVo> rows, Boolean updateSupport, String operName, String fileName);

    StandardAddressApproval submitImportRowApproval(StandardAddressImportVo row, Long batchId, Long itemId, Integer rowNum,
                                                    Boolean updateSupport, String operName, String fileName);

    TableDataInfo<StandardAddressApprovalVo> queryMyPage(StandardAddressApprovalBo bo, PageQuery pageQuery);

    TableDataInfo<StandardAddressApprovalVo> queryHandledPage(StandardAddressApprovalBo bo, PageQuery pageQuery);

    /**
     * 目的：按当前筛选条件导出当前登录人已审批记录。
     * 入参：审批筛选条件。
     * 出参：导出用审批记录列表。
     * 关键约束：仅返回当前登录人已实际处理完成的数据，字段口径与“已审批”列表保持一致。
     * 异常与副作用：无写入副作用。
     *
     * @param bo 审批筛选条件
     * @return 导出用审批记录列表
     */
    List<StandardAddressApprovalVo> listHandledForExport(StandardAddressApprovalBo bo);

    StandardAddressApprovalVo getDetail(Long id);

    StandardAddressApprovalVo getByBusinessId(String businessId);

    Boolean approve(StandardAddressApprovalActionBo bo);

    Boolean reject(StandardAddressApprovalActionBo bo);
}
