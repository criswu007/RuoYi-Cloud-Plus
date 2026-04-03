package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.StandardAddressManagementStationBo;
import org.dromara.address.domain.vo.StandardAddressManagementStationVo;
import org.dromara.address.service.IStandardAddressManagementStationService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理站管理接口。
 * 目的：提供原型“管理站管理”页面所需的分页查询、详情、新增、修改、删除能力。
 * 入参/出参：查询接口使用表单参数 + `PageQuery`，写接口使用 JSON 请求体，统一返回 `R`/`TableDataInfo`。
 * 关键约束：内部管理端统一采用 `POST`；查询字段仅按页面交互保留名称检索。
 * 异常与副作用：新增/修改/删除会写入管理站表；查询无写入副作用。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/address/station")
public class StandardAddressManagementStationController extends BaseController {

    private final IStandardAddressManagementStationService managementStationService;

    /**
     * 查询管理站列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     *
     * 关键约束：分页由 MyBatis-Plus 下推执行。
     * 异常与副作用：无写入副作用。
     */
    @SaCheckPermission("address:station:list")
    @PostMapping("/list")
    public TableDataInfo<StandardAddressManagementStationVo> list(StandardAddressManagementStationBo bo, PageQuery pageQuery) {
        return managementStationService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询管理站详情。
     *
     * @param id 管理站主键
     * @return 管理站详情
     *
     * 关键约束：主键不能为空。
     * 异常与副作用：无写入副作用。
     */
    @SaCheckPermission("address:station:query")
    @PostMapping("/{id}")
    public R<StandardAddressManagementStationVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(managementStationService.queryById(id));
    }

    /**
     * 新增管理站。
     *
     * @param bo 新增参数
     * @return 操作结果
     *
     * 关键约束：管理站名称不能为空。
     * 异常与副作用：会写入管理站表。
     */
    @SaCheckPermission("address:station:add")
    @Log(title = "管理站", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody StandardAddressManagementStationBo bo) {
        return toAjax(managementStationService.insertByBo(bo));
    }

    /**
     * 修改管理站。
     *
     * @param bo 修改参数
     * @return 操作结果
     *
     * 关键约束：主键不能为空，管理站名称不能为空。
     * 异常与副作用：会更新管理站表。
     */
    @SaCheckPermission("address:station:edit")
    @Log(title = "管理站", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    public R<Void> edit(@Validated @RequestBody StandardAddressManagementStationBo bo) {
        return toAjax(managementStationService.updateByBo(bo));
    }

    /**
     * 删除管理站。
     *
     * @param ids 主键集合
     * @return 操作结果
     *
     * 关键约束：主键集合不能为空。
     * 异常与副作用：会删除管理站记录。
     */
    @SaCheckPermission("address:station:remove")
    @Log(title = "管理站", businessType = BusinessType.DELETE)
    @PostMapping("/remove/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(managementStationService.deleteWithValidByIds(List.of(ids), true));
    }
}
