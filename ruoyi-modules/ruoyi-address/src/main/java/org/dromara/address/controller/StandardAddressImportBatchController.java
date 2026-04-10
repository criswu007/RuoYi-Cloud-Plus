package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.StandardAddressImportDetailBo;
import org.dromara.address.domain.vo.StandardAddressImportBatchVo;
import org.dromara.address.domain.vo.StandardAddressImportDetailVo;
import org.dromara.address.service.IStandardAddressImportBatchService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标准地址导入批次对外接口。
 * 目的：提供导入批次摘要、导入明细分页和失败明细导出能力，支撑审计回溯与二次修复。
 * 入参/出参：入参包含导入明细筛选条件、分页参数和批次主键；出参包含导入明细分页、批次摘要和失败明细文件流。
 * 关键约束：当前接口只读，不提供导入结果修改与删除；接口路径统一采用多级路径，禁止使用中划线。
 * 异常与副作用：查询条件非法时返回业务异常；失败明细导出会向响应流写出 Excel 文件，其余接口无写入副作用。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/address/import/batch")
public class StandardAddressImportBatchController {

    private final IStandardAddressImportBatchService importBatchService;

    /**
     * 分页查询导入明细。
     * 目的：按导入文件、地址名称、状态和操作人等条件分页查询导入明细，支撑失败定位和批次回溯。
     * 入参：`bo` 为导入明细筛选条件，`pageQuery` 为分页参数。
     * 出参：返回导入明细分页结果，列表粒度为单条导入行结果。
     * 关键约束：页面主视角是导入明细，不是批次主表；分页必须依赖数据库真实分页。
     * 异常与副作用：查询条件非法时返回业务异常，无写入副作用。
     */
    @SaCheckPermission("address:import:record:list")
    @PostMapping("/list")
    public TableDataInfo<StandardAddressImportDetailVo> list(StandardAddressImportDetailBo bo, PageQuery pageQuery) {
        return importBatchService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询导入批次摘要。
     * 目的：根据批次主键返回批次号、文件名、汇总状态和统计结果，供详情弹窗和失败导出入口复用。
     * 入参：`batchId` 为导入批次主键。
     * 出参：返回导入批次摘要信息。
     * 关键约束：批次状态以导入明细聚合结果为准，不单独维护另一套状态真相源。
     * 异常与副作用：批次不存在时返回业务异常，无写入副作用。
     */
    @SaCheckPermission("address:import:record:query")
    @PostMapping("/{batchId}")
    public R<StandardAddressImportBatchVo> getBatchInfo(@PathVariable Long batchId) {
        return R.ok(importBatchService.queryBatchById(batchId));
    }

    /**
     * 导出当前批次失败明细。
     * 目的：将指定批次下失败状态的导入明细导出为 Excel，便于修正后重新导入。
     * 入参：`batchId` 为导入批次主键，`response` 为下载响应流。
     * 出参：无显式返回值，结果通过响应流写出 Excel 文件。
     * 关键约束：只导出失败状态的导入明细，不得混入成功或待审批数据。
     * 异常与副作用：写出文件失败时抛出异常；会向响应流写入二进制内容。
     *
     * @throws Exception 写出异常
     */
    @SaCheckPermission("address:import:record:export")
    @PostMapping("/failure/export/{batchId}")
    public void exportFailureDetails(@PathVariable Long batchId, HttpServletResponse response) throws Exception {
        FileUtils.setAttachmentResponseHeader(response, ExcelUtil.encodingFilename("标准地址导入失败明细"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        ExcelUtil.exportExcel(importBatchService.listFailDetailsByBatchId(batchId),
            "标准地址导入失败明细",
            StandardAddressImportDetailVo.class,
            response);
    }
}
