package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.StandardAddressImportRecordBo;
import org.dromara.address.domain.vo.StandardAddressImportBatchVo;
import org.dromara.address.domain.vo.StandardAddressImportRecordVo;
import org.dromara.address.service.IStandardAddressImportRecordService;
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
 * 地址导入记录对外接口。
 * 目的：提供导入记录的查询能力，便于审计与回溯。
 * 关键约束：仅查询，不提供修改与删除。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/address/import-record")
public class StandardAddressImportRecordController {

    private final IStandardAddressImportRecordService importRecordService;

    /**
     * 查询导入记录列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @SaCheckPermission("address:import:record:list")
    @PostMapping("/list")
    public TableDataInfo<StandardAddressImportRecordVo> list(StandardAddressImportRecordBo bo, PageQuery pageQuery) {
        return importRecordService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取导入批次详情。
     *
     * @param batchId 批次ID
     * @return 批次详情
     */
    @SaCheckPermission("address:import:record:query")
    @PostMapping("/batch/{batchId}")
    public R<StandardAddressImportBatchVo> getBatchInfo(@PathVariable Long batchId) {
        return R.ok(importRecordService.queryBatchById(batchId));
    }

    /**
     * 导出当前批次失败明细。
     *
     * @param batchId 批次ID
     * @param response 响应流
     * @throws Exception 写出异常
     */
    @SaCheckPermission("address:import:record:export")
    @PostMapping("/failure/export/{batchId}")
    public void exportFailureDetails(@PathVariable Long batchId, HttpServletResponse response) throws Exception {
        FileUtils.setAttachmentResponseHeader(response, ExcelUtil.encodingFilename("标准地址导入失败明细"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        ExcelUtil.exportExcel(importRecordService.listFailDetailsByBatchId(batchId),
            "标准地址导入失败明细",
            StandardAddressImportRecordVo.class,
            response);
    }
}
