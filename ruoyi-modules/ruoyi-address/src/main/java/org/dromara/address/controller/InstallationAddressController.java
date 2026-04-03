package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.InstallationAddressBo;
import org.dromara.address.domain.vo.InstallationAddressVo;
import org.dromara.address.service.IInstallationAddressService;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 安装地址对外管理接口。
 * 目的：提供安装地址的查询与维护能力，支撑标准地址关联资源场景。
 * 副作用：新增/修改/删除会写入安装地址表。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/address/installation")
public class InstallationAddressController extends BaseController {

    private final IInstallationAddressService addressInstallationService;

    /**
     * 查询安装地址列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @SaCheckPermission("address:installation:list")
    @RequestMapping(value = "/list", method = {RequestMethod.GET, RequestMethod.POST})
    public TableDataInfo<InstallationAddressVo> list(InstallationAddressBo bo, PageQuery pageQuery) {
        return addressInstallationService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取安装地址详细信息。
     *
     * @param setAddrId 安装地址主键
     * @return 安装地址详情
     */
    @SaCheckPermission("address:installation:query")
    @RequestMapping(value = "/{setAddrId}", method = {RequestMethod.GET, RequestMethod.POST})
    public R<InstallationAddressVo> getInfo(@NotBlank(message = "主键不能为空") @PathVariable String setAddrId) {
        return R.ok(addressInstallationService.queryById(setAddrId));
    }

    /**
     * 新增安装地址。
     *
     * @param bo 新增参数
     * @return 操作结果
     */
    @SaCheckPermission("address:installation:add")
    @Log(title = "安装地址", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody InstallationAddressBo bo) {
        return toAjax(addressInstallationService.insertByBo(bo));
    }

    /**
     * 修改安装地址。
     *
     * @param bo 修改参数
     * @return 操作结果
     */
    @SaCheckPermission("address:installation:edit")
    @Log(title = "安装地址", businessType = BusinessType.UPDATE)
    @RequestMapping(value = "/update", method = {RequestMethod.PUT, RequestMethod.POST})
    public R<Void> edit(@Validated @RequestBody InstallationAddressBo bo) {
        return toAjax(addressInstallationService.updateByBo(bo));
    }

    /**
     * 删除安装地址。
     *
     * @param setAddrIds 主键串
     * @return 操作结果
     */
    @SaCheckPermission("address:installation:remove")
    @Log(title = "安装地址", businessType = BusinessType.DELETE)
    @RequestMapping(value = "/remove/{setAddrIds}", method = {RequestMethod.DELETE, RequestMethod.POST})
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable String[] setAddrIds) {
        return toAjax(addressInstallationService.deleteWithValidByIds(List.of(setAddrIds), true));
    }
}
