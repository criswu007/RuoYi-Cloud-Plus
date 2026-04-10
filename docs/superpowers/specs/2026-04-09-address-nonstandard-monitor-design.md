# 标准地址非标地址监控设计

## 背景

当前仓库已经存在非标地址监控的第一版骨架：

- 前端已有 [MonitorRules.vue](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-address-ui/src/views/MonitorRules.vue)、[MonitorTask.vue](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-address-ui/src/views/MonitorTask.vue)、[MonitorRecords.vue](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-address-ui/src/views/MonitorRecords.vue)
- 后端已有规则表 `address_standard_monitor_rule`、异常记录表 `address_standard_monitor_record`
- `StandardAddressAdminApi` 已预留规则配置、任务管理、异常记录分页/详情/状态流转等完整接口合同

但现状仍是“占位式可联调”，与需求文档中的“智能检测配置 -> 监控任务管理 -> 异常地址预警”闭环差距明显：

- 规则配置仍是通用文本规则，缺少模板化业务配置
- 任务侧只有摘要与“立即执行”，没有任务定义、范围、运行记录和 `snailjob` 对接
- 异常记录仍以简单 `ruleId + standardAddressId + status` 建模，缺少预警快照、来源任务、命中详情和批量治理能力

## 本次设计范围

本轮只覆盖管理端闭环，不处理企业微信、工单、BOSS 等外部联动。

本次固定采用以下口径：

- 规则以“规则”为中心，不引入“检测方案”层
- 调度框架统一基于 `snailjob`
- 任务范围首版只支持 `ALL`、`REGION`、`ADDRESS`
- 检测规则首版采用模板化配置，不开放自由脚本/表达式
- 智能检测先提供模板、开关、阈值和结果展示位，不接入外部 AI 平台

## 方案选择

本轮对比了三种方案：

### 方案 A：继续沿用当前轻量骨架，只补页面字段

优点：改动最小，能较快补齐页面。

缺点：无法真正支撑任务管理与调度；后续接 `snailjob`、任务日志、预警去重时仍需推倒重做。

### 方案 B：规则中心 + 任务定义 + 运行日志 + 异常记录快照

优点：与接口基线、归档库表设计、需求书流程一致；可以先完成管理端闭环，再逐步扩展外部通知。

缺点：需要补表、补实体和前后端页面重构。

### 方案 C：规则中心 + 动态脚本引擎

优点：灵活。

缺点：安全风险高，维护成本大，和当前项目的业务模板化诉求不匹配。

本次采用 **方案 B**。

## 功能设计

### 1. 智能检测配置

规则页面升级为“模板化规则配置页”，规则不是任意文本，而是“规则模板 + 模板参数”。

首版规则模板固定为 4 类：

- `FORMAT_STANDARD`：格式规范性检测
- `REGION_COMPLIANCE`：行政区划合规性检测
- `ELEMENT_COMPLETENESS`：地址要素完整性检测
- `SMART_SUSPECT`：智能疑似异常检测

规则主字段建议补齐：

- `ruleCode`：稳定编码，便于任务引用和后续统计
- `ruleName`
- `ruleTemplate`
- `ruleStatus`
- `severity`
- `priority`
- `dedupHours`
- `configJson`
- `remark`

其中 `configJson` 由模板决定：

- 格式规范性：非法字符、连续分隔符、名称长度、是否允许纯数字等
- 行政区划合规性：是否校验前三级区域主数据、是否校验 `regionId` 与父级链一致
- 地址要素完整性：按 `segmType/levelId` 配置必填项与向上包含规则
- 智能疑似异常：疑似分阈值、相似度阈值、是否启用名称相似/父子重复/异常短名检测

前端页面调整为：

- 列表页：按模板、状态、优先级、严重等级筛选，支持批量启停
- 新增/编辑：按模板动态展示表单，不再直接暴露自由文本规则内容
- 详情弹窗：展示模板说明、阈值、适用范围、最近命中统计

### 2. 监控任务管理

任务页从“摘要页”升级为“摘要 + 列表 + 新增编辑 + 运行记录入口”。

任务定义采用持久化模型：

- `address_standard_monitor_task`：任务主表
- `address_standard_monitor_task_rule_rel`：任务关联规则
- `address_standard_monitor_task_scope_rel`：任务范围明细
- `address_standard_monitor_task_run_log`：每次执行日志

任务核心字段建议为：

- `taskName`
- `taskType`：`MANUAL` / `SCHEDULE`
- `taskStatus`：`ENABLED` / `PAUSED` / `TERMINATED`
- `executeRule`：cron 或保留统一表达式
- `monitorScope`
- `snailJobTaskId`
- `taskDesc`
- `lastExecuteTime`
- `lastSuccessTime`
- `lastFailureReason`

任务范围首版只支持：

- `ALL`：扫描全量标准地址
- `REGION`：按区域 ID 集合扫描
- `ADDRESS`：按指定 `segmId` 集合扫描

`snailjob` 对接策略：

- 每个启用的定时任务在保存时同步注册/更新 `snailjob`
- 手动执行不依赖定时触发，直接创建一次运行日志并调用统一执行器
- 暂停任务时同步暂停 `snailjob`
- 终止任务时停调度并标记任务不可继续使用
- 重跑任务基于任务定义重新创建一次运行实例，不复用旧运行记录

执行链路统一为：

1. 创建 `runLog`
2. 加载任务定义、规则集合、范围集合
3. 分页扫描标准地址
4. 逐条应用规则模板检测
5. 按去重策略写入异常记录
6. 回写运行日志统计

### 3. 异常地址预警

“预警”在本轮的定义是“管理端待治理异常池”，不发送外部通知。

异常记录模型需从当前最小字段集升级为预警快照模型，建议补齐：

- `segmId`
- `standNameSnapshot`
- `regionIdSnapshot`
- `ruleId`
- `ruleNameSnapshot`
- `ruleTemplateSnapshot`
- `taskId`
- `taskRunLogId`
- `severity`
- `hitDetailJson`
- `dedupKey`
- `firstDetectedTime`
- `lastDetectedTime`
- `hitCount`
- `status`：`PENDING` / `IGNORED` / `PROCESSED`
- `processRemark`
- `processBy`
- `processTime`

预警列表页调整为：

- 支持按任务、规则模板、规则、状态、区域、发现时间筛选
- 列表字段展示标准地址名称、命中规则、严重等级、首次/最近发现时间、命中次数
- 支持批量忽略、批量标记已处理、查看命中详情
- 详情页展示“异常说明 + 命中字段 + 当前地址信息 + 来源任务”

这里不做工单，但需要预留后续工单接入位：

- `taskRunLogId`、`dedupKey`、`processRemark` 保留
- 详情页按钮区预留“创建工单”占位，不在本轮实现

## 后端设计

### 接口口径

当前 `ruoyi-address` 的监控 Controller 使用 `GET/PUT/DELETE`，与本模块文档基线“内部管理端统一 POST”不一致。本轮统一按 `StandardAddressAdminApi` 口径收口：

- 规则：`/address/monitor/rule/*`
- 任务：`/address/monitor/task/*`
- 记录：`/address/monitor/record/*`

需要新增或补齐的能力：

- 规则启用/禁用
- 任务列表/详情/新增/修改/重跑/暂停/终止
- 异常记录批量忽略
- 规则下拉、任务下拉、模板下拉等辅助接口

### 检测引擎

检测逻辑不直接散落在任务服务中，建议新增模板化检测器层：

- `MonitorRuleDetector`：统一接口
- `FormatStandardDetector`
- `RegionComplianceDetector`
- `ElementCompletenessDetector`
- `SmartSuspectDetector`

任务执行器只负责调度和聚合，单条地址检测由 detector 返回命中结果。

### 并发与幂等

- 同一任务同一时间只允许一个运行实例处于 `RUNNING`
- 手动执行和定时执行共用互斥锁，避免重复扫描
- 异常记录落库前按 `dedupKey + dedupHours` 去重
- 任务暂停/终止只影响后续调度，不强行中断已开始的单次扫描

## 前端设计

### 路由与导航

保留现有 3 个菜单，但页面语义升级：

- `/monitor/rules`：智能检测配置
- `/monitor/task`：监控任务管理
- `/monitor/records`：异常地址预警

建议把菜单文案从“规则配置/监控任务/异常地址”同步优化为更贴近业务的名称，但路径保持不变，避免已有联调地址失效。

### 页面结构

`MonitorRules.vue`

- 列表 + 统计卡片 + 模板化编辑抽屉
- 不再直接输入 `ruleContent`
- 增加启用/禁用、批量操作、模板说明

`MonitorTask.vue`

- 顶部保留摘要卡片
- 下半区新增任务列表
- 新增任务编辑抽屉：名称、执行方式、cron、范围、规则勾选
- 详情页/抽屉展示最近运行记录和失败原因

`MonitorRecords.vue`

- 扩展筛选区与表格列
- 详情弹窗改为分区展示：地址信息、命中规则、命中详情、来源任务、处理信息
- 批量处理操作保留

## 数据迁移策略

当前已有两张表：

- `address_standard_monitor_rule`
- `address_standard_monitor_record`

本轮建议：

- 规则表扩字段，不改表名
- 异常记录表扩字段，不改表名
- 新增任务主表、任务规则关系表、任务范围表、任务运行日志表

旧数据兼容方式：

- 原有规则按规则内容人工迁移到最接近的模板；无法自动归类的旧规则先迁移为禁用态并标记“待治理”
- 原有异常记录补齐快照字段时，以当前地址/规则信息兜底回填

## 测试与验证

本轮至少覆盖：

- 规则模板参数校验测试
- 任务新增/修改/暂停/终止/重跑测试
- `snailjob` 执行器参数解析与运行日志回写测试
- 异常去重测试
- 异常记录分页与批量忽略测试
- 前端页面联调与基础回归测试

## 明确不做

- 外部通知推送
- 工单创建与状态回写
- 标签/管理站/层级维度的复杂任务范围
- 自由脚本规则引擎
- AI 大模型外部调用
