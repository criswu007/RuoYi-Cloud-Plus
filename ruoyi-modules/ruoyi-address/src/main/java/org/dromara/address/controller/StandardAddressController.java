package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.StandardAddressAdminBo;
import org.dromara.address.domain.bo.StandardAddressBatchAddBo;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.bo.StandardAddressMergeBo;
import org.dromara.address.domain.bo.StandardAddressSplitBo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.domain.vo.StandardAddressImportResultVo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.search.service.StandardAddressSearchExportService;
import org.dromara.address.service.IStandardAddressService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.excel.core.DefaultExcelListener;
import org.dromara.common.excel.core.ExcelResult;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.excel.utils.ExcelWriterWrapper;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    private static final int EXPORT_BATCH_SIZE = 500;

    private final IStandardAddressService addressStandardService;
    private final StandardAddressSearchExportService standardAddressSearchExportService;

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
     * 查询标准地址级别选项。
     *
     * @return 标准地址级别字典
     *
     * 关键约束：选项必须以线上 `segm_addr_type` 的真实层级定义为准。
     * 异常与副作用：无写入副作用。
     */
    @Override
    @SaCheckPermission("address:standard:list")
    @PostMapping("/levelOptions")
    public R<List<StandardAddressAdminVo.LevelOptionVo>> listStandardAddressLevelOptions() {
        return R.ok(addressStandardService.listStandardAddressLevelOptions());
    }

    /**
     * 查询标准地址编辑页聚合字典。
     *
     * @return 编辑页聚合字典
     *
     * 关键约束：状态、接入方式、接入能力、城乡属性、房屋属性等选项必须直接来源于线上 `pub_restriction`。
     * 异常与副作用：无写入副作用。
     */
    @Override
    @SaCheckPermission("address:standard:list")
    @PostMapping("/formOptions")
    public R<StandardAddressAdminVo.FormOptionsVo> listStandardAddressFormOptions() {
        return R.ok(addressStandardService.listStandardAddressFormOptions());
    }

    /**
     * 查询标准地址编辑页管理站候选。
     *
     * @param bo 管理站候选查询条件
     * @return 管理站候选列表
     *
     * 关键约束：必须显式按 `manageType` 区分维修、安装、营业管理站，并优先按 `regionId` 收敛结果。
     * 异常与副作用：无写入副作用。
     */
    @Override
    @SaCheckPermission("address:standard:list")
    @PostMapping("/stationOptions")
    public R<List<StandardAddressAdminVo.StationOptionVo>> listStandardAddressStationOptions(@RequestBody @Validated StandardAddressAdminBo.StationOptionQueryBo bo) {
        return R.ok(addressStandardService.listStandardAddressStationOptions(bo));
    }

    /**
     * 获取标准地址详细信息。
     *
     * @param segmId 主键
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
        return toAjax(addressStandardService.mergeStandardAddresses(bo.getSourceSegmIds(), bo.getTargetSegmId()));
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
        return toAjax(addressStandardService.splitStandardAddress(bo.getSourceSegmId(), bo.getSplitItems()));
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
     * 关键约束：导出必须按分页分批拉取标准地址数据，避免超大数据量下全量加载造成内存放大。
     * 异常与副作用：输出 Excel 文件；写出流异常时抛出运行时异常。
     */
    @Override
    @SaCheckPermission("address:standard:export")
    @Log(title = "标准地址", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void exportStandardAddresses(StandardAddressBo bo, HttpServletResponse response) {
        try {
            FileUtils.setAttachmentResponseHeader(response, ExcelUtil.encodingFilename("标准地址"));
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
            ExcelUtil.exportExcel(StandardAddressVo.class, response.getOutputStream(), writer -> writeStandardAddressExportRows(bo, writer));
        } catch (Exception e) {
            if (!response.isCommitted()) {
                response.reset();
            }
            throw new RuntimeException("导出标准地址异常", e);
        }
    }

    /**
     * 目的：下载标准地址导入模板。
     * 入参：响应输出流。
     * 出参：无，直接向响应流写出模板文件。
     * 关键约束：模板字段口径必须与当前导入接口合同保持一致。
     * 异常与副作用：会写出 Excel 文件流，不产生数据库写入副作用。
     */
    @Override
    @SaCheckPermission("address:standard:import")
    @PostMapping("/import/template")
    public void downloadStandardAddressImportTemplate(HttpServletResponse response) throws Exception {
        FileUtils.setAttachmentResponseHeader(response, ExcelUtil.encodingFilename("标准地址导入模板"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        ExcelUtil.exportExcel(List.of(), "标准地址导入模板", StandardAddressImportVo.class, response.getOutputStream());
    }

    /**
     * 目的：按当前导出口径流式写出标准地址数据。
     * 入参：标准地址查询条件与 Excel 写出包装器。
     * 出参：无，直接把查询结果逐批写入工作表。
     * 关键约束：区域级标准地址或 ES 读链路关闭时回退数据库分页；`ADDR_SEGM` 导出走 `PIT + search_after` 批次查询，禁止一次性全量加载。
     * 异常与副作用：会持续写入响应输出流，不产生数据库写入副作用。
     */
    private void writeStandardAddressExportRows(StandardAddressBo bo, ExcelWriterWrapper<StandardAddressVo> writer) {
        var writeSheet = ExcelWriterWrapper.buildSheet("标准地址");
        standardAddressSearchExportService.writeRows(bo, EXPORT_BATCH_SIZE, rows -> writer.write(rows, writeSheet));
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
    @PostMapping("/import")
    public R<StandardAddressImportResultVo> importStandardAddressData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelResult<StandardAddressImportVo> result = ExcelUtil.importExcel(file.getInputStream(), StandardAddressImportVo.class, new DefaultExcelListener<>());
        StandardAddressImportResultVo summary = addressStandardService.importStandardAddressData(result.getList(), updateSupport, LoginHelper.getUsername(), file.getOriginalFilename());
        return R.ok(summary);
    }
}
