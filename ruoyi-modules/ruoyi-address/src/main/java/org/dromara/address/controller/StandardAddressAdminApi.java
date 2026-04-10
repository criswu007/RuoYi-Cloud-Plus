package org.dromara.address.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.dromara.address.domain.bo.InstallationAddressBo;
import org.dromara.address.domain.bo.StandardAddressAdminBo;
import org.dromara.address.domain.bo.StandardAddressBatchAddBo;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.bo.StandardAddressImportDetailBo;
import org.dromara.address.domain.bo.StandardAddressMergeBo;
import org.dromara.address.domain.bo.StandardAddressMonitorRecordBo;
import org.dromara.address.domain.bo.StandardAddressMonitorRuleBo;
import org.dromara.address.domain.bo.StandardAddressOperationLogBo;
import org.dromara.address.domain.bo.StandardAddressSelectionRoomBo;
import org.dromara.address.domain.bo.StandardAddressSplitBo;
import org.dromara.address.domain.bo.StandardAddressTagBindBo;
import org.dromara.address.domain.bo.StandardAddressTagBo;
import org.dromara.address.domain.vo.InstallationAddressVo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.domain.vo.StandardAddressImportResultVo;
import org.dromara.address.domain.vo.StandardAddressImportDetailVo;
import org.dromara.address.domain.vo.StandardAddressMonitorRecordVo;
import org.dromara.address.domain.vo.StandardAddressMonitorRuleVo;
import org.dromara.address.domain.vo.StandardAddressMonitorTaskSummaryVo;
import org.dromara.address.domain.vo.StandardAddressOperationLogVo;
import org.dromara.address.domain.vo.StandardAddressSelectionResultVo;
import org.dromara.address.domain.vo.StandardAddressTagVo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 标准地址模块接口基线。
 * 目的：在现有 `controller / domain/bo / domain/vo` 分层下固化标准地址模块后续开发边界。
 * 入参/出参：统一使用仓库已有 `R`、`TableDataInfo`、`PageQuery` 与模块内 BO/VO。
 * 关键约束：该接口只定义合同，不承诺当前已经具备运行时实现；后续开发与调整均以本接口和接口清单文档为准。
 * 异常与副作用：业务异常、状态流转和事务边界由后续实现层负责，本接口本身无运行时副作用。
 */
@Validated
public interface StandardAddressAdminApi {

    /**
     * 目的：分页查询标准地址主资源。
     * 入参：标准地址查询条件与分页参数。
     * 出参：标准地址分页列表。
     * 关键约束：保持与现有 `StandardAddressController` 列表风格一致。
     * 异常与副作用：筛选条件非法时由实现层抛出业务异常，无写入副作用。
     */
    @PostMapping("/address/standard/list")
    TableDataInfo<StandardAddressVo> listStandardAddresses(StandardAddressBo bo, PageQuery pageQuery);

    /**
     * 目的：查询标准地址级别下拉选项。
     * 入参：无。
     * 出参：标准地址级别字典集合。
     * 关键约束：选项必须直接来源于线上 `segm_addr_type` 的 19 级数据。
     * 异常与副作用：无写入副作用。
     */
    @PostMapping("/address/standard/levelOptions")
    R<List<StandardAddressAdminVo.LevelOptionVo>> listStandardAddressLevelOptions();

    /**
     * 目的：查询标准地址编辑页聚合字典。
     * 入参：无。
     * 出参：状态、接入方式、接入能力、城乡属性、房屋属性等字典分组。
     * 关键约束：字典值必须直接来源于线上 `pub_restriction`，返回 key 与表单字段保持一致。
     * 异常与副作用：无写入副作用。
     */
    @PostMapping("/address/standard/formOptions")
    R<StandardAddressAdminVo.FormOptionsVo> listStandardAddressFormOptions();

    /**
     * 目的：查询标准地址编辑页管理站候选。
     * 入参：区域、管理站类型、搜索关键字与返回上限。
     * 出参：管理站候选集合。
     * 关键约束：必须显式按 `manageType` 区分维修/安装/营业管理站。
     * 异常与副作用：无写入副作用。
     */
    @PostMapping("/address/standard/stationOptions")
    R<List<StandardAddressAdminVo.StationOptionVo>> listStandardAddressStationOptions(
        @Valid @RequestBody StandardAddressAdminBo.StationOptionQueryBo bo);

    /**
     * 目的：查询标准地址详情。
     * 入参：标准地址主键。
     * 出参：标准地址详情。
     * 关键约束：主键不能为空且需命中可见数据范围。
     * 异常与副作用：主键不存在时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/standard/{segmId}")
    R<StandardAddressVo> getStandardAddressInfo(@NotBlank(message = "标准地址ID不能为空") @PathVariable String segmId);

    /**
     * 目的：新增标准地址。
     * 入参：标准地址业务对象。
     * 出参：统一操作结果。
     * 关键约束：层级规则、重名校验和一二级地址限制由实现层负责。
     * 异常与副作用：成功后会新增地址数据并记录操作日志。
     */
    @PostMapping("/address/standard")
    R<Void> addStandardAddress(@Valid @RequestBody StandardAddressBo bo);

    /**
     * 目的：修改标准地址。
     * 入参：标准地址业务对象。
     * 出参：统一操作结果。
     * 关键约束：涉及父级或名称变更时需要级联刷新完整名称。
     * 异常与副作用：成功后会更新地址数据并可能级联更新子级名称。
     */
    @PostMapping("/address/standard/update")
    R<Void> editStandardAddress(@Valid @RequestBody StandardAddressBo bo);

    /**
     * 目的：删除标准地址。
     * 入参：标准地址主键集合与是否确认删除安装地址关联。
     * 出参：统一操作结果。
     * 关键约束：存在子级地址或安装地址关联时需按业务规则校验。
     * 异常与副作用：成功后会删除地址数据并写入日志。
     */
    @PostMapping("/address/standard/remove/{segmIds}")
    R<Void> removeStandardAddresses(@NotEmpty(message = "标准地址ID不能为空") @PathVariable String[] segmIds,
                                    @RequestParam(defaultValue = "false") boolean confirm);

    /**
     * 目的：合并标准地址。
     * 入参：源地址集合与目标地址。
     * 出参：统一操作结果。
     * 关键约束：合并过程需要保证子节点与安装地址迁移的一致性。
     * 异常与副作用：成功后会迁移关联数据并回收源地址。
     */
    @PostMapping("/address/standard/merge")
    R<Void> mergeStandardAddresses(@Valid @RequestBody StandardAddressMergeBo bo);

    /**
     * 目的：拆分标准地址。
     * 入参：源地址与拆分项集合。
     * 出参：统一操作结果。
     * 关键约束：至少一条拆分项，且父子层级必须合法。
     * 异常与副作用：成功后会新增新地址并回收源地址。
     */
    @PostMapping("/address/standard/split")
    R<Void> splitStandardAddress(@Valid @RequestBody StandardAddressSplitBo bo);

    /**
     * 目的：预览批量新增下级地址结果。
     * 入参：批量新增参数。
     * 出参：批量预览结果列表。
     * 关键约束：该接口只做预演，不落库。
     * 异常与副作用：区间非法或父级不存在时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/standard/batchPreviewChild")
    R<List<StandardAddressAdminVo.BatchPreviewVo>> previewStandardAddressChildren(@Valid @RequestBody StandardAddressBatchAddBo bo);

    /**
     * 目的：批量新增下级标准地址。
     * 入参：批量新增参数。
     * 出参：统一操作结果。
     * 关键约束：编号区间和层级上限由实现层控制。
     * 异常与副作用：成功后会批量写入标准地址。
     */
    @PostMapping({"/address/standard/batchAddChild", "/address/standard/batchAddStandardAddressChildren"})
    R<Void> batchAddStandardAddressChildren(@Valid @RequestBody StandardAddressBatchAddBo bo);

    /**
     * 目的：导出标准地址列表。
     * 入参：标准地址筛选条件与响应流。
     * 出参：Excel 文件流。
     * 关键约束：导出字段口径需与列表和接口清单保持一致。
     * 异常与副作用：成功后会向响应流写出文件内容。
     */
    @PostMapping("/address/standard/export")
    void exportStandardAddresses(StandardAddressBo bo, HttpServletResponse response);

    /**
     * 目的：下载标准地址导入模板。
     * 入参：响应流。
     * 出参：Excel 模板文件流。
     * 关键约束：模板需与线上字段口径和当前导入合同保持一致。
     * 异常与副作用：成功后会向响应流写出模板内容。
     */
    @PostMapping("/address/standard/import/template")
    void downloadStandardAddressImportTemplate(HttpServletResponse response) throws Exception;

    /**
     * 目的：导入标准地址数据。
     * 入参：导入文件与是否允许更新。
     * 出参：导入结果描述。
     * 关键约束：导入模板、失败明细记录与失败导出规则由实现层统一保证，本轮不提供成功数据回滚。
     * 异常与副作用：成功后会写入地址数据、导入记录和操作日志。
     */
    @PostMapping("/address/standard/import")
    R<StandardAddressImportResultVo> importStandardAddressData(@RequestPart("file") MultipartFile file,
                                                               @RequestParam(defaultValue = "false") boolean updateSupport) throws Exception;

    /**
     * 目的：分页查询标签。
     * 入参：标签查询条件与分页参数。
     * 出参：标签分页列表。
     * 关键约束：标签名称唯一性由实现层保证。
     * 异常与副作用：查询条件非法时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/tag/list")
    TableDataInfo<StandardAddressTagVo> listTags(StandardAddressTagBo bo, PageQuery pageQuery);

    /**
     * 目的：查询标签详情。
     * 入参：标签主键。
     * 出参：标签详情。
     * 关键约束：主键不能为空。
     * 异常与副作用：标签不存在时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/tag/{id}")
    R<StandardAddressTagVo> getTagInfo(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：新增标签。
     * 入参：标签业务对象。
     * 出参：统一操作结果。
     * 关键约束：标签名称唯一。
     * 异常与副作用：成功后会新增标签主数据。
     */
    @PostMapping("/address/tag")
    R<Void> addTag(@Valid @RequestBody StandardAddressTagBo bo);

    /**
     * 目的：修改标签。
     * 入参：标签业务对象。
     * 出参：统一操作结果。
     * 关键约束：修改后名称仍需满足唯一性。
     * 异常与副作用：成功后会更新标签数据。
     */
    @PostMapping("/address/tag/update")
    R<Void> editTag(@Valid @RequestBody StandardAddressTagBo bo);

    /**
     * 目的：删除标签。
     * 入参：标签主键集合。
     * 出参：统一操作结果。
     * 关键约束：需要同步清理标签关联关系。
     * 异常与副作用：成功后会删除标签和关联关系。
     */
    @PostMapping("/address/tag/remove/{ids}")
    R<Void> removeTag(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids);

    /**
     * 目的：查询指定标准地址已绑定标签。
     * 入参：标准地址主键。
     * 出参：标签列表。
     * 关键约束：仅返回当前有效标签。
     * 异常与副作用：地址不存在时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/tag/standardAddress/{standardAddressId}")
    R<List<StandardAddressTagVo>> listTagsByStandardAddressId(@NotBlank(message = "标准地址不能为空") @PathVariable String standardAddressId);

    /**
     * 目的：批量给标准地址绑定标签。
     * 入参：地址与标签绑定对象。
     * 出参：统一操作结果。
     * 关键约束：重复提交需幂等。
     * 异常与副作用：成功后会新增地址标签关联。
     */
    @PostMapping("/address/tag/bind")
    R<Void> bindTagsToStandardAddresses(@Valid @RequestBody StandardAddressTagBindBo bo);

    /**
     * 目的：批量移除标准地址标签。
     * 入参：地址与标签绑定对象。
     * 出参：统一操作结果。
     * 关键约束：重复解绑需幂等。
     * 异常与副作用：成功后会删除地址标签关联。
     */
    @PostMapping("/address/tag/unbind")
    R<Void> unbindTagsFromStandardAddresses(@Valid @RequestBody StandardAddressTagBindBo bo);

    /**
     * 目的：分页查询导入明细。
     * 入参：导入明细筛选条件与分页参数。
     * 出参：导入明细分页列表。
     * 关键约束：导入状态口径与导入实现保持一致。
     * 异常与副作用：查询条件非法时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/import/batch/list")
    TableDataInfo<StandardAddressImportDetailVo> listImportRecords(StandardAddressImportDetailBo bo, PageQuery pageQuery);

    /**
     * 目的：查询导入明细详情。
     * 入参：导入明细主键。
     * 出参：导入明细详情。
     * 关键约束：详情需能支撑问题定位和撤回。
     * 异常与副作用：记录不存在时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/import/batch/{id}")
    R<StandardAddressImportDetailVo> getImportRecordInfo(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：分页查询地址操作日志。
     * 入参：操作日志筛选条件与分页参数。
     * 出参：操作日志分页列表。
     * 关键约束：需要覆盖新增、修改、删除、导入、合并、拆分等动作。
     * 异常与副作用：查询条件非法时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/operation-log/list")
    TableDataInfo<StandardAddressOperationLogVo> listOperationLogs(StandardAddressOperationLogBo bo, PageQuery pageQuery);

    /**
     * 目的：查询地址操作日志详情。
     * 入参：日志主键。
     * 出参：日志详情。
     * 关键约束：详情信息要满足审计追溯要求。
     * 异常与副作用：日志不存在时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/operation-log/{id}")
    R<StandardAddressOperationLogVo> getOperationLogInfo(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：分页查询安装地址。
     * 入参：安装地址筛选条件与分页参数。
     * 出参：安装地址分页列表。
     * 关键约束：需支持标准地址关联查询。
     * 异常与副作用：查询条件非法时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/installation/list")
    TableDataInfo<InstallationAddressVo> listInstallationAddresses(InstallationAddressBo bo, PageQuery pageQuery);

    /**
     * 目的：查询安装地址详情。
     * 入参：安装地址主键。
     * 出参：安装地址详情。
     * 关键约束：需返回标准地址关联及设备信息。
     * 异常与副作用：主键不存在时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/installation/{id}")
    R<InstallationAddressVo> getInstallationAddressInfo(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：新增安装地址。
     * 入参：安装地址业务对象。
     * 出参：统一操作结果。
     * 关键约束：标准地址关联规则由实现层校验。
     * 异常与副作用：成功后会新增安装地址数据。
     */
    @PostMapping("/address/installation")
    R<Void> addInstallationAddress(@Valid @RequestBody InstallationAddressBo bo);

    /**
     * 目的：修改安装地址。
     * 入参：安装地址业务对象。
     * 出参：统一操作结果。
     * 关键约束：变更关联标准地址时需处理引用一致性。
     * 异常与副作用：成功后会更新安装地址数据。
     */
    @PostMapping("/address/installation/update")
    R<Void> editInstallationAddress(@Valid @RequestBody InstallationAddressBo bo);

    /**
     * 目的：删除安装地址。
     * 入参：安装地址主键集合。
     * 出参：统一操作结果。
     * 关键约束：需校验是否存在外部资源依赖。
     * 异常与副作用：成功后会删除安装地址数据。
     */
    @PostMapping("/address/installation/remove/{ids}")
    R<Void> removeInstallationAddress(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids);

    /**
     * 目的：分页查询监控规则。
     * 入参：规则筛选条件与分页参数。
     * 出参：规则分页列表。
     * 关键约束：规则状态需与任务执行口径一致。
     * 异常与副作用：查询条件非法时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/monitor/rule/list")
    TableDataInfo<StandardAddressMonitorRuleVo> listMonitorRules(StandardAddressMonitorRuleBo bo, PageQuery pageQuery);

    /**
     * 目的：查询监控规则详情。
     * 入参：规则主键。
     * 出参：规则详情。
     * 关键约束：规则主键不能为空。
     * 异常与副作用：规则不存在时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/monitor/rule/{id}")
    R<StandardAddressMonitorRuleVo> getMonitorRuleInfo(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：新增监控规则。
     * 入参：监控规则业务对象。
     * 出参：统一操作结果。
     * 关键约束：规则名称与核心配置合法性由实现层校验。
     * 异常与副作用：成功后会新增规则数据。
     */
    @PostMapping("/address/monitor/rule")
    R<Void> addMonitorRule(@Valid @RequestBody StandardAddressMonitorRuleBo bo);

    /**
     * 目的：修改监控规则。
     * 入参：监控规则业务对象。
     * 出参：统一操作结果。
     * 关键约束：变更后需保证与任务配置兼容。
     * 异常与副作用：成功后会更新规则数据。
     */
    @PostMapping("/address/monitor/rule/update")
    R<Void> editMonitorRule(@Valid @RequestBody StandardAddressMonitorRuleBo bo);

    /**
     * 目的：删除监控规则。
     * 入参：规则主键集合。
     * 出参：统一操作结果。
     * 关键约束：被任务引用时删除策略由实现层控制。
     * 异常与副作用：成功后会删除规则数据。
     */
    @PostMapping("/address/monitor/rule/remove/{ids}")
    R<Void> removeMonitorRule(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids);

    /**
     * 目的：批量启用监控规则。
     * 入参：规则主键集合。
     * 出参：统一操作结果。
     * 关键约束：重复启用需幂等。
     * 异常与副作用：成功后会更新规则状态。
     */
    @PostMapping("/address/monitor/rule/enable/{ids}")
    R<Void> enableMonitorRule(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids);

    /**
     * 目的：批量禁用监控规则。
     * 入参：规则主键集合。
     * 出参：统一操作结果。
     * 关键约束：禁用后不应再被任务调度。
     * 异常与副作用：成功后会更新规则状态。
     */
    @PostMapping("/address/monitor/rule/disable/{ids}")
    R<Void> disableMonitorRule(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids);

    /**
     * 目的：查询监控任务摘要。
     * 入参：无。
     * 出参：监控任务摘要。
     * 关键约束：摘要口径需和规则、异常记录状态保持一致。
     * 异常与副作用：统计失败时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/monitor/task/summary")
    R<StandardAddressMonitorTaskSummaryVo> summaryMonitorTasks();

    /**
     * 目的：手动立即执行一次监控扫描。
     * 入参：无。
     * 出参：提交结果，`1` 表示已成功提交后台执行，`0` 表示已有巡检执行中。
     * 关键约束：接口需快速返回，避免管理端因全量扫描耗时过长而超时。
     * 异常与副作用：成功后会异步触发监控作业并新增异常记录。
     */
    @PostMapping("/address/monitor/task/execute")
    R<Integer> executeMonitorTask();

    /**
     * 目的：分页查询监控任务。
     * 入参：任务筛选条件与分页参数。
     * 出参：监控任务分页列表。
     * 关键约束：列表字段需覆盖状态、规则、执行时间与统计信息。
     * 异常与副作用：查询条件非法时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/monitor/task/list")
    TableDataInfo<StandardAddressAdminVo.MonitorTaskVo> listMonitorTasks(StandardAddressAdminBo.MonitorTaskBo bo, PageQuery pageQuery);

    /**
     * 目的：查询监控任务详情。
     * 入参：任务主键。
     * 出参：监控任务详情。
     * 关键约束：详情需包含监控范围、关联规则和地址集合。
     * 异常与副作用：任务不存在时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/monitor/task/{id}")
    R<StandardAddressAdminVo.MonitorTaskVo> getMonitorTaskInfo(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：查询指定监控任务的运行日志。
     * 入参：任务主键与分页参数。
     * 出参：运行日志分页列表。
     * 关键约束：返回结果按最近开始时间倒序。
     * 异常与副作用：无写入副作用。
     */
    @PostMapping("/address/monitor/task/runs/{taskId}")
    TableDataInfo<StandardAddressAdminVo.MonitorTaskRunLogVo> listMonitorTaskRunLogs(
        @NotNull(message = "主键不能为空") @PathVariable Long taskId,
        PageQuery pageQuery);

    /**
     * 目的：新增监控任务。
     * 入参：监控任务业务对象。
     * 出参：统一操作结果。
     * 关键约束：执行规则、关联规则和监控范围需统一校验。
     * 异常与副作用：成功后会新增任务与关联关系。
     */
    @PostMapping("/address/monitor/task")
    R<Void> addMonitorTask(@Valid @RequestBody StandardAddressAdminBo.MonitorTaskBo bo);

    /**
     * 目的：修改监控任务。
     * 入参：监控任务业务对象。
     * 出参：统一操作结果。
     * 关键约束：修改后需同步调度配置。
     * 异常与副作用：成功后会更新任务定义和关联关系。
     */
    @PostMapping("/address/monitor/task/update")
    R<Void> editMonitorTask(@Valid @RequestBody StandardAddressAdminBo.MonitorTaskBo bo);

    /**
     * 目的：重跑指定监控任务。
     * 入参：任务主键。
     * 出参：统一操作结果。
     * 关键约束：需保证当前任务状态允许重跑。
     * 异常与副作用：成功后会触发新的任务执行实例。
     */
    @PostMapping("/address/monitor/task/rerun/{id}")
    R<Void> rerunMonitorTask(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：暂停指定监控任务。
     * 入参：任务主键。
     * 出参：统一操作结果。
     * 关键约束：暂停状态需可审计、可恢复。
     * 异常与副作用：成功后会更新任务状态并停止后续调度。
     */
    @PostMapping("/address/monitor/task/pause/{id}")
    R<Void> pauseMonitorTask(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：终止指定监控任务。
     * 入参：任务主键。
     * 出参：统一操作结果。
     * 关键约束：终止后需清理执行状态和未来调度。
     * 异常与副作用：成功后会更新任务状态并停止执行。
     */
    @PostMapping("/address/monitor/task/terminate/{id}")
    R<Void> terminateMonitorTask(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：分页查询异常地址记录。
     * 入参：异常记录筛选条件与分页参数。
     * 出参：异常记录分页列表。
     * 关键约束：状态口径与工单流转保持一致。
     * 异常与副作用：查询条件非法时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/monitor/record/list")
    TableDataInfo<StandardAddressMonitorRecordVo> listMonitorRecords(StandardAddressMonitorRecordBo bo, PageQuery pageQuery);

    /**
     * 目的：查询异常地址详情。
     * 入参：异常记录主键。
     * 出参：异常记录详情。
     * 关键约束：详情需保留规则命中与处理信息。
     * 异常与副作用：记录不存在时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/monitor/record/{id}")
    R<StandardAddressMonitorRecordVo> getMonitorRecordInfo(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：修改异常地址记录。
     * 入参：异常记录业务对象。
     * 出参：统一操作结果。
     * 关键约束：处理状态流转由实现层统一约束。
     * 异常与副作用：成功后会更新异常记录状态。
     */
    @PostMapping("/address/monitor/record/update")
    R<Void> editMonitorRecord(@Valid @RequestBody StandardAddressMonitorRecordBo bo);

    /**
     * 目的：删除异常地址记录。
     * 入参：异常记录主键集合。
     * 出参：统一操作结果。
     * 关键约束：删除策略需与审计要求兼容。
     * 异常与副作用：成功后会删除异常记录。
     */
    @PostMapping("/address/monitor/record/remove/{ids}")
    R<Void> removeMonitorRecord(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids);

    /**
     * 目的：批量忽略异常地址记录。
     * 入参：异常记录主键集合。
     * 出参：统一操作结果。
     * 关键约束：重复提交需要幂等。
     * 异常与副作用：成功后会将异常记录状态置为忽略。
     */
    @PostMapping("/address/monitor/record/ignore/{ids}")
    R<Void> ignoreMonitorRecord(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids);

    /**
     * 目的：分页查询地址工单。
     * 入参：工单筛选条件与分页参数。
     * 出参：工单分页列表。
     * 关键约束：工单状态口径需与异常记录、修正流程一致。
     * 异常与副作用：查询条件非法时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/work-order/list")
    TableDataInfo<StandardAddressAdminVo.WorkOrderVo> listWorkOrders(StandardAddressAdminBo.WorkOrderBo bo, PageQuery pageQuery);

    /**
     * 目的：查询地址工单详情。
     * 入参：工单主键。
     * 出参：工单详情。
     * 关键约束：详情需包含原始地址、修正地址、关联网格和操作轨迹。
     * 异常与副作用：工单不存在时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/work-order/{id}")
    R<StandardAddressAdminVo.WorkOrderVo> getWorkOrderInfo(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：由异常记录批量生成工单。
     * 入参：工单业务对象，重点使用异常记录ID集合。
     * 出参：统一操作结果。
     * 关键约束：重复提单策略由实现层统一控制。
     * 异常与副作用：成功后会新增工单及流转记录。
     */
    @PostMapping("/address/work-order")
    R<Void> createWorkOrder(@Valid @RequestBody StandardAddressAdminBo.WorkOrderBo bo);

    /**
     * 目的：提交工单修正结果。
     * 入参：工单主键与修正业务对象。
     * 出参：统一操作结果。
     * 关键约束：修正结果需与标准地址、网格归属和同步规则联动。
     * 异常与副作用：成功后会更新工单状态并可能落库新地址。
     */
    @PostMapping("/address/work-order/correct/{id}")
    R<Void> correctWorkOrder(@NotNull(message = "主键不能为空") @PathVariable Long id,
                             @Valid @RequestBody StandardAddressAdminBo.WorkOrderBo bo);

    /**
     * 目的：驳回工单。
     * 入参：工单主键与驳回原因。
     * 出参：统一操作结果。
     * 关键约束：驳回原因必须保留，便于审计追溯。
     * 异常与副作用：成功后会更新工单状态并写入流转日志。
     */
    @PostMapping("/address/work-order/reject/{id}")
    R<Void> rejectWorkOrder(@NotNull(message = "主键不能为空") @PathVariable Long id,
                            @Valid @RequestBody StandardAddressAdminBo.WorkOrderBo bo);

    /**
     * 目的：关键字搜索标准地址，支撑选址平台检索。
     * 入参：关键字、最大层级和返回上限。
     * 出参：选址结果列表。
     * 关键约束：默认限制楼栋及以下逻辑由实现层统一控制。
     * 异常与副作用：查询异常时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/selection/search")
    R<List<StandardAddressVo>> searchStandardAddresses(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Integer levelMax,
                                                       @RequestParam(required = false) Integer limit);

    /**
     * 目的：地图圈选查询标准地址。
     * 入参：地图圈选参数。
     * 出参：选址结果列表。
     * 关键约束：几何参数和坐标系合法性由实现层负责。
     * 异常与副作用：参数非法时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/selection/mapSearch")
    R<List<StandardAddressAdminVo.SelectionResultVo>> mapSearchStandardAddresses(@Valid @RequestBody StandardAddressAdminBo.SelectionMapSearchBo bo);

    /**
     * 目的：查询楼栋或地址下的房间列表。
     * 入参：标准地址主键。
     * 出参：房间列表。
     * 关键约束：仅支持房间维度地址层级。
     * 异常与副作用：地址不存在或层级不支持时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/selection/rooms/{standardAddressId}")
    R<List<StandardAddressAdminVo.SelectionRoomVo>> listSelectionRooms(@NotNull(message = "标准地址不能为空") @PathVariable Long standardAddressId);

    /**
     * 目的：预览标准地址选址结果。
     * 入参：标准地址主键。
     * 出参：标准地址预览信息。
     * 关键约束：预览内容需覆盖页面回显所需核心字段。
     * 异常与副作用：地址不存在时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/selection/preview/standard/{standardAddressId}")
    R<StandardAddressAdminVo.SelectionPreviewVo> previewStandardAddress(@NotNull(message = "标准地址不能为空") @PathVariable Long standardAddressId);

    /**
     * 目的：预览安装地址生成结果。
     * 入参：标准地址和可选房间集合。
     * 出参：安装地址预览信息。
     * 关键约束：仅预览不落库。
     * 异常与副作用：参数非法时返回业务异常，无写入副作用。
     */
    @PostMapping("/address/selection/preview/installation")
    R<StandardAddressAdminVo.SelectionPreviewVo> previewInstallationAddress(@Valid @RequestBody StandardAddressAdminBo.SelectionPreviewBo bo);

    /**
     * 目的：楼栋级地址下新增房间标准地址并生成安装地址。
     * 入参：房间创建参数。
     * 出参：创建结果。
     * 关键约束：同楼栋下房间号唯一。
     * 异常与副作用：成功后会新增标准地址与安装地址。
     */
    @PostMapping("/address/selection/room")
    R<StandardAddressSelectionResultVo> createRoomStandardAddress(@Valid @RequestBody StandardAddressSelectionRoomBo bo);

    /**
     * 目的：提交生成安装地址。
     * 入参：标准地址和可选房间集合。
     * 出参：统一操作结果。
     * 关键约束：生成结果需与预览口径一致。
     * 异常与副作用：成功后会新增安装地址数据。
     */
    @PostMapping("/address/selection/installation")
    R<Void> generateInstallationAddress(@Valid @RequestBody StandardAddressAdminBo.SelectionPreviewBo bo);
}
