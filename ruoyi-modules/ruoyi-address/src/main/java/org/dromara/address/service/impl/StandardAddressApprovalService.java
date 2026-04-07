package org.dromara.address.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.SpcRegion;
import org.dromara.address.domain.StandardAddressApproval;
import org.dromara.address.domain.bo.StandardAddressApprovalActionBo;
import org.dromara.address.domain.bo.StandardAddressApprovalBo;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.bo.StandardAddressSplitItemBo;
import org.dromara.address.domain.vo.StandardAddressApprovalVo;
import org.dromara.address.domain.vo.StandardAddressImportResultVo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.AddrSetSegmMapper;
import org.dromara.address.mapper.SpcRegionMapper;
import org.dromara.address.mapper.StandardAddressApprovalMapper;
import org.dromara.address.service.IStandardAddressApprovalService;
import org.dromara.address.workflow.AddressWorkflowHttpClient;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.api.RemoteWorkflowService;
import org.dromara.workflow.api.domain.RemoteFlowInstanceBizExt;
import org.dromara.workflow.api.domain.RemoteStartProcess;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 标准地址审批申请服务。
 * 目的：承接标准地址各类写操作的审批提交流程，统一生成审批申请单并发起 workflow。
 * 入参/出参：输入标准地址业务参数，输出审批提交结果、导入审批摘要或分页审批记录。
 * 关键约束：审批阶段只保存业务快照，不允许直接改写正式地址主表；`id` 必须和 workflow `businessId` 对齐。
 * 异常与副作用：会写入 `address_standard_approval` 并调用 workflow 发起审批流程。
 */
@Service
@RequiredArgsConstructor
public class StandardAddressApprovalService implements IStandardAddressApprovalService {

    public static final String FLOW_CODE = "address_standard_approve_v1";
    public static final String OPERATION_ADD = "ADD";
    public static final String OPERATION_IMPORT = "IMPORT";
    public static final String OPERATION_UPDATE = "UPDATE";
    public static final String OPERATION_DELETE = "DELETE";
    public static final String OPERATION_MERGE = "MERGE";
    public static final String OPERATION_SPLIT = "SPLIT";

    public static final String APPROVAL_WAITING = "WAITING";
    public static final String APPROVAL_APPROVED = "APPROVED";
    public static final String APPROVAL_REJECTED = "REJECTED";
    public static final String APPROVAL_EXECUTING = "EXECUTING";
    public static final String APPROVAL_EXECUTE_FAILED = "EXECUTE_FAILED";
    public static final String APPROVAL_ALREADY_PROCESSED_MESSAGE = "该审批已处理，请刷新列表后重试";
    public static final String DUPLICATE_SUBMIT_MESSAGE = "相同内容已提交审批，请勿重复提交";
    public static final String ACTIVE_SUBMIT_GUARD_KEY = "ACTIVE";

    private final StandardAddressApprovalMapper approvalMapper;
    private final AddrSegmMapper addrSegmMapper;
    private final AddrSetSegmMapper addrSetSegmMapper;
    private final SpcRegionMapper spcRegionMapper;
    private final RemoteWorkflowService remoteWorkflowService;
    private final AddressWorkflowHttpClient workflowHttpClient;
    private final StandardAddressApprovalExecutor approvalExecutor;

    /**
     * 目的：提交标准地址新增审批申请。
     * 入参：新增标准地址业务对象。
     * 出参：提交是否成功。
     * 关键约束：审批提交只生成申请单和流程，不直接写 `ADDR_SEGM`。
     * 异常与副作用：会落审批申请单并发起 workflow。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean submitAddApproval(StandardAddressBo bo) {
        String parentStandName = requireParentStandName(bo.getParentSegmId(), "新增失败：父级地址不存在");
        StandardAddressApproval approval = initApproval(OPERATION_ADD, "标准地址新增审批-" + summaryForAdd(bo, parentStandName));
        approval.setTargetSummary(summaryForAdd(bo, parentStandName));
        approval.setRequestPayload(JsonUtils.toJsonString(bo));
        approval.setTargetSnapshot(JsonUtils.toJsonString(List.of(bo)));
        prepareSubmitGuard(approval, bo);
        return persistAndStart(approval);
    }

    /**
     * 目的：提交标准地址修改审批申请。
     * 入参：修改标准地址业务对象。
     * 出参：提交是否成功。
     * 关键约束：必须先确认原地址存在；审批期间正式地址保持不变。
     * 异常与副作用：会落审批申请单并发起 workflow。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean submitUpdateApproval(StandardAddressBo bo) {
        AddrSegm existing = requireAddress(bo.getSegmId(), "修改失败：标准地址不存在");
        StandardAddressApproval approval = initApproval(OPERATION_UPDATE, "标准地址编辑审批-" + existing.getStandName());
        approval.setSourceSummary(existing.getStandName());
        approval.setTargetSummary(StringUtils.defaultIfBlank(bo.getStandName(), bo.getSegmName()));
        approval.setSourceSnapshot(JsonUtils.toJsonString(List.of(existing)));
        approval.setTargetSnapshot(JsonUtils.toJsonString(List.of(bo)));
        approval.setRequestPayload(JsonUtils.toJsonString(bo));
        prepareSubmitGuard(approval, bo);
        return persistAndStart(approval);
    }

    /**
     * 目的：提交标准地址删除审批申请。
     * 入参：待删除地址主键集合和确认标记。
     * 出参：提交是否成功。
     * 关键约束：申请阶段需校验子级地址和安装地址关联确认，避免无效审批单进入流程。
     * 异常与副作用：会落审批申请单并发起 workflow。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean submitDeleteApproval(Collection<String> segmIds, boolean confirm) {
        List<String> normalizedIds = normalizeSegmIds(segmIds);
        validateDeleteRequest(normalizedIds, confirm);
        List<AddrSegm> addresses = addrSegmMapper.selectBatchIds(normalizedIds);
        String summary = joinStandNames(addresses);
        StandardAddressApproval approval = initApproval(OPERATION_DELETE, "标准地址删除审批-" + summary);
        approval.setSourceSummary(summary);
        approval.setSourceSnapshot(JsonUtils.toJsonString(addresses));
        DeleteApprovalPayload payload = new DeleteApprovalPayload(normalizeSegmIdsForFingerprint(normalizedIds), confirm);
        approval.setRequestPayload(JsonUtils.toJsonString(payload));
        prepareSubmitGuard(approval, payload);
        return persistAndStart(approval);
    }

    /**
     * 目的：提交标准地址合并审批申请。
     * 入参：源地址主键集合与目标地址主键。
     * 出参：提交是否成功。
     * 关键约束：目标地址不能包含在源地址中，且所有地址都必须存在。
     * 异常与副作用：会落审批申请单并发起 workflow。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean submitMergeApproval(List<String> sourceSegmIds, String targetSegmId) {
        List<String> normalizedSourceIds = normalizeSegmIds(sourceSegmIds);
        if (normalizedSourceIds.contains(targetSegmId)) {
            throw new ServiceException("合并失败：目标地址不能包含在待合并地址中");
        }
        List<AddrSegm> sources = addrSegmMapper.selectBatchIds(normalizedSourceIds);
        if (sources.size() != normalizedSourceIds.size()) {
            throw new ServiceException("合并失败：待合并地址不存在");
        }
        AddrSegm target = requireAddress(targetSegmId, "合并失败：目标地址不存在");
        StandardAddressApproval approval = initApproval(OPERATION_MERGE, "标准地址合并审批-" + target.getStandName());
        approval.setSourceSummary(joinStandNames(sources));
        approval.setTargetSummary(target.getStandName());
        approval.setSourceSnapshot(JsonUtils.toJsonString(sources));
        approval.setTargetSnapshot(JsonUtils.toJsonString(List.of(target)));
        MergeApprovalPayload payload = new MergeApprovalPayload(normalizeSegmIdsForFingerprint(normalizedSourceIds), targetSegmId);
        approval.setRequestPayload(JsonUtils.toJsonString(payload));
        prepareSubmitGuard(approval, payload);
        return persistAndStart(approval);
    }

    /**
     * 目的：提交标准地址拆分审批申请。
     * 入参：源地址主键和拆分项集合。
     * 出参：提交是否成功。
     * 关键约束：源地址必须存在，拆分项不能为空。
     * 异常与副作用：会落审批申请单并发起 workflow。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean submitSplitApproval(String sourceSegmId, List<StandardAddressSplitItemBo> splitItems) {
        if (splitItems == null || splitItems.isEmpty()) {
            throw new ServiceException("拆分失败：拆分地址项不能为空");
        }
        AddrSegm source = requireAddress(sourceSegmId, "拆分失败：待拆分地址不存在");
        StandardAddressApproval approval = initApproval(OPERATION_SPLIT, "标准地址拆分审批-" + source.getStandName());
        approval.setSourceSummary(source.getStandName());
        approval.setTargetSummary(splitItems.stream().map(StandardAddressSplitItemBo::getSegmName).filter(StringUtils::isNotBlank).collect(Collectors.joining("、")));
        approval.setSourceSnapshot(JsonUtils.toJsonString(List.of(source)));
        approval.setTargetSnapshot(JsonUtils.toJsonString(splitItems));
        SplitApprovalPayload payload = new SplitApprovalPayload(sourceSegmId, splitItems);
        approval.setRequestPayload(JsonUtils.toJsonString(payload));
        prepareSubmitGuard(approval, payload);
        return persistAndStart(approval);
    }

    /**
     * 目的：提交标准地址导入审批申请。
     * 入参：导入行、更新支持标记、操作人和文件名。
     * 出参：审批提交摘要。
     * 关键约束：申请阶段仅保存导入明细，不写正式地址表和导入批次表。
     * 异常与副作用：会落审批申请单并发起 workflow。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StandardAddressImportResultVo submitImportApproval(List<StandardAddressImportVo> rows, Boolean updateSupport, String operName, String fileName) {
        int totalCount = rows == null ? 0 : rows.size();
        StandardAddressApproval approval = initApproval(OPERATION_IMPORT, "标准地址导入审批-" + StringUtils.defaultIfBlank(fileName, "标准地址导入"));
        approval.setTargetSummary("共" + totalCount + "条");
        approval.setImportBatchPayload(JsonUtils.toJsonString(rows));
        approval.setRequestPayload(JsonUtils.toJsonString(new ImportApprovalPayload(Boolean.TRUE.equals(updateSupport), operName, fileName)));
        approval.setTargetSnapshot(JsonUtils.toJsonString(rows));
        prepareSubmitGuard(approval, new ImportFingerprintPayload(Boolean.TRUE.equals(updateSupport), fileName, rows));
        persistAndStart(approval);

        StandardAddressImportResultVo result = new StandardAddressImportResultVo();
        result.setBatchId(approval.getId());
        result.setBatchNo(approval.getApplyNo());
        result.setFileName(fileName);
        result.setStatus(APPROVAL_WAITING);
        result.setTotalCount(totalCount);
        result.setSuccessCount(totalCount);
        result.setFailCount(0);
        result.setFailureExportable(false);
        return result;
    }

    /**
     * 目的：分页查询当前用户提交的审批记录。
     * 入参：审批筛选条件与分页参数。
     * 出参：分页结果。
     * 关键约束：只返回当前登录用户提交的数据。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public TableDataInfo<StandardAddressApprovalVo> queryMyPage(StandardAddressApprovalBo bo, PageQuery pageQuery) {
        StandardAddressApprovalBo queryBo = bo == null ? new StandardAddressApprovalBo() : bo;
        LambdaQueryWrapper<StandardAddressApproval> queryWrapper = new LambdaQueryWrapper<StandardAddressApproval>()
            .eq(StandardAddressApproval::getSubmitUserId, LoginHelper.getUserId())
            .eq(StringUtils.isNotBlank(queryBo.getOperationType()), StandardAddressApproval::getOperationType, queryBo.getOperationType())
            .eq(StringUtils.isNotBlank(queryBo.getApprovalStatus()), StandardAddressApproval::getApprovalStatus, queryBo.getApprovalStatus())
            .and(StringUtils.isNotBlank(queryBo.getKeyword()), wrapper -> wrapper
                .like(StandardAddressApproval::getBizTitle, queryBo.getKeyword())
                .or()
                .like(StandardAddressApproval::getSourceSummary, queryBo.getKeyword())
            .or()
            .like(StandardAddressApproval::getTargetSummary, queryBo.getKeyword()))
            .eq(StandardAddressApproval::getDelFlag, "0")
            .orderByDesc(StandardAddressApproval::getCreateTime);
        Page<StandardAddressApproval> page = approvalMapper.selectPage(pageQuery.build(), queryWrapper);
        Page<StandardAddressApprovalVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVo).toList());
        return TableDataInfo.build(result);
    }

    /**
     * 目的：分页查询当前登录人已办理完成的审批记录。
     * 入参：审批筛选条件与分页参数。
     * 出参：已审批分页结果。
     * 关键约束：仅返回当前登录人真正处理完成的申请单，必须排除仍处于待审批中的“提交审批”历史节点。
     * 异常与副作用：无写入副作用。
     *
     * @param bo 审批筛选条件
     * @param pageQuery 分页参数
     * @return 当前登录人的已审批分页结果
     */
    @Override
    public TableDataInfo<StandardAddressApprovalVo> queryHandledPage(StandardAddressApprovalBo bo, PageQuery pageQuery) {
        StandardAddressApprovalBo queryBo = bo == null ? new StandardAddressApprovalBo() : bo;
        LambdaQueryWrapper<StandardAddressApproval> queryWrapper = buildHandledApprovalQueryWrapper(queryBo);
        Page<StandardAddressApproval> page = approvalMapper.selectPage(pageQuery.build(), queryWrapper);
        Page<StandardAddressApprovalVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVo).toList());
        return TableDataInfo.build(result);
    }

    /**
     * 目的：按当前筛选条件查询“我已审批”的导出结果。
     * 入参：审批筛选条件。
     * 出参：导出用审批记录列表。
     * 关键约束：导出口径与已审批列表一致，只返回当前登录人处理完成的数据。
     * 异常与副作用：无写入副作用。
     *
     * @param bo 审批筛选条件
     * @return 导出用审批记录列表
     */
    @Override
    public List<StandardAddressApprovalVo> listHandledForExport(StandardAddressApprovalBo bo) {
        StandardAddressApprovalBo queryBo = bo == null ? new StandardAddressApprovalBo() : bo;
        return approvalMapper.selectList(buildHandledApprovalQueryWrapper(queryBo)).stream()
            .map(this::toVo)
            .toList();
    }

    /**
     * 目的：按申请单 ID 获取审批详情。
     * 入参：申请单主键。
     * 出参：审批详情。
     * 关键约束：只返回未删除申请单。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public StandardAddressApprovalVo getDetail(Long id) {
        StandardAddressApproval approval = approvalMapper.selectById(id);
        return approval == null || !"0".equals(StringUtils.defaultIfBlank(approval.getDelFlag(), "0")) ? null : toVo(approval);
    }

    /**
     * 目的：按 workflow 业务 ID 获取审批详情。
     * 入参：workflow 业务 ID。
     * 出参：审批详情。
     * 关键约束：业务 ID 与申请单主键保持同值。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public StandardAddressApprovalVo getByBusinessId(String businessId) {
        if (StringUtils.isBlank(businessId)) {
            return null;
        }
        if (!StrUtil.isNumeric(businessId)) {
            return null;
        }
        return getDetail(Long.valueOf(businessId));
    }

    /**
     * 目的：办理标准地址审批通过。
     * 入参：审批动作参数，包含 workflow 任务 ID、业务 ID 和审批意见。
     * 出参：办理是否成功。
     * 关键约束：必须先完成 workflow 当前待办，再执行正式地址写链路；同一审批单重复通过时直接按当前状态回写。
     * 异常与副作用：会调用 workflow 办理接口、执行正式地址写入并更新审批申请单状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean approve(StandardAddressApprovalActionBo bo) {
        StandardAddressApproval approval = requireApprovalByBusinessId(bo.getBusinessId());
        validatePendingApprovalAction(approval, bo.getTaskId());
        completeWorkflowApprovalTask(approval, bo);
        executeApproved(approval, StringUtils.defaultIfBlank(bo.getMessage(), "审批通过"));
        return true;
    }

    /**
     * 目的：办理 workflow 审批任务，并在任务已通过但地址侧尚未落库时做幂等恢复。
     * 入参：审批动作参数，包含业务 ID 与 workflow 任务 ID。
     * 出参：无。
     * 关键约束：仅当 workflow 历史任务确认该任务已 `pass` 时，才允许跳过重复办理并继续执行地址侧正式写入。
     * 异常与副作用：会请求 workflow 办理或历史查询接口；若任务确实未通过则透传原始业务异常。
     *
     * @param approval 审批申请单
     * @param bo 审批动作参数
     */
    private void completeWorkflowApprovalTask(StandardAddressApproval approval, StandardAddressApprovalActionBo bo) {
        try {
            workflowHttpClient.completeTask(buildCompleteTask(bo));
        } catch (ServiceException ex) {
            if (isApprovalProcessedAfterAction(approval.getId(), bo.getTaskId())) {
                throw new ServiceException(APPROVAL_ALREADY_PROCESSED_MESSAGE);
            }
            if (!workflowHttpClient.isTaskPassed(bo.getBusinessId(), bo.getTaskId())) {
                throw ex;
            }
        }
    }

    /**
     * 目的：办理标准地址审批驳回。
     * 入参：审批动作参数，包含 workflow 任务 ID、业务 ID 和驳回意见。
     * 出参：办理是否成功。
     * 关键约束：驳回原因不能为空；驳回后流程需直接终止，申请人只能重新发起，不允许原申请单继续流转。
     * 异常与副作用：会调用 workflow 终止接口并更新审批申请单状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reject(StandardAddressApprovalActionBo bo) {
        String rejectReason = StringUtils.trimToEmpty(bo.getMessage());
        if (StringUtils.isBlank(rejectReason)) {
            throw new ServiceException("驳回原因不能为空");
        }
        StandardAddressApproval approval = requireApprovalByBusinessId(bo.getBusinessId());
        validatePendingApprovalAction(approval, bo.getTaskId());
        try {
            workflowHttpClient.terminationTask(bo.getTaskId(), rejectReason);
        } catch (ServiceException ex) {
            if (isApprovalProcessedAfterAction(approval.getId(), bo.getTaskId())) {
                throw new ServiceException(APPROVAL_ALREADY_PROCESSED_MESSAGE);
            }
            throw ex;
        }
        approval.setApprovalStatus(APPROVAL_REJECTED);
        approval.setBusinessStatus(BusinessStatusEnum.TERMINATION.getStatus());
        approval.setCurrentTaskId(null);
        approval.setRejectReason(rejectReason);
        approval.setApproveUserId(LoginHelper.getUserId());
        approval.setApproveUserName(LoginHelper.getUsername());
        approval.setApproveTime(new java.util.Date());
        approval.setExecuteMessage(null);
        releaseSubmitGuard(approval);
        approvalMapper.updateById(approval);
        return true;
    }

    /**
     * 目的：校验审批动作是否仍指向当前待处理任务。
     * 入参：审批申请单与前端提交的 workflow 任务 ID。
     * 出参：无。
     * 关键约束：只有待审批状态且任务未被他人处理/切换时才允许继续；重复审批统一返回友好提示。
     * 异常与副作用：校验失败时抛业务异常，不产生写入副作用。
     *
     * @param approval 审批申请单
     * @param taskId 前端提交的 workflow 任务 ID
     */
    private void validatePendingApprovalAction(StandardAddressApproval approval, Long taskId) {
        if (!APPROVAL_WAITING.equals(approval.getApprovalStatus())) {
            throw new ServiceException(APPROVAL_ALREADY_PROCESSED_MESSAGE);
        }
        if (approval.getCurrentTaskId() != null && !approval.getCurrentTaskId().equals(taskId)) {
            throw new ServiceException(APPROVAL_ALREADY_PROCESSED_MESSAGE);
        }
    }

    /**
     * 目的：在 workflow 办理异常后回查审批单最新状态，判断是否已被其他人处理。
     * 入参：审批申请单主键与本次办理的任务 ID。
     * 出参：若申请单已非待审批或任务已切换则返回 true。
     * 关键约束：仅用于并发办理场景的友好提示转换，不改变原始审批数据。
     * 异常与副作用：只读数据库，无写入副作用。
     *
     * @param approvalId 审批申请单主键
     * @param taskId 本次办理的任务 ID
     * @return 是否已被其他人处理
     */
    private boolean isApprovalProcessedAfterAction(Long approvalId, Long taskId) {
        StandardAddressApproval latestApproval = approvalMapper.selectById(approvalId);
        return latestApproval != null
            && (!APPROVAL_WAITING.equals(latestApproval.getApprovalStatus())
            || (latestApproval.getCurrentTaskId() != null && !latestApproval.getCurrentTaskId().equals(taskId)));
    }

    /**
     * 目的：初始化审批申请单公共字段。
     * 入参：操作类型与业务标题。
     * 出参：初始化完成的审批实体。
     * 关键约束：公共状态统一由此方法创建，避免不同操作类型口径不一致。
     * 异常与副作用：仅在内存中构建对象，不直接写库。
     */
    private StandardAddressApproval initApproval(String operationType, String bizTitle) {
        StandardAddressApproval approval = new StandardAddressApproval();
        approval.setId(IdUtil.getSnowflakeNextId());
        approval.setApplyNo("STDADDRAPP" + approval.getId());
        approval.setOperationType(operationType);
        approval.setFlowCode(FLOW_CODE);
        approval.setBizTitle(bizTitle);
        approval.setBusinessStatus(BusinessStatusEnum.WAITING.getStatus());
        approval.setApprovalStatus(APPROVAL_WAITING);
        approval.setSubmitUserId(LoginHelper.getUserId());
        approval.setSubmitUserName(LoginHelper.getUsername());
        approval.setSubmitDeptId(LoginHelper.getDeptId());
        approval.setSubmitDeptName(LoginHelper.getDeptName());
        approval.setSubmitGuardKey(ACTIVE_SUBMIT_GUARD_KEY);
        approval.setDelFlag("0");
        return approval;
    }

    /**
     * 目的：持久化审批申请单并启动 workflow。
     * 入参：审批申请单实体。
     * 出参：提交是否成功。
     * 关键约束：落库与流程发起必须放在同一事务中，任一失败都需回滚。
     * 异常与副作用：会写入申请单并发起 workflow。
     */
    private Boolean persistAndStart(StandardAddressApproval approval) {
        ensureNoActiveDuplicate(approval);
        try {
            approvalMapper.insert(approval);
        } catch (DuplicateKeyException ex) {
            StandardAddressApproval existing = findActiveDuplicateApproval(approval);
            if (existing != null) {
                throw buildDuplicateSubmitException(existing);
            }
            throw ex;
        }
        RemoteStartProcess startProcess = new RemoteStartProcess();
        startProcess.setBusinessId(String.valueOf(approval.getId()));
        startProcess.setFlowCode(FLOW_CODE);
        Map<String, Object> variables = new HashMap<>(4);
        variables.put("ignore", true);
        startProcess.setVariables(variables);
        RemoteFlowInstanceBizExt bizExt = new RemoteFlowInstanceBizExt();
        bizExt.setBusinessCode(approval.getApplyNo());
        bizExt.setBusinessTitle(approval.getBizTitle());
        startProcess.setBizExt(bizExt);
        boolean started = remoteWorkflowService.startCompleteTask(startProcess);
        if (!started) {
            throw new ServiceException("标准地址审批流程发起失败");
        }
        approval.setInstanceId(remoteWorkflowService.getInstanceIdByBusinessId(String.valueOf(approval.getId())));
        approvalMapper.updateById(approval);
        return true;
    }

    /**
     * 目的：构建审批通过的 workflow 办理参数。
     * 入参：审批动作参数。
     * 出参：workflow 办理任务参数。
     * 关键约束：统一携带 `ignore=true`，保持与现有前端办理口径一致。
     * 异常与副作用：仅构建内存对象，不直接写库。
     */
    private org.dromara.workflow.api.domain.RemoteCompleteTask buildCompleteTask(StandardAddressApprovalActionBo bo) {
        org.dromara.workflow.api.domain.RemoteCompleteTask completeTask = new org.dromara.workflow.api.domain.RemoteCompleteTask();
        completeTask.setTaskId(bo.getTaskId());
        completeTask.setMessage(StringUtils.defaultIfBlank(StringUtils.trimToEmpty(bo.getMessage()), "审批通过"));
        Map<String, Object> variables = new HashMap<>(2);
        variables.put("ignore", true);
        completeTask.setVariables(variables);
        return completeTask;
    }

    /**
     * 目的：按审批通过结果执行正式地址变更并回写审批状态。
     * 入参：审批申请单与审批意见。
     * 出参：无。
     * 关键约束：正式写链路必须复用既有命令服务；执行失败时保留 workflow 已完成事实并标记申请单执行失败。
     * 异常与副作用：会更新审批单状态，并可能修改正式地址表、导入批次与 ES 索引。
     */
    private void executeApproved(StandardAddressApproval approval, String message) {
        approval.setApprovalStatus(APPROVAL_EXECUTING);
        approval.setBusinessStatus(BusinessStatusEnum.FINISH.getStatus());
        approval.setApproveUserId(LoginHelper.getUserId());
        approval.setApproveUserName(LoginHelper.getUsername());
        approval.setApproveTime(new java.util.Date());
        approval.setCurrentTaskId(null);
        approval.setRejectReason(null);
        approvalMapper.updateById(approval);
        try {
            approvalExecutor.execute(approval);
            approval.setApprovalStatus(APPROVAL_APPROVED);
            approval.setExecuteMessage(StringUtils.defaultIfBlank(message, "正式地址变更已生效"));
        } catch (Exception ex) {
            approval.setApprovalStatus(APPROVAL_EXECUTE_FAILED);
            approval.setExecuteMessage(StringUtils.defaultIfBlank(ex.getMessage(), "正式地址变更执行失败"));
        }
        releaseSubmitGuard(approval);
        approvalMapper.updateById(approval);
    }

    /**
     * 目的：按业务 ID 加载有效审批申请单。
     * 入参：workflow 业务 ID。
     * 出参：审批申请单实体。
     * 关键约束：业务 ID 必须是数值且对应未删除申请单。
     * 异常与副作用：申请单不存在时抛出业务异常，不产生写入副作用。
     */
    private StandardAddressApproval requireApprovalByBusinessId(String businessId) {
        StandardAddressApprovalVo detail = getByBusinessId(businessId);
        if (detail == null || detail.getId() == null) {
            throw new ServiceException("标准地址审批单不存在");
        }
        StandardAddressApproval approval = approvalMapper.selectById(detail.getId());
        if (approval == null || !"0".equals(StringUtils.defaultIfBlank(approval.getDelFlag(), "0"))) {
            throw new ServiceException("标准地址审批单不存在");
        }
        return approval;
    }

    /**
     * 目的：校验删除申请的前置约束。
     * 入参：待删除地址集合与确认标记。
     * 出参：无。
     * 关键约束：存在子级地址禁止提交审批；存在安装地址关联且未确认时禁止提交审批。
     * 异常与副作用：校验失败时直接抛异常，不产生写入副作用。
     */
    private void validateDeleteRequest(List<String> segmIds, boolean confirm) {
        if (segmIds.isEmpty()) {
            throw new ServiceException("删除失败：标准地址ID不能为空");
        }
        List<AddrSegm> addresses = addrSegmMapper.selectBatchIds(segmIds);
        if (addresses.size() != segmIds.size()) {
            throw new ServiceException("删除失败：标准地址不存在");
        }
        Long childCount = addrSegmMapper.countChildren(segmIds);
        if (childCount != null && childCount > 0) {
            throw new ServiceException("删除失败：存在下级标准地址");
        }
        Long installCount = addrSetSegmMapper.countBySegmIds(segmIds);
        if (installCount != null && installCount > 0 && !confirm) {
            throw new ServiceException("删除失败：存在关联安装地址，请确认后重试");
        }
    }

    /**
     * 目的：根据地址主键加载有效地址。
     * 入参：地址主键与异常提示信息。
     * 出参：命中的地址实体。
     * 关键约束：只允许审批未删除的正式地址。
     * 异常与副作用：未命中时抛出业务异常，无写入副作用。
     */
    private AddrSegm requireAddress(String segmId, String message) {
        if (StringUtils.isBlank(segmId)) {
            throw new ServiceException(message);
        }
        AddrSegm address = addrSegmMapper.selectById(segmId);
        if (address == null || !"0".equals(StringUtils.blankToDefault(address.getDeleteState(), "0"))) {
            throw new ServiceException(message);
        }
        return address;
    }

    /**
     * 目的：标准化地址主键集合。
     * 入参：原始主键集合。
     * 出参：去重去空后的主键列表。
     * 关键约束：保留原有顺序，便于摘要展示和正式执行保持一致。
     * 异常与副作用：无写入副作用。
     */
    private List<String> normalizeSegmIds(Collection<String> segmIds) {
        Set<String> normalized = new LinkedHashSet<>();
        if (segmIds != null) {
            for (String segmId : segmIds) {
                if (StringUtils.isNotBlank(segmId)) {
                    normalized.add(segmId.trim());
                }
            }
        }
        return List.copyOf(normalized);
    }

    /**
     * 目的：规范化用于提交指纹计算的地址主键集合。
     * 入参：原始主键集合。
     * 出参：排序后的主键列表。
     * 关键约束：同一批地址仅顺序不同也必须命中相同提交指纹。
     * 异常与副作用：无写入副作用。
     *
     * @param segmIds 原始主键集合
     * @return 排序后的主键列表
     */
    private List<String> normalizeSegmIdsForFingerprint(Collection<String> segmIds) {
        return normalizeSegmIds(segmIds).stream()
            .sorted()
            .toList();
    }

    /**
     * 目的：为审批申请准备提交指纹与活跃占位键。
     * 入参：审批申请单与指纹载荷。
     * 出参：无。
     * 关键约束：同一用户、同一操作类型、同一规范化提交内容必须生成相同指纹。
     * 异常与副作用：仅更新内存对象，不直接写库。
     *
     * @param approval 审批申请单
     * @param fingerprintPayload 指纹载荷
     */
    private void prepareSubmitGuard(StandardAddressApproval approval, Object fingerprintPayload) {
        approval.setSubmitFingerprint(buildSubmitFingerprint(approval.getOperationType(), fingerprintPayload));
        approval.setSubmitGuardKey(ACTIVE_SUBMIT_GUARD_KEY);
    }

    /**
     * 目的：构建重复提交指纹。
     * 入参：操作类型与规范化指纹载荷。
     * 出参：MD5 指纹字符串。
     * 关键约束：指纹仅依赖稳定业务字段，避免无关字段干扰。
     * 异常与副作用：无写入副作用。
     *
     * @param operationType 操作类型
     * @param fingerprintPayload 规范化指纹载荷
     * @return 提交指纹
     */
    private String buildSubmitFingerprint(String operationType, Object fingerprintPayload) {
        String source = JsonUtils.toJsonString(Map.of(
            "operationType", StringUtils.defaultString(operationType),
            "payload", fingerprintPayload
        ));
        return SecureUtil.md5(source);
    }

    /**
     * 目的：校验当前提交是否已存在活跃重复审批单。
     * 入参：待提交审批申请单。
     * 出参：无。
     * 关键约束：只拦截当前用户、同操作类型、同提交指纹且仍占用活跃提交位的审批单。
     * 异常与副作用：命中重复时抛业务异常，不产生写入副作用。
     *
     * @param approval 待提交审批申请单
     */
    private void ensureNoActiveDuplicate(StandardAddressApproval approval) {
        StandardAddressApproval existing = findActiveDuplicateApproval(approval);
        if (existing != null) {
            throw buildDuplicateSubmitException(existing);
        }
    }

    /**
     * 目的：查询当前提交对应的活跃重复审批单。
     * 入参：待提交审批申请单。
     * 出参：已存在审批单；未命中时返回 `null`。
     * 关键约束：只匹配未删除且仍处于活跃提交位的审批单。
     * 异常与副作用：只读数据库，无写入副作用。
     *
     * @param approval 待提交审批申请单
     * @return 已存在的活跃重复审批单
     */
    private StandardAddressApproval findActiveDuplicateApproval(StandardAddressApproval approval) {
        return approvalMapper.selectOne(new LambdaQueryWrapper<StandardAddressApproval>()
            .eq(StandardAddressApproval::getSubmitUserId, approval.getSubmitUserId())
            .eq(StandardAddressApproval::getOperationType, approval.getOperationType())
            .eq(StandardAddressApproval::getSubmitFingerprint, approval.getSubmitFingerprint())
            .eq(StandardAddressApproval::getSubmitGuardKey, ACTIVE_SUBMIT_GUARD_KEY)
            .eq(StandardAddressApproval::getDelFlag, "0"));
    }

    /**
     * 目的：构建重复提交友好提示。
     * 入参：已存在审批单。
     * 出参：业务异常对象。
     * 关键约束：若审批单号存在，则提示中需显式带出审批单号。
     * 异常与副作用：仅构建异常，不写库。
     *
     * @param existing 已存在审批单
     * @return 业务异常
     */
    private ServiceException buildDuplicateSubmitException(StandardAddressApproval existing) {
        String applyNo = existing == null ? null : existing.getApplyNo();
        String message = StringUtils.isNotBlank(applyNo)
            ? DUPLICATE_SUBMIT_MESSAGE + "。申请单号：" + applyNo
            : DUPLICATE_SUBMIT_MESSAGE;
        return new ServiceException(message);
    }

    /**
     * 目的：审批结束后释放重复提交保护占位。
     * 入参：审批申请单。
     * 出参：无。
     * 关键约束：只有审批进入终态后才允许释放，避免执行中重复提单。
     * 异常与副作用：仅更新内存对象，不直接写库。
     *
     * @param approval 审批申请单
     */
    private void releaseSubmitGuard(StandardAddressApproval approval) {
        approval.setSubmitGuardKey(StringUtils.defaultIfBlank(approval.getApplyNo(), String.valueOf(approval.getId())));
    }

    /**
     * 目的：构建新增审批摘要。
     * 入参：新增 BO 与父地址实体。
     * 出参：摘要字符串。
     * 关键约束：优先展示完整地址，否则回退当级名称。
     * 异常与副作用：无写入副作用。
     */
    private String summaryForAdd(StandardAddressBo bo, String parentStandName) {
        String segmName = StringUtils.defaultIfBlank(bo.getSegmName(), bo.getName());
        if (StringUtils.isBlank(parentStandName)) {
            return segmName;
        }
        return parentStandName + segmName;
    }

    /**
     * 目的：拼接多个原地址名称摘要。
     * 入参：地址实体集合。
     * 出参：用顿号拼接的地址名称。
     * 关键约束：空名称自动跳过。
     * 异常与副作用：无写入副作用。
     */
    private String joinStandNames(List<AddrSegm> addresses) {
        return addresses.stream()
            .map(AddrSegm::getStandName)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining("、"));
    }

    /**
     * 目的：解析父级地址展示名称，兼容区域级和普通标准地址两类父节点。
     * 入参：父级地址主键与异常提示。
     * 出参：父级标准地址名称。
     * 关键约束：区域级父级从 `spc_region` 读取，普通父级从 `ADDR_SEGM` 读取。
     * 异常与副作用：未命中时抛出业务异常，不产生写入副作用。
     */
    private String requireParentStandName(String parentSegmId, String message) {
        AddrSegm parent = addrSegmMapper.selectById(parentSegmId);
        if (parent != null && "0".equals(StringUtils.blankToDefault(parent.getDeleteState(), "0"))) {
            return parent.getStandName();
        }
        SpcRegion region = spcRegionMapper.selectById(parentSegmId);
        if (region != null) {
            return region.getRegionName();
        }
        throw new ServiceException(message);
    }

    /**
     * 目的：构建“我已审批”列表与导出共用查询条件。
     * 入参：审批筛选条件。
     * 出参：统一的 MyBatis 查询包装器。
     * 关键约束：仅返回当前登录人已处理完成的审批记录，并按审批时间倒序输出。
     * 异常与副作用：无写入副作用。
     *
     * @param queryBo 审批筛选条件
     * @return 已审批查询条件
     */
    private LambdaQueryWrapper<StandardAddressApproval> buildHandledApprovalQueryWrapper(StandardAddressApprovalBo queryBo) {
        return new LambdaQueryWrapper<StandardAddressApproval>()
            .eq(StandardAddressApproval::getApproveUserId, LoginHelper.getUserId())
            .in(StandardAddressApproval::getApprovalStatus, List.of(APPROVAL_APPROVED, APPROVAL_REJECTED, APPROVAL_EXECUTE_FAILED))
            .eq(StandardAddressApproval::getDelFlag, "0")
            .and(StringUtils.isNotBlank(queryBo.getKeyword()), wrapper -> wrapper
                .like(StandardAddressApproval::getBizTitle, queryBo.getKeyword())
                .or()
                .like(StandardAddressApproval::getSourceSummary, queryBo.getKeyword())
                .or()
                .like(StandardAddressApproval::getTargetSummary, queryBo.getKeyword()))
            .orderByDesc(StandardAddressApproval::getApproveTime)
            .orderByDesc(StandardAddressApproval::getCreateTime);
    }

    /**
     * 目的：把审批实体转换为展示 VO。
     * 入参：审批实体。
     * 出参：展示 VO。
     * 关键约束：当前版本保持一一映射，复杂快照解析由前端详情按需处理。
     * 异常与副作用：无写入副作用。
     */
    private StandardAddressApprovalVo toVo(StandardAddressApproval approval) {
        StandardAddressApprovalVo vo = new StandardAddressApprovalVo();
        vo.setId(approval.getId());
        vo.setApplyNo(approval.getApplyNo());
        vo.setOperationType(approval.getOperationType());
        vo.setApprovalStatus(approval.getApprovalStatus());
        vo.setApprovalStatusName(resolveApprovalStatusName(approval.getApprovalStatus()));
        vo.setBusinessStatus(approval.getBusinessStatus());
        vo.setBizTitle(approval.getBizTitle());
        vo.setSourceSummary(approval.getSourceSummary());
        vo.setTargetSummary(approval.getTargetSummary());
        vo.setInstanceId(approval.getInstanceId());
        vo.setCurrentTaskId(approval.getCurrentTaskId());
        vo.setSubmitUserName(approval.getSubmitUserName());
        vo.setSubmitDeptName(approval.getSubmitDeptName());
        vo.setApproveUserId(approval.getApproveUserId());
        vo.setApproveUserName(approval.getApproveUserName());
        vo.setApproveTime(approval.getApproveTime());
        vo.setRejectReason(approval.getRejectReason());
        vo.setExecuteMessage(approval.getExecuteMessage());
        vo.setSourceSnapshot(approval.getSourceSnapshot());
        vo.setTargetSnapshot(approval.getTargetSnapshot());
        vo.setRequestPayload(approval.getRequestPayload());
        vo.setImportBatchPayload(approval.getImportBatchPayload());
        vo.setCreateTime(approval.getCreateTime());
        return vo;
    }

    /**
     * 目的：把审批状态码转换为可读中文名称。
     * 入参：审批状态码。
     * 出参：中文展示名称。
     * 关键约束：导出与页面列表需使用统一状态文案；未知状态回退原始状态码。
     * 异常与副作用：无写入副作用。
     *
     * @param approvalStatus 审批状态码
     * @return 中文展示名称
     */
    private String resolveApprovalStatusName(String approvalStatus) {
        return switch (StringUtils.defaultString(approvalStatus)) {
            case APPROVAL_WAITING -> "待审批";
            case APPROVAL_APPROVED -> "审批通过";
            case APPROVAL_REJECTED -> "审批驳回";
            case APPROVAL_EXECUTING -> "执行中";
            case APPROVAL_EXECUTE_FAILED -> "执行失败";
            default -> StringUtils.defaultIfBlank(approvalStatus, "-");
        };
    }

    @Data
    public static class DeleteApprovalPayload {
        private List<String> segmIds;
        private boolean confirm;

        public DeleteApprovalPayload() {
        }

        public DeleteApprovalPayload(List<String> segmIds, boolean confirm) {
            this.segmIds = segmIds;
            this.confirm = confirm;
        }
    }

    @Data
    public static class MergeApprovalPayload {
        private List<String> sourceSegmIds;
        private String targetSegmId;

        public MergeApprovalPayload() {
        }

        public MergeApprovalPayload(List<String> sourceSegmIds, String targetSegmId) {
            this.sourceSegmIds = sourceSegmIds;
            this.targetSegmId = targetSegmId;
        }
    }

    @Data
    public static class SplitApprovalPayload {
        private String sourceSegmId;
        private List<StandardAddressSplitItemBo> splitItems;

        public SplitApprovalPayload() {
        }

        public SplitApprovalPayload(String sourceSegmId, List<StandardAddressSplitItemBo> splitItems) {
            this.sourceSegmId = sourceSegmId;
            this.splitItems = splitItems;
        }
    }

    @Data
    public static class ImportApprovalPayload {
        private Boolean updateSupport;
        private String operName;
        private String fileName;

        public ImportApprovalPayload() {
        }

        public ImportApprovalPayload(Boolean updateSupport, String operName, String fileName) {
            this.updateSupport = updateSupport;
            this.operName = operName;
            this.fileName = fileName;
        }
    }

    @Data
    public static class ImportFingerprintPayload {
        private Boolean updateSupport;
        private String fileName;
        private List<StandardAddressImportVo> rows;

        public ImportFingerprintPayload() {
        }

        public ImportFingerprintPayload(Boolean updateSupport, String fileName, List<StandardAddressImportVo> rows) {
            this.updateSupport = updateSupport;
            this.fileName = fileName;
            this.rows = rows == null ? List.of() : new ArrayList<>(rows).stream()
                .sorted(Comparator.comparing(item -> StringUtils.defaultString(item.getSegmName())
                    + "|" + StringUtils.defaultString(item.getParentStandName())
                    + "|" + StringUtils.defaultString(item.getAddrLevel() == null ? null : String.valueOf(item.getAddrLevel()))))
                .toList();
        }
    }
}
