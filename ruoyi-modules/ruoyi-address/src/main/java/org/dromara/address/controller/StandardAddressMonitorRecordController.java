package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.StandardAddressMonitorRecordBo;
import org.dromara.address.domain.vo.StandardAddressMonitorRecordVo;
import org.dromara.address.service.IStandardAddressMonitorRecordService;
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
 * 地址监控记录对外接口。
 * 目的：提供监控记录查询与处理能力。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/address/monitor/record")
public class StandardAddressMonitorRecordController extends BaseController {

    private final IStandardAddressMonitorRecordService monitorRecordService;

    /**
     * 查询监控记录列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @SaCheckPermission("address:monitor:record:list")
    @GetMapping("/list")
    public TableDataInfo<StandardAddressMonitorRecordVo> list(StandardAddressMonitorRecordBo bo, PageQuery pageQuery) {
        return monitorRecordService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取监控记录详情。
     *
     * @param id 主键ID
     * @return 监控记录详情
     */
    @SaCheckPermission("address:monitor:record:query")
    @GetMapping("/{id}")
    public R<StandardAddressMonitorRecordVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(monitorRecordService.queryById(id));
    }

    /**
     * 更新监控记录（处理/忽略）。
     *
     * @param bo 修改参数
     * @return 操作结果
     */
    @SaCheckPermission("address:monitor:record:edit")
    @Log(title = "地址监控记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody StandardAddressMonitorRecordBo bo) {
        return toAjax(monitorRecordService.updateByBo(bo));
    }

    /**
     * 删除监控记录。
     *
     * @param ids 主键集合
     * @return 操作结果
     */
    @SaCheckPermission("address:monitor:record:remove")
    @Log(title = "地址监控记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(monitorRecordService.deleteWithValidByIds(List.of(ids), true));
    }
}
