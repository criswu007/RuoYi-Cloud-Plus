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
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/list")
    public TableDataInfo<StandardAddressMonitorRecordVo> listMonitorRecords(StandardAddressMonitorRecordBo bo, PageQuery pageQuery) {
        return monitorRecordService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取监控记录详情。
     *
     * @param id 主键ID
     * @return 监控记录详情
     */
    @SaCheckPermission("address:monitor:record:query")
    @PostMapping("/{id}")
    public R<StandardAddressMonitorRecordVo> getMonitorRecordInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
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
    @PostMapping("/update")
    public R<Void> editMonitorRecord(@Validated @RequestBody StandardAddressMonitorRecordBo bo) {
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
    @PostMapping("/remove/{ids}")
    public R<Void> removeMonitorRecord(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(monitorRecordService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 批量忽略监控记录。
     *
     * @param ids 记录主键集合
     * @return 操作结果
     *
     * 目的：提供异常预警页批量忽略入口。
     * 入参/出参：入参为异常记录主键数组，出参为统一操作结果。
     * 关键约束：重复忽略需保持幂等，仅更新状态与处理时间。
     * 异常与副作用：成功后会批量更新异常记录状态。
     */
    @SaCheckPermission("address:monitor:record:edit")
    @Log(title = "地址监控记录", businessType = BusinessType.UPDATE)
    @PostMapping("/ignore/{ids}")
    public R<Void> ignoreMonitorRecord(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(monitorRecordService.ignoreByIds(List.of(ids)));
    }
}
