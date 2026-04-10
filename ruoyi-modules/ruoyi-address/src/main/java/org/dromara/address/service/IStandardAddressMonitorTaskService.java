package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressAdminBo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.domain.vo.StandardAddressMonitorTaskSummaryVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

/**
 * 非标地址监控任务服务接口。
 * 目的：提供监控任务摘要查询与手动触发能力，支撑原型中的监控任务页面。
 */
public interface IStandardAddressMonitorTaskService {

    /**
     * 查询监控任务摘要。
     *
     * @return 任务摘要
     *
     * 关键约束：摘要基于现有规则与异常记录实时统计。
     */
    StandardAddressMonitorTaskSummaryVo querySummary();

    /**
     * 立即执行一次非标地址监控。
     *
     * @return 提交结果，`1` 表示已成功提交后台执行，`0` 表示已有巡检在执行中
     *
     * 目的：为管理端提供全局手动巡检入口，同时避免请求线程长时间阻塞。
     * 关键约束：同一时刻只允许一个全局手动巡检执行。
     * 异常与副作用：成功后会异步触发监控扫描并新增异常记录。
     */
    Integer executeNow();

    /**
     * 查询监控任务分页列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     *
     * 目的：返回监控任务分页数据，支撑管理端列表展示。
     * 关键约束：当前阶段仅返回任务主表字段，范围和规则明细后续补充。
     * 异常与副作用：查询参数非法时由实现层抛出业务异常，无写入副作用。
     */
    TableDataInfo<StandardAddressAdminVo.MonitorTaskVo> queryPageList(StandardAddressAdminBo.MonitorTaskBo bo, PageQuery pageQuery);

    /**
     * 查询监控任务详情。
     *
     * @param id 任务ID
     * @return 任务详情
     *
     * 目的：查询单个监控任务详情，供编辑页和详情抽屉复用。
     * 关键约束：主键不能为空且必须命中当前可见任务。
     * 异常与副作用：任务不存在时由实现层返回空或抛出业务异常，无写入副作用。
     */
    StandardAddressAdminVo.MonitorTaskVo queryById(Long id);

    /**
     * 查询监控任务运行日志分页列表。
     *
     * @param taskId 任务ID
     * @param pageQuery 分页参数
     * @return 运行日志分页结果
     *
     * 目的：为任务管理页提供单任务最近运行记录查询能力。
     * 关键约束：必须按任务ID过滤，并按最近开始时间倒序返回。
     * 异常与副作用：无写入副作用。
     */
    TableDataInfo<StandardAddressAdminVo.MonitorTaskRunLogVo> queryRunLogPageList(Long taskId, PageQuery pageQuery);

    /**
     * 新增监控任务。
     *
     * @param bo 任务业务对象
     * @return 是否成功
     *
     * 目的：创建新的监控任务定义。
     * 关键约束：名称、执行规则与范围参数需在实现层校验。
     * 异常与副作用：成功后会写入任务主表。
     */
    Boolean insertByBo(StandardAddressAdminBo.MonitorTaskBo bo);

    /**
     * 修改监控任务。
     *
     * @param bo 任务业务对象
     * @return 是否成功
     *
     * 目的：更新已有监控任务定义。
     * 关键约束：修改主表时必须保留主键，范围与规则关联后续补充同步逻辑。
     * 异常与副作用：成功后会更新任务主表。
     */
    Boolean updateByBo(StandardAddressAdminBo.MonitorTaskBo bo);

    /**
     * 重跑监控任务。
     *
     * @param id 任务ID
     * @return 是否成功
     *
     * 目的：提供任务重跑入口，并复用统一执行器重新执行当前任务定义。
     * 关键约束：仅对存在的任务生效。
     * 异常与副作用：成功后会触发一次真实监控执行，并新增运行日志/异常记录。
     */
    Boolean rerunById(Long id);

    /**
     * 暂停监控任务。
     *
     * @param id 任务ID
     * @return 是否成功
     *
     * 目的：暂停任务后续调度。
     * 关键约束：仅更新状态，不强行终止已运行中的任务实例。
     * 异常与副作用：成功后会把任务状态改为 `PAUSED`。
     */
    Boolean pauseById(Long id);

    /**
     * 终止监控任务。
     *
     * @param id 任务ID
     * @return 是否成功
     *
     * 目的：终止任务并阻止后续继续复用该任务定义。
     * 关键约束：终止后状态不可自动恢复。
     * 异常与副作用：成功后会把任务状态改为 `TERMINATED`。
     */
    Boolean terminateById(Long id);
}
