package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.StandardAddressAttributeBo;
import org.dromara.address.domain.vo.StandardAddressAttributeVo;
import org.dromara.address.service.IStandardAddressAttributeService;
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
 * 地址属性扩展对外接口。
 * 目的：提供标准地址属性扩展的增删改查能力。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/address/attribute")
public class StandardAddressAttributeController extends BaseController {

    private final IStandardAddressAttributeService addressAttributeService;

    /**
     * 查询地址属性扩展列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @SaCheckPermission("address:attribute:list")
    @GetMapping("/list")
    public TableDataInfo<StandardAddressAttributeVo> list(StandardAddressAttributeBo bo, PageQuery pageQuery) {
        return addressAttributeService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取地址属性扩展详细信息。
     *
     * @param id 主键
     * @return 详情
     */
    @SaCheckPermission("address:attribute:query")
    @GetMapping("/{id}")
    public R<StandardAddressAttributeVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(addressAttributeService.queryById(id));
    }

    /**
     * 新增地址属性扩展。
     *
     * @param bo 新增参数
     * @return 操作结果
     */
    @SaCheckPermission("address:attribute:add")
    @Log(title = "地址属性扩展", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody StandardAddressAttributeBo bo) {
        return toAjax(addressAttributeService.insertByBo(bo));
    }

    /**
     * 修改地址属性扩展。
     *
     * @param bo 修改参数
     * @return 操作结果
     */
    @SaCheckPermission("address:attribute:edit")
    @Log(title = "地址属性扩展", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody StandardAddressAttributeBo bo) {
        return toAjax(addressAttributeService.updateByBo(bo));
    }

    /**
     * 删除地址属性扩展。
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("address:attribute:remove")
    @Log(title = "地址属性扩展", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(addressAttributeService.deleteWithValidByIds(List.of(ids), true));
    }
}
