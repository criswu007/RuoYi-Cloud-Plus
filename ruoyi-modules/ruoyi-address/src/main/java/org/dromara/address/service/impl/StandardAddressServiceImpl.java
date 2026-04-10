package org.dromara.address.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.StandardAddressApproval;
import org.dromara.address.domain.StandardAddressImportBatch;
import org.dromara.address.domain.StandardAddressImportDetail;
import org.dromara.address.domain.bo.StandardAddressAdminBo;
import org.dromara.address.domain.bo.StandardAddressBatchAddBo;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.bo.StandardAddressSplitItemBo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.domain.vo.StandardAddressImportBatchVo;
import org.dromara.address.domain.vo.StandardAddressImportResultVo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.mapper.StandardAddressImportBatchMapper;
import org.dromara.address.mapper.StandardAddressImportDetailMapper;
import org.dromara.address.service.IStandardAddressApprovalService;
import org.dromara.address.service.IStandardAddressImportBatchService;
import org.dromara.address.service.IStandardAddressService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.excel.core.DropDownOptions;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 标准地址核心 facade。
 * 目的：把标准地址核心主链路统一收口到查询服务与命令服务，避免核心能力继续扩散历史实现。
 * 入参/出参：输入标准地址查询/命令 BO，输出统一 `VO`、分页结果或布尔型执行结果。
 * 关键约束：查询分流由 `StandardAddressQueryService` 负责；正式写链路由命令服务执行，前台写操作统一先走审批申请。
 * 异常与副作用：查询接口无写入副作用；写接口会创建审批申请单并发起 workflow，审批通过后才会写正式地址表。
 */
@Service
@DS("address")
@RequiredArgsConstructor
public class StandardAddressServiceImpl implements IStandardAddressService {

    private final StandardAddressQueryService queryService;
    private final StandardAddressCommandService commandService;
    private final StandardAddressDictionaryService dictionaryService;
    private final StandardAddressImportService importService;
    private final IStandardAddressApprovalService approvalService;
    private final StandardAddressImportBatchMapper importBatchMapper;
    private final StandardAddressImportDetailMapper importDetailMapper;
    private final IStandardAddressImportBatchService importBatchService;

    /**
     * 目的：按线上 `segmId` 查询标准地址详情。
     * 入参：标准地址字符串主键。
     * 出参：标准地址详情；未命中时返回 `null`。
     * 关键约束：查询分流由查询服务按层级规则自行处理。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public StandardAddressVo getStandardAddressBySegmId(String segmId) {
        return queryService.getBySegmId(segmId, null);
    }

    /**
     * 目的：分页查询标准地址。
     * 入参：查询条件与分页参数。
     * 出参：分页结果。
     * 关键约束：`1/2` 级走 `spc_region`，其他层级走 `ADDR_SEGM`。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public TableDataInfo<StandardAddressVo> queryStandardAddressPageList(StandardAddressBo bo, PageQuery pageQuery) {
        return queryService.queryPageList(bo, pageQuery);
    }

    /**
     * 目的：查询标准地址级别选项。
     * 入参：无。
     * 出参：标准地址级别选项列表。
     * 关键约束：结果必须与线上 `segm_addr_type` 保持一致，供前端下拉与展示统一复用。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public List<StandardAddressAdminVo.LevelOptionVo> listStandardAddressLevelOptions() {
        return dictionaryService.listLevelOptions();
    }

    /**
     * 目的：查询标准地址编辑页聚合字典。
     * 入参：无。
     * 出参：编辑页聚合字典。
     * 关键约束：状态、接入方式、接入能力、城乡属性和房屋属性都必须以线上 `pub_restriction` 为准。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public StandardAddressAdminVo.FormOptionsVo listStandardAddressFormOptions() {
        return dictionaryService.listFormOptions();
    }

    /**
     * 目的：查询标准地址编辑页管理站候选。
     * 入参：管理站类型、区域和搜索关键字。
     * 出参：管理站候选列表。
     * 关键约束：候选必须按 `manageType` 显式区分维修/安装/营业，并优先按 `regionId` 收敛结果。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public List<StandardAddressAdminVo.StationOptionVo> listStandardAddressStationOptions(StandardAddressAdminBo.StationOptionQueryBo bo) {
        return dictionaryService.listStationOptions(bo);
    }

    /**
     * 目的：查询标准地址导入模板下拉选项。
     * 入参：无。
     * 出参：Excel 模板下拉选项集合。
     * 关键约束：模板字段口径必须与当前导入解析规则保持一致。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public List<DropDownOptions> listStandardAddressImportTemplateOptions() {
        return dictionaryService.listImportTemplateOptions();
    }

    /**
     * 目的：查询标准地址列表。
     * 入参：查询条件。
     * 出参：标准地址列表。
     * 关键约束：查询字段与命名口径均以线上表结构为准。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public List<StandardAddressVo> queryStandardAddressList(StandardAddressBo bo) {
        return queryService.queryList(bo);
    }

    /**
     * 目的：批量查询标准地址名称映射。
     * 入参：标准地址字符串主键集合。
     * 出参：`segmId -> standName` 映射。
     * 关键约束：列表和聚合回填场景必须走批量查询，避免逐条详情查询放大数据库压力。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public Map<String, String> listStandardAddressStandNameMapBySegmIds(Collection<String> segmIds) {
        return queryService.listStandardAddressStandNameMapBySegmIds(segmIds);
    }

    /**
     * 目的：新增标准地址。
     * 入参：标准地址业务对象。
     * 出参：新增是否成功。
     * 关键约束：一二级地址只读，新增必须落到 `ADDR_SEGM`。
     * 异常与副作用：会创建审批申请单并发起 workflow，不直接写正式地址主表。
     */
    @Override
    public Boolean addStandardAddress(StandardAddressBo bo) {
        return approvalService.submitAddApproval(bo);
    }

    /**
     * 目的：修改标准地址。
     * 入参：标准地址业务对象。
     * 出参：修改是否成功。
     * 关键约束：名称或父级变化后需同步刷新子节点完整名称与简拼。
     * 异常与副作用：会创建审批申请单并发起 workflow，不直接更新正式地址主表。
     */
    @Override
    public Boolean updateStandardAddress(StandardAddressBo bo) {
        return approvalService.submitUpdateApproval(bo);
    }

    /**
     * 目的：删除标准地址。
     * 入参：标准地址主键集合与确认标记。
     * 出参：删除是否成功。
     * 关键约束：删除前必须先校验子节点和安装地址关联；仅做逻辑删除。
     * 异常与副作用：会创建审批申请单并发起 workflow，不直接更新 `ADDR_SEGM`。
     */
    @Override
    public Boolean deleteStandardAddresses(Collection<String> standardAddressIds, boolean confirm) {
        return approvalService.submitDeleteApproval(standardAddressIds, confirm);
    }

    /**
     * 目的：预览批量新增下级标准地址。
     * 入参：批量新增参数。
     * 出参：预览结果列表。
     * 关键约束：仅做规则预演，不落库。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public List<StandardAddressAdminVo.BatchPreviewVo> previewStandardAddressChildren(StandardAddressBatchAddBo bo) {
        return commandService.previewChildren(bo);
    }

    /**
     * 目的：批量新增下级标准地址。
     * 入参：批量新增参数。
     * 出参：执行是否成功。
     * 关键约束：父级必须存在，下级层级与地址类型必须能从线上字典解析。
     * 异常与副作用：会批量写入 `ADDR_SEGM`。
     */
    @Override
    public Boolean batchAddStandardAddressChildren(StandardAddressBatchAddBo bo) {
        return commandService.batchAddChildren(bo);
    }

    /**
     * 目的：提交标准地址合并审批申请。
     * 入参：源地址集合与目标地址。
     * 出参：提交是否成功。
     * 关键约束：审批通过前不修改正式地址。
     * 异常与副作用：会创建审批申请单并发起 workflow。
     */
    @Override
    public Boolean mergeStandardAddresses(List<String> sourceSegmIds, String targetSegmId) {
        return approvalService.submitMergeApproval(sourceSegmIds, targetSegmId);
    }

    /**
     * 目的：提交标准地址拆分审批申请。
     * 入参：源地址与新地址集合。
     * 出参：提交是否成功。
     * 关键约束：审批通过前不修改正式地址。
     * 异常与副作用：会创建审批申请单并发起 workflow。
     */
    @Override
    public Boolean splitStandardAddress(String sourceSegmId, List<StandardAddressSplitItemBo> splitItems) {
        return approvalService.submitSplitApproval(sourceSegmId, splitItems);
    }

    /**
     * 目的：提交标准地址导入审批申请。
     * 入参：导入数据、更新标识、操作人和文件名。
     * 出参：审批提交摘要。
     * 关键约束：上传阶段只做逐条前置校验与单条提审，不写正式地址表；每条 Excel 记录都必须落导入行结果。
     * 异常与副作用：会写入导入批次、导入行结果，并为校验通过的行发起单条审批。
     */
    @Override
    public StandardAddressImportResultVo importStandardAddressData(List<StandardAddressImportVo> list, Boolean updateSupport, String operName, String fileName) {
        List<StandardAddressImportVo> rows = list == null ? List.of() : list;
        boolean allowUpdate = Boolean.TRUE.equals(updateSupport);
        StandardAddressImportBatch batchRecord = importService.buildImportBatchRecord(rows.size(), allowUpdate, fileName);
        importBatchMapper.insert(batchRecord);

        int successCount = 0;
        int failCount = 0;
        String firstErrorMsg = null;
        for (int index = 0; index < rows.size(); index++) {
            int rowNum = index + 1;
            StandardAddressImportVo row = rows.get(index);
            try {
                importService.validateImportRow(row, allowUpdate);
                StandardAddressImportDetail detail = importService.buildImportRowDetail(batchRecord.getId(), rowNum, row,
                    fileName, allowUpdate, StandardAddressImportDetail.STATUS_WAITING_APPROVAL, null);
                importDetailMapper.insert(detail);
                try {
                    StandardAddressApproval approval = approvalService.submitImportRowApproval(row, batchRecord.getId(), detail.getId(),
                        rowNum, allowUpdate, operName, fileName);
                    detail.setApprovalId(approval.getId());
                    detail.setApprovalNo(approval.getApplyNo());
                    detail.setApprovalStatus(approval.getApprovalStatus());
                    importDetailMapper.updateById(detail);
                    successCount++;
                } catch (Exception ex) {
                    failCount++;
                    firstErrorMsg = StringUtils.defaultIfBlank(firstErrorMsg, ex.getMessage());
                    detail.setStatus(StandardAddressImportDetail.STATUS_EXECUTE_FAILED);
                    detail.setFailReason(StringUtils.defaultIfBlank(ex.getMessage(), "发起审批失败"));
                    detail.setApprovalStatus(StandardAddressApprovalService.APPROVAL_EXECUTE_FAILED);
                    importDetailMapper.updateById(detail);
                }
            } catch (Exception ex) {
                failCount++;
                firstErrorMsg = StringUtils.defaultIfBlank(firstErrorMsg, ex.getMessage());
                importDetailMapper.insert(importService.buildImportRowDetail(batchRecord.getId(), rowNum, row,
                    fileName, allowUpdate, StandardAddressImportDetail.STATUS_VALIDATE_FAILED,
                    StringUtils.defaultIfBlank(ex.getMessage(), "导入失败")));
            }
        }
        if (StringUtils.isNotBlank(firstErrorMsg)) {
            StandardAddressImportBatch update = new StandardAddressImportBatch();
            update.setId(batchRecord.getId());
            update.setErrorMsg(firstErrorMsg);
            importBatchMapper.updateById(update);
        }
        importBatchService.refreshBatchSummary(batchRecord.getId());
        StandardAddressImportBatchVo batchVo = importBatchService.queryBatchById(batchRecord.getId());

        StandardAddressImportResultVo result = new StandardAddressImportResultVo();
        result.setBatchId(batchRecord.getId());
        result.setBatchNo(batchRecord.getBatchNo());
        result.setFileName(batchRecord.getFileName());
        result.setStatus(batchVo == null ? StandardAddressImportBatch.STATUS_PENDING : batchVo.getStatus());
        result.setTotalCount(rows.size());
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setPendingCount(batchVo == null ? successCount : batchVo.getPendingCount());
        result.setFailureExportable(failCount > 0);
        return result;
    }
}
