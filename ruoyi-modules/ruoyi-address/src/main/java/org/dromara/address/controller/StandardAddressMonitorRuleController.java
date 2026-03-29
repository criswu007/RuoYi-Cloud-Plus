package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.StandardAddressMonitorRuleBo;
import org.dromara.address.domain.vo.StandardAddressMonitorRuleVo;
import org.dromara.address.service.IStandardAddressMonitorRuleService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 地址监控规则对外接口。
 * 目的：提供规则配置与管理能力，供监控任务使用。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/address/monitor/rule")
public class StandardAddressMonitorRuleController extends BaseController {

    private final IStandardAddressMonitorRuleService monitorRuleService;

    /**
     * 查询规则列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @SaCheckPermission("address:monitor:rule:list")
    @GetMapping("/list")
    public TableDataInfo<StandardAddressMonitorRuleVo> list(StandardAddressMonitorRuleBo bo, PageQuery pageQuery) {
        return monitorRuleService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取规则详情。
     *
     * @param id 主键ID
     * @return 规则详情
     */
    @SaCheckPermission("address:monitor:rule:query")
    @GetMapping("/{id}")
    public R<StandardAddressMonitorRuleVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(monitorRuleService.queryById(id));
    }

    /**
     * 新增规则。
     *
     * @param bo 新增参数
     * @return 操作结果
     */
    @SaCheckPermission("address:monitor:rule:add")
    @Log(title = "地址监控规则", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody StandardAddressMonitorRuleBo bo) {
        return toAjax(monitorRuleService.insertByBo(bo));
    }

    /**
     * 修改规则。
     *
     * @param bo 修改参数
     * @return 操作结果
     */
    @SaCheckPermission("address:monitor:rule:edit")
    @Log(title = "地址监控规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody StandardAddressMonitorRuleBo bo) {
        return toAjax(monitorRuleService.updateByBo(bo));
    }

    /**
     * 删除规则。
     *
     * @param ids 主键集合
     * @return 操作结果
     */
    @SaCheckPermission("address:monitor:rule:remove")
    @Log(title = "地址监控规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(monitorRuleService.deleteWithValidByIds(List.of(ids), true));
    }
}
