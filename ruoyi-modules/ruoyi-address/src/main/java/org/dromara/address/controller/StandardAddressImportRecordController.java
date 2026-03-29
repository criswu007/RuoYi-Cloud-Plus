package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.StandardAddressImportRecordBo;
import org.dromara.address.domain.vo.StandardAddressImportRecordVo;
import org.dromara.address.service.IStandardAddressImportRecordService;
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
 * 地址导入记录对外接口。
 * 目的：提供导入记录的查询能力，便于审计与回溯。
 * 关键约束：仅查询，不提供修改与删除。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/address/import-record")
public class StandardAddressImportRecordController extends BaseController {

    private final IStandardAddressImportRecordService importRecordService;

    /**
     * 查询导入记录列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @SaCheckPermission("address:import:record:list")
    @GetMapping("/list")
    public TableDataInfo<StandardAddressImportRecordVo> list(StandardAddressImportRecordBo bo, PageQuery pageQuery) {
        return importRecordService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取导入记录详情。
     *
     * @param id 主键ID
     * @return 导入记录详情
     */
    @SaCheckPermission("address:import:record:query")
    @GetMapping("/{id}")
    public R<StandardAddressImportRecordVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(importRecordService.queryById(id));
    }
}
