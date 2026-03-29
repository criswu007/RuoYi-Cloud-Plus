package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.StandardAddressBatchAddBo;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.bo.StandardAddressMergeBo;
import org.dromara.address.domain.bo.StandardAddressSplitBo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.service.IStandardAddressService;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.core.DefaultExcelListener;
import org.dromara.common.excel.core.ExcelResult;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 标准地址对外管理接口。
 * 目的：提供标准地址的查询、维护、合并/拆分、导入导出能力。
 * 关键约束：层级规则由服务层校验；删除存在子级或关联安装地址需要二次确认。
 * 副作用：新增/修改/删除/合并/拆分/导入会产生数据写入与日志记录。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/address/standard")
public class StandardAddressController extends StandardAddressAdminApiSupport {

    private final IStandardAddressService addressStandardService;

    /**
     * 查询标准地址列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     *
     * 关键约束：数据权限与租户过滤由底层拦截器处理。
     */
    @Override
    @SaCheckPermission("address:standard:list")
    @PostMapping("/list")
    public TableDataInfo<StandardAddressVo> listStandardAddresses(StandardAddressBo bo, PageQuery pageQuery) {
        return addressStandardService.queryStandardAddressPageList(bo, pageQuery);
    }

    /**
     * 获取标准地址详细信息。
     *
     * @param id 主键
     * @return 标准地址详情
     *
     * 异常：主键为空时触发参数校验错误。
     */
    @Override
    @SaCheckPermission("address:standard:query")
    @PostMapping("/{segmId}")
    public R<StandardAddressVo> getStandardAddressInfo(@PathVariable String segmId) {
        return R.ok(addressStandardService.getStandardAddressBySegmId(segmId));
    }

    /**
     * 新增标准地址。
     *
     * @param bo 新增参数
     * @return 操作结果
     *
     * 关键约束：层级规则、一级/二级地址限制由服务层校验。
     * 副作用：写入标准地址并生成完整地址名称。
     */
    @Override
    @SaCheckPermission("address:standard:add")
    @Log(title = "标准地址", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> addStandardAddress(@RequestBody StandardAddressBo bo) {
        return toAjax(addressStandardService.addStandardAddress(bo));
    }

    /**
     * 修改标准地址。
     *
     * @param bo 修改参数
     * @return 操作结果
     *
     * 关键约束：层级规则与一级/二级地址限制由服务层校验。
     * 副作用：必要时级联更新子级地址完整名称。
     */
    @Override
    @SaCheckPermission("address:standard:edit")
    @Log(title = "标准地址", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    public R<Void> editStandardAddress(@RequestBody StandardAddressBo bo) {
        return toAjax(addressStandardService.updateStandardAddress(bo));
    }

    /**
     * 删除标准地址。
     *
     * @param ids 主键串
     * @param confirm 是否确认删除（用于存在安装地址时的二次确认）
     * @return 操作结果
     *
     * 关键约束：存在子级地址禁止删除；有关联安装地址需二次确认。
     * 副作用：逻辑删除并写入操作日志。
     */
    @Override
    @SaCheckPermission("address:standard:remove")
    @Log(title = "标准地址", businessType = BusinessType.DELETE)
    @PostMapping("/remove/{segmIds}")
    public R<Void> removeStandardAddresses(@PathVariable String[] segmIds,
                                           @RequestParam(defaultValue = "false") boolean confirm) {
        return toAjax(addressStandardService.deleteStandardAddresses(Arrays.asList(segmIds), confirm));
    }

    /**
     * 合并地址。
     *
     * @param bo 参数集合，包含 sourceStandardAddressIds 与 targetStandardAddressId
     * @return 操作结果
     *
     * 关键约束：目标地址层级必须高于待合并地址。
     * 副作用：迁移子地址与安装地址，源地址被逻辑删除。
     */
    @Override
    @SaCheckPermission("address:standard:merge")
    @Log(title = "标准地址", businessType = BusinessType.UPDATE)
    @PostMapping("/merge")
    public R<Void> mergeStandardAddresses(@RequestBody StandardAddressMergeBo bo) {
        return toAjax(addressStandardService.mergeStandardAddresses(bo.getSourceStandardAddressIds(), bo.getTargetStandardAddressId()));
    }

    /**
     * 拆分地址。
     *
     * @param bo 参数集合，包含 sourceStandardAddressId 与 newAddresses
     * @return 操作结果
     *
     * 关键约束：继承源地址行政区划与层级信息。
     * 副作用：新增多条标准地址并逻辑删除源地址。
     */
    @Override
    @SaCheckPermission("address:standard:split")
    @Log(title = "标准地址", businessType = BusinessType.UPDATE)
    @PostMapping("/split")
    public R<Void> splitStandardAddress(@RequestBody StandardAddressSplitBo bo) {
        return toAjax(addressStandardService.splitStandardAddress(bo.getSourceStandardAddressId(), bo.getNewAddresses()));
    }

    /**
     * 预览批量新增子地址。
     *
     * @param bo 批量新增参数
     * @return 预览结果
     *
     * 关键约束：仅做规则预演，不落库。
     */
    @Override
    @SaCheckPermission("address:standard:add")
    @PostMapping("/batchPreviewChild")
    public R<List<StandardAddressAdminVo.BatchPreviewVo>> previewStandardAddressChildren(@RequestBody StandardAddressBatchAddBo bo) {
        return R.ok(addressStandardService.previewStandardAddressChildren(bo));
    }

    /**
     * 批量新增子地址。
     *
     * @param bo 批量新增参数
     * @return 操作结果
     *
     * 关键约束：父级层级不可超过最大层级；生成名称规则为前缀+序号+后缀。
     */
    @Override
    @SaCheckPermission("address:standard:add")
    @Log(title = "标准地址", businessType = BusinessType.INSERT)
    @PostMapping({"/batchAddChild", "/batchAddStandardAddressChildren"})
    public R<Void> batchAddStandardAddressChildren(@RequestBody StandardAddressBatchAddBo bo) {
        return toAjax(addressStandardService.batchAddStandardAddressChildren(bo));
    }

    /**
     * 导出标准地址列表。
     *
     * @param bo 查询条件
     * @param response 响应输出流
     *
     * 副作用：输出 Excel 文件。
     */
    @Override
    @SaCheckPermission("address:standard:export")
    @Log(title = "标准地址", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void exportStandardAddresses(StandardAddressBo bo, HttpServletResponse response) {
        List<StandardAddressVo> list = addressStandardService.queryStandardAddressList(bo);
        ExcelUtil.exportExcel(list, "标准地址", StandardAddressVo.class, response);
    }

    /**
     * 导入标准地址。
     *
     * @param file Excel 文件
     * @param updateSupport 是否支持更新（当前预留）
     * @return 导入结果
     * @throws Exception 导入解析异常
     *
     * 关键约束：一级/二级地址为系统预置数据，不允许通过导入变更。
     * 副作用：批量写入标准地址并记录导入日志。
     */
    @Override
    @SaCheckPermission("address:standard:import")
    @Log(title = "标准地址", businessType = BusinessType.IMPORT)
    @PostMapping({"/importData", "/importStandardAddressData"})
    public R<String> importStandardAddressData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelResult<StandardAddressImportVo> result = ExcelUtil.importExcel(file.getInputStream(), StandardAddressImportVo.class, new DefaultExcelListener<>());
        String msg = addressStandardService.importStandardAddressData(result.getList(), updateSupport, LoginHelper.getUsername(), file.getOriginalFilename());
        return R.ok(msg);
    }
}
