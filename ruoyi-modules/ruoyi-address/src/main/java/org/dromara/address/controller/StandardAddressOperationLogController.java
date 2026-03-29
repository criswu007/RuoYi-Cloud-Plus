package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.StandardAddressOperationLogBo;
import org.dromara.address.domain.vo.StandardAddressOperationLogVo;
import org.dromara.address.service.IStandardAddressOperationLogService;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotNull;

/**
 * 地址操作日志对外接口。
 * 目的：提供操作日志查询能力，供运维与审计使用。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/address/operation-log")
public class StandardAddressOperationLogController extends BaseController {

    private final IStandardAddressOperationLogService operationLogService;

    /**
     * 查询操作日志列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @SaCheckPermission("address:operation:log:list")
    @GetMapping("/list")
    public TableDataInfo<StandardAddressOperationLogVo> list(StandardAddressOperationLogBo bo, PageQuery pageQuery) {
        return operationLogService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取操作日志详情。
     *
     * @param id 主键ID
     * @return 日志详情
     */
    @SaCheckPermission("address:operation:log:query")
    @GetMapping("/{id}")
    public R<StandardAddressOperationLogVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(operationLogService.queryById(id));
    }
}
