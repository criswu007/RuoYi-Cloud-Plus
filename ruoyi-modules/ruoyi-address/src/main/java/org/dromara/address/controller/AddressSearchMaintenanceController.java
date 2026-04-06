package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.AddressSearchMaintenanceBo;
import org.dromara.address.domain.vo.AddressSearchMaintenanceVo;
import org.dromara.address.search.service.AddressSearchMaintenanceTaskService;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 地址搜索维护接口。
 * 目的：提供 ES 全量重建与 repair 重放入口，供受控运维场景执行索引修复。
 * 入参/出参：输入重建类型或 repair 任务主键，输出统一响应结果。
 * 关键约束：仅承载运维入口编排，不直接暴露底层索引细节；实际一致性语义与 repair 口径由维护服务统一保证。
 * 异常与副作用：会触发索引重建、别名切换或 repair 执行，并更新 repair 任务状态。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/address/search")
public class AddressSearchMaintenanceController extends BaseController {

    private final AddressSearchMaintenanceTaskService addressSearchMaintenanceTaskService;

    /**
     * 目的：查询 ES 运维台概览信息。
     * 入参：无。
     * 出参：运维总览视图。
     * 关键约束：仅做状态聚合查询，不触发写操作。
     * 异常与副作用：会读取 ES 运行态、任务表和 repair 表。
     *
     * @return 运维总览
     */
    @SaCheckPermission("address:search:maintain")
    @GetMapping("/ops/overview")
    public R<AddressSearchMaintenanceVo.OverviewVo> getOverview() {
        return R.ok(addressSearchMaintenanceTaskService.getOverview());
    }

    /**
     * 目的：提交标准地址全量重建任务。
     * 入参：标准地址重建确认口令。
     * 出参：新建任务主键。
     * 关键约束：口令必须匹配标准地址重建约定值；实际执行为异步任务。
     * 异常与副作用：会申请写锁、写入任务表并异步触发标准地址索引重建。
     *
     * @param bo 重建提交参数
     * @return 运维任务主键
     */
    @SaCheckPermission("address:search:maintain")
    @PostMapping("/tasks/rebuild/standard")
    public R<Long> createStandardRebuildTask(@Validated @RequestBody AddressSearchMaintenanceBo.RebuildTaskSubmitBo bo) {
        return R.ok(addressSearchMaintenanceTaskService.submitStandardRebuild(bo.getConfirmationCode()));
    }

    /**
     * 目的：提交安装地址全量重建任务。
     * 入参：安装地址重建确认口令。
     * 出参：新建任务主键。
     * 关键约束：口令必须匹配安装地址重建约定值；实际执行为异步任务。
     * 异常与副作用：会申请写锁、写入任务表并异步触发安装地址索引重建。
     *
     * @param bo 重建提交参数
     * @return 运维任务主键
     */
    @SaCheckPermission("address:search:maintain")
    @PostMapping("/tasks/rebuild/installation")
    public R<Long> createInstallationRebuildTask(@Validated @RequestBody AddressSearchMaintenanceBo.RebuildTaskSubmitBo bo) {
        return R.ok(addressSearchMaintenanceTaskService.submitInstallationRebuild(bo.getConfirmationCode()));
    }

    /**
     * 目的：分页查询运维任务记录。
     * 入参：任务筛选条件与分页参数。
     * 出参：任务分页结果。
     * 关键约束：仅按任务类型和状态过滤，排序口径固定为创建时间倒序。
     * 异常与副作用：会读取任务表，无写操作。
     *
     * @param bo 任务查询条件
     * @param pageQuery 分页参数
     * @return 运维任务分页结果
     */
    @SaCheckPermission("address:search:maintain")
    @GetMapping("/tasks")
    public TableDataInfo<AddressSearchMaintenanceVo.TaskVo> listTasks(AddressSearchMaintenanceBo.TaskQueryBo bo, PageQuery pageQuery) {
        return addressSearchMaintenanceTaskService.listTasks(bo, pageQuery);
    }

    /**
     * 目的：查询单个运维任务详情。
     * 入参：运维任务主键。
     * 出参：任务详情视图。
     * 关键约束：仅按主键查询，不做额外聚合。
     * 异常与副作用：会读取任务表，无写操作。
     *
     * @param taskId 运维任务主键
     * @return 运维任务详情
     */
    @SaCheckPermission("address:search:maintain")
    @GetMapping("/tasks/{taskId}")
    public R<AddressSearchMaintenanceVo.TaskVo> getTask(@PathVariable Long taskId) {
        return R.ok(addressSearchMaintenanceTaskService.getTask(taskId));
    }

    /**
     * 目的：分页查询 repair 任务列表。
     * 入参：repair 筛选条件与分页参数。
     * 出参：repair 任务分页结果。
     * 关键约束：仅按实体类型和状态过滤，排序口径固定为更新时间倒序。
     * 异常与副作用：会读取 repair 表，无写操作。
     *
     * @param bo repair 查询条件
     * @param pageQuery 分页参数
     * @return repair 任务分页结果
     */
    @SaCheckPermission("address:search:maintain")
    @GetMapping("/repair/tasks")
    public TableDataInfo<AddressSearchMaintenanceVo.RepairTaskVo> listRepairTasks(AddressSearchMaintenanceBo.RepairTaskQueryBo bo, PageQuery pageQuery) {
        return addressSearchMaintenanceTaskService.listRepairTasks(bo, pageQuery);
    }

    /**
     * 目的：执行单条地址搜索 repair 任务。
     * 入参：repair 任务主键与执行确认口令。
     * 出参：统一成功响应。
     * 关键约束：repair 与重建共享串行写锁，控制器层不直接解析 repair payload。
     * 异常与副作用：会申请写锁、触发 repair 执行，并回写 repair 任务状态与重试次数。
     *
     * @param taskId repair 任务主键
     * @param bo repair 执行参数
     * @return 操作结果
     */
    @SaCheckPermission("address:search:maintain")
    @PostMapping("/repair/{taskId}/execute")
    public R<Void> executeRepairTask(@PathVariable Long taskId,
                                     @Validated @RequestBody AddressSearchMaintenanceBo.RepairExecuteBo bo) {
        addressSearchMaintenanceTaskService.executeRepairTask(taskId, bo.getConfirmationCode());
        return R.ok();
    }
}
