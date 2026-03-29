package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.StandardAddressTagBindBo;
import org.dromara.address.domain.bo.StandardAddressTagBo;
import org.dromara.address.domain.vo.StandardAddressTagVo;
import org.dromara.address.service.IStandardAddressTagService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地址标签对外接口。
 * 目的：提供标签管理与地址打标能力，支撑原型中的标签管理与批量打标页面。
 * 关键约束：标签名称唯一；绑定/解绑需显式指定地址集合与标签集合。
 * 副作用：新增、修改、删除、绑定、解绑会写入标签表或关系表。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/address/tag")
public class StandardAddressTagController extends BaseController {

    private final IStandardAddressTagService addressTagService;

    /**
     * 查询标签列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @SaCheckPermission("address:tag:list")
    @GetMapping("/list")
    public TableDataInfo<StandardAddressTagVo> list(StandardAddressTagBo bo, PageQuery pageQuery) {
        return addressTagService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询标签明细。
     *
     * @param id 标签ID
     * @return 标签详情
     *
     * 异常：主键为空时触发参数校验错误。
     */
    @SaCheckPermission("address:tag:query")
    @GetMapping("/{id}")
    public R<StandardAddressTagVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(addressTagService.queryById(id));
    }

    /**
     * 新增标签。
     *
     * @param bo 新增参数
     * @return 操作结果
     *
     * 关键约束：标签名称唯一。
     * 异常：标签名称重复时抛出业务异常。
     * 副作用：写入标签表。
     */
    @SaCheckPermission("address:tag:add")
    @Log(title = "地址标签", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody StandardAddressTagBo bo) {
        return toAjax(addressTagService.insertByBo(bo));
    }

    /**
     * 修改标签。
     *
     * @param bo 修改参数
     * @return 操作结果
     *
     * 关键约束：标签名称唯一。
     * 副作用：更新标签表。
     */
    @SaCheckPermission("address:tag:edit")
    @Log(title = "地址标签", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody StandardAddressTagBo bo) {
        return toAjax(addressTagService.updateByBo(bo));
    }

    /**
     * 删除标签。
     *
     * @param ids 标签ID集合
     * @return 操作结果
     *
     * 副作用：逻辑删除标签并清理地址标签关系。
     */
    @SaCheckPermission("address:tag:remove")
    @Log(title = "地址标签", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(addressTagService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 查询指定标准地址的标签列表。
     *
     * @param standardAddressId 标准地址ID
     * @return 标签列表
     */
    @SaCheckPermission("address:tag:query")
    @GetMapping("/standard-address/{standardAddressId}")
    public R<List<StandardAddressTagVo>> listTagsByStandardAddressId(@NotNull(message = "标准地址不能为空") @PathVariable Long standardAddressId) {
        return R.ok(addressTagService.listTagsByStandardAddressId(standardAddressId));
    }

    /**
     * 批量绑定标签。
     *
     * @param bo 绑定参数
     * @return 操作结果
     *
     * 关键约束：重复关系自动忽略。
     * 异常：地址或标签不存在时抛出业务异常。
     * 副作用：写入地址标签关联表。
     */
    @SaCheckPermission("address:tag:bind")
    @Log(title = "地址标签绑定", businessType = BusinessType.UPDATE)
    @PostMapping("/bind")
    public R<Void> bindTagsToStandardAddresses(@Validated @RequestBody StandardAddressTagBindBo bo) {
        return toAjax(addressTagService.bindTagsToStandardAddresses(bo.getStandardAddressIds(), bo.getTagIds()));
    }

    /**
     * 批量解绑标签。
     *
     * @param bo 解绑参数
     * @return 操作结果
     *
     * 副作用：删除地址标签关联表中命中的关系。
     */
    @SaCheckPermission("address:tag:bind")
    @Log(title = "地址标签解绑", businessType = BusinessType.UPDATE)
    @PostMapping("/unbind")
    public R<Void> unbindTagsFromStandardAddresses(@Validated @RequestBody StandardAddressTagBindBo bo) {
        return toAjax(addressTagService.unbindTagsFromStandardAddresses(bo.getStandardAddressIds(), bo.getTagIds()));
    }
}
