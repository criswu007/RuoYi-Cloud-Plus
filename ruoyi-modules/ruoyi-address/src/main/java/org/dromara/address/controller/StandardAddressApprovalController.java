package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.StandardAddressApprovalActionBo;
import org.dromara.address.domain.bo.StandardAddressApprovalBo;
import org.dromara.address.domain.vo.StandardAddressApprovalVo;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.address.service.IStandardAddressApprovalService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标准地址审批记录控制器。
 * 目的：提供“我提交的”“我已审批”的审批单分页、审批详情和按 workflow 业务 ID 查询详情接口。
 * 入参/出参：输入审批筛选条件、申请单主键或 workflow 业务 ID，输出审批记录分页或详情。
 * 关键约束：业务详情主键与 workflow `businessId` 保持一致；审批动作统一由地址模块封装 workflow 细节。
 * 异常与副作用：查询接口无数据库写入副作用；审批动作会更新申请单和 workflow 任务状态。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/address/standard/approval")
public class StandardAddressApprovalController {

    private static final String ADDRESS_APPROVER_ROLE_KEY = "address_approver";
    private static final String APPROVAL_ACTION_PERMISSION_MESSAGE = "当前用户没有标准地址审批权限，请联系管理员授权“标准地址审批员”角色或使用系统管理员账号办理";

    private final IStandardAddressApprovalService approvalService;

    /**
     * 目的：分页查询当前登录人提交的标准地址审批申请。
     * 入参：审批筛选条件与分页参数。
     * 出参：审批申请分页结果。
     * 关键约束：服务层会自动限制为当前登录用户提交的数据。
     * 异常与副作用：无写入副作用。
     */
    @SaCheckPermission("address:standard:list")
    @PostMapping("/my/page")
    public TableDataInfo<StandardAddressApprovalVo> queryMyPage(StandardAddressApprovalBo bo, PageQuery pageQuery) {
        return approvalService.queryMyPage(bo, pageQuery);
    }

    /**
     * 目的：分页查询当前登录人已办理完成的标准地址审批申请。
     * 入参：审批筛选条件与分页参数。
     * 出参：已审批申请分页结果。
     * 关键约束：服务层会自动限制为当前登录人已实际办理完成的申请，不返回仍在审批中的提交流程记录。
     * 异常与副作用：无写入副作用。
     */
    @PostMapping("/handled/page")
    public TableDataInfo<StandardAddressApprovalVo> queryHandledPage(StandardAddressApprovalBo bo, PageQuery pageQuery) {
        checkApprovalHandledListPermission();
        return approvalService.queryHandledPage(bo, pageQuery);
    }

    /**
     * 目的：导出当前登录人的已审批标准地址记录。
     * 入参：审批筛选条件与响应输出流。
     * 出参：无，直接向响应流写出 Excel。
     * 关键约束：导出口径与“已审批”列表保持一致；拥有标准地址导出权限、列表权限，或审批角色的用户才允许导出。
     * 异常与副作用：会写出 Excel 文件流，不产生数据库写入副作用。
     */
    @Log(title = "标准地址审批记录", businessType = BusinessType.EXPORT)
    @PostMapping("/handled/export")
    public void exportHandledApprovals(StandardAddressApprovalBo bo, HttpServletResponse response) throws Exception {
        checkApprovalHandledExportPermission();
        FileUtils.setAttachmentResponseHeader(response, ExcelUtil.encodingFilename("标准地址已审批记录"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        ExcelUtil.exportExcel(approvalService.listHandledForExport(bo), "标准地址已审批记录", StandardAddressApprovalVo.class, response);
    }

    /**
     * 目的：根据审批申请单主键查询详情。
     * 入参：审批申请单主键。
     * 出参：审批详情。
     * 关键约束：只返回未删除审批单；详情中的快照字段保留 JSON 原文，供前端按需渲染。
     * 异常与副作用：无写入副作用。
     */
    @SaCheckPermission("address:standard:query")
    @PostMapping("/{id}")
    public R<StandardAddressApprovalVo> getDetail(@NotNull(message = "审批单主键不能为空") @PathVariable Long id) {
        return R.ok(approvalService.getDetail(id));
    }

    /**
     * 目的：根据 workflow 业务 ID 查询审批详情。
     * 入参：workflow 业务 ID。
     * 出参：审批详情。
     * 关键约束：`businessId` 与审批申请单主键同值；用于 workflow 待办/已办列表点击详情后的业务数据回填。
     * 异常与副作用：无写入副作用。
     */
    @PostMapping("/business/{businessId}")
    public R<StandardAddressApprovalVo> getByBusinessId(@NotNull(message = "业务ID不能为空") @PathVariable String businessId) {
        checkApprovalQueryPermission();
        return R.ok(approvalService.getByBusinessId(businessId));
    }

    /**
     * 目的：查询当前登录人是否具备标准地址审批办理权限。
     * 入参：无。
     * 出参：返回是否可办理通过/驳回，以及对应提示文案。
     * 关键约束：权限判定口径必须与审批通过/驳回接口保持完全一致，避免页面显隐与实际校验不一致。
     * 异常与副作用：无写入副作用，仅基于当前登录态返回权限结果。
     *
     * @return 当前登录人的审批办理权限结果
     */
    @PostMapping("/action/permission")
    public R<java.util.Map<String, Object>> getActionPermission() {
        boolean canApprove = hasApprovalRole();
        java.util.Map<String, Object> result = new java.util.HashMap<>(4);
        result.put("canApprove", canApprove);
        result.put("message", canApprove ? "" : APPROVAL_ACTION_PERMISSION_MESSAGE);
        return R.ok(result);
    }

    /**
     * 目的：办理标准地址审批通过。
     * 入参：审批动作参数，包含 workflow 任务 ID、业务 ID 和审批意见。
     * 出参：办理结果。
     * 关键约束：只允许对当前待办任务发起办理；正式地址写链路由服务层统一执行。
     * 异常与副作用：会调用 workflow 办理接口并更新审批申请单、正式地址数据。
     */
    @PostMapping("/approve")
    @RepeatSubmit(interval = 5000)
    public R<Void> approve(@Valid @org.springframework.web.bind.annotation.RequestBody StandardAddressApprovalActionBo bo) {
        checkApprovalActionPermission();
        return approvalService.approve(bo) ? R.ok() : R.fail("审批通过失败");
    }

    /**
     * 目的：办理标准地址审批驳回。
     * 入参：审批动作参数，包含 workflow 任务 ID、业务 ID 和驳回意见。
     * 出参：办理结果。
     * 关键约束：驳回原因不能为空；驳回后会直接终止当前流程，申请人需重新从业务入口发起新申请。
     * 异常与副作用：会调用 workflow 终止接口并更新审批申请单状态。
     */
    @PostMapping("/reject")
    @RepeatSubmit(interval = 5000)
    public R<Void> reject(@Valid @org.springframework.web.bind.annotation.RequestBody StandardAddressApprovalActionBo bo) {
        checkApprovalActionPermission();
        return approvalService.reject(bo) ? R.ok() : R.fail("审批驳回失败");
    }

    /**
     * 目的：校验“已审批”列表访问权限。
     * 入参：无。
     * 出参：无。
     * 关键约束：具备标准地址查询权限，或拥有“标准地址审批员/系统管理员”角色时才允许访问。
     * 异常与副作用：无权限时抛出 Sa-Token 权限异常，不产生写入副作用。
     */
    private void checkApprovalHandledListPermission() {
        if (StpUtil.hasPermission("address:standard:list") || hasApprovalRole()) {
            return;
        }
        throw new NotPermissionException("address:standard:list");
    }

    /**
     * 目的：校验审批详情访问权限。
     * 入参：无。
     * 出参：无。
     * 关键约束：具备标准地址详情权限，或拥有“标准地址审批员/系统管理员”角色时才允许访问。
     * 异常与副作用：无权限时抛出 Sa-Token 权限异常，不产生写入副作用。
     */
    private void checkApprovalQueryPermission() {
        if (StpUtil.hasPermission("address:standard:query") || hasApprovalRole()) {
            return;
        }
        throw new NotPermissionException("address:standard:query");
    }

    /**
     * 目的：校验审批通过/驳回动作权限。
     * 入参：无。
     * 出参：无。
     * 关键约束：只有“标准地址审批员”或“系统管理员”角色允许办理审批，避免普通编辑权限越权审批。
     * 异常与副作用：无权限时抛出 Sa-Token 权限异常，不产生写入副作用。
     */
    private void checkApprovalActionPermission() {
        if (hasApprovalRole()) {
            return;
        }
        throw new ServiceException(APPROVAL_ACTION_PERMISSION_MESSAGE);
    }

    /**
     * 目的：校验“已审批导出”访问权限。
     * 入参：无。
     * 出参：无。
     * 关键约束：拥有标准地址导出权限、列表权限或审批角色时才允许导出。
     * 异常与副作用：无权限时抛出 Sa-Token 权限异常，不产生写入副作用。
     */
    private void checkApprovalHandledExportPermission() {
        if (StpUtil.hasPermission("address:standard:export")
            || StpUtil.hasPermission("address:standard:list")
            || hasApprovalRole()) {
            return;
        }
        throw new NotPermissionException("address:standard:export");
    }

    /**
     * 目的：判断当前登录人是否具备标准地址审批角色。
     * 入参：无。
     * 出参：是否拥有“标准地址审批员”或“系统管理员”角色。
     * 关键约束：审批动作统一以角色为准，不依赖菜单按钮权限。
     * 异常与副作用：仅查询当前登录态，不产生写入副作用。
     *
     * @return 是否具备审批角色
     */
    private boolean hasApprovalRole() {
        return StpUtil.hasRoleOr(TenantConstants.SUPER_ADMIN_ROLE_KEY, ADDRESS_APPROVER_ROLE_KEY);
    }
}
