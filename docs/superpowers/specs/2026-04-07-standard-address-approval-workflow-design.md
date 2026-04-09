# 标准地址审批流设计

## 背景

标准地址的新增、编辑、删除、导入、合并、拆分当前均直接落正式表 `ADDR_SEGM`。根据《标准地址管理——审批流程需求文档》，上述变更必须改为“先申请、后审批、审批通过再生效”。

本次设计目标：

- 复用现有 `workflow` 模块，不重新造审批引擎。
- 审批期间不影响正式地址查询与使用。
- 驳回后只能重新发起，不允许改原申请单继续流转。
- 前端新增“待审批地址管理”模块，覆盖“我提交的 / 待审批 / 已审批”。

## 约束

- 地址正式数据源仍为 `ftth_cloud_address.ADDR_SEGM`。
- 审批链路必须兼容现有标准地址 ES 同步逻辑。
- `ruoyi-modules/ruoyi-address` 的核心类、接口、方法必须补齐中文注释。
- 现有 workflow 通过 `RemoteWorkflowService` 发起流程，通过 `ProcessEvent / ProcessTaskEvent / ProcessDeleteEvent` 回调业务侧。

## 方案选型

### 方案 A：正式表直接写草稿状态

- 在 `ADDR_SEGM` 先落审批中数据，再通过状态字段区分是否生效。
- 缺点：会污染正式查询链路；列表、详情、ES 同步、安装地址关联都要改读逻辑；删除/合并/拆分的过渡态复杂。

### 方案 B：独立审批申请单 + 审批通过后执行正式写链路（推荐）

- 所有写操作先转成“审批申请单”。
- workflow 只负责审批流转与待办。
- 申请通过时，调用正式执行器写 `ADDR_SEGM` 与 ES。
- 驳回仅更新申请单状态，不触碰正式数据。

选择方案 B，原因：

- 正式查询链路零污染。
- 能完整满足“审批中原数据继续可用”。
- 与 workflow 的业务接入样例一致，风险最低。
- 后续可扩展多级审批、批量审批、导出追溯。

## 总体架构

### 后端分层

1. `StandardAddressApprovalService`
   - 负责创建申请单、查询申请单、发起流程、审批详情聚合。
2. `StandardAddressApprovalExecutor`
   - 负责审批通过后的正式写入。
   - 内部复用 `StandardAddressCommandService` / `StandardAddressImportService`。
3. `StandardAddressWorkflowListener`
   - 监听 workflow 流程事件，同步申请单状态、当前任务、审批结果。
4. `StandardAddressApprovalController`
   - 提供“我提交的 / 详情 / 审批详情”接口。
5. `workflow` 前端接口直连现有 `/task/*`、`/instance/*`
   - 待审批 / 已审批 列表与通过/驳回操作直接复用 workflow 现成 API。

### 数据流

1. 前端发起新增/修改/删除/导入/合并/拆分。
2. 地址模块先做业务校验与快照组装，生成申请单。
3. 地址模块通过 `RemoteWorkflowService.startWorkFlow` 发起流程。
4. workflow 产生待办，前端在“待审批”页通过 workflow API 查看。
5. 审批通过：
   - workflow 发布 `ProcessEvent(status=finish)`。
   - 地址模块监听事件，执行正式写链路。
   - 执行成功后申请单标记为“审批通过”。
6. 审批驳回：
   - workflow 发布 `ProcessEvent(status=back)`。
   - 地址模块仅更新申请单为“审批驳回”，记录原因。

## 数据模型

新增表：`address_standard_approval`

核心字段：

- `id`：申请单 ID，同时作为 workflow `businessId`
- `apply_no`：申请单号
- `operation_type`：`ADD / IMPORT / UPDATE / DELETE / MERGE / SPLIT`
- `business_status`：workflow 业务状态，复用 `BusinessStatusEnum`
- `approval_status`：页面展示状态，归一到 `WAITING / APPROVED / REJECTED / EXECUTING / EXECUTE_FAILED`
- `flow_code` / `instance_id` / `current_task_id`
- `biz_title`：展示标题
- `source_segm_ids`：关联原地址 ID 集合（JSON）
- `target_segm_ids`：结果地址 ID 集合（JSON，可为空）
- `source_snapshot`：原地址快照（JSON）
- `target_snapshot`：新地址快照（JSON）
- `request_payload`：原始请求参数（JSON）
- `import_batch_payload`：导入明细（JSON，可为空）
- `submit_user_id` / `submit_user_name` / `submit_dept_id` / `submit_dept_name`
- `approve_user_id` / `approve_user_name` / `approve_time`
- `reject_reason`
- `execute_message`
- `del_flag`
- `create_by/create_time/update_by/update_time/create_dept`

设计原则：

- 快照采用 JSON 存储，避免为六类操作分别建表。
- 正式执行不依赖前端再次传参，只依赖申请单快照。
- 导入按“整批一单”处理，满足需求文档中的“整批驳回/整批通过”。

## 正式执行策略

### ADD

- 审批通过后调用 `StandardAddressCommandService.addStandardAddress`。

### UPDATE

- 审批通过后调用 `StandardAddressCommandService.updateStandardAddress`。
- 审批前正式地址保持不变。

### DELETE

- 申请时先执行删除前校验（子级、安装地址确认）。
- 审批通过后调用 `StandardAddressCommandService.deleteStandardAddresses(..., true)`。

### IMPORT

- 申请时只做模板解析与逐条校验，不写正式表、不写导入记录。
- 审批通过后执行“导入正式执行器”，批量写正式地址并补导入记录、失败明细、操作日志。

### MERGE

- 审批通过后调用 `StandardAddressCommandService.mergeStandardAddresses`。

### SPLIT

- 审批通过后调用 `StandardAddressCommandService.splitStandardAddress`。

## workflow 集成设计

### 流程编码

- 新增流程编码：`address_standard_approve_v1`

### 流程结构

- 开始
- 申请人
- 分公司审批员
- 结束

节点权限：

- 审批节点暂按角色权限控制，使用 workflow 的 `permissionFlag`。
- 本地提供 SQL/JSON 初始化脚本，便于在本地与测试环境落库。

### 业务扩展信息

- `businessCode`：申请单号
- `businessTitle`：如 `标准地址编辑审批-莲花新城南苑`

### 事件监听

- `ProcessEvent submit=true`：申请单状态置为 `WAITING`
- `ProcessTaskEvent`：回填 `currentTaskId`、当前节点信息
- `ProcessEvent status=finish`：执行正式变更
- `ProcessEvent status=back`：记录驳回状态和驳回原因
- `ProcessDeleteEvent`：兜底清理实例关联字段，不删除业务申请单

## 前端设计

### 标准地址现有写操作

- 新增/修改/删除/导入/合并/拆分成功提示统一改为：
  - `已提交审批，待审批通过后生效，审批期间原地址可继续使用。`

### 新增页面

- 路由：`/standard/approvals`
- 菜单：`待审批地址管理`
- 页面结构：3 个 Tab
  - 我提交的：调用地址模块申请单列表
  - 待审批：调用 workflow `/task/pageByAllTaskWait?flowCode=address_standard_approve_v1`
  - 已审批：调用 workflow `/task/pageByAllTaskFinish?flowCode=address_standard_approve_v1`

### 详情抽屉

- 展示：
  - 申请单号、操作类型、提交人、申请时间、审批状态
  - 原地址快照
  - 新地址快照
  - 审批记录
  - 驳回原因/审批意见

### 审批动作

- 通过：调用 workflow `/task/completeTask`
- 驳回：调用 workflow `/task/backProcess`
- 详情里的业务数据由地址模块接口提供，审批动作走 workflow 原生接口。

## 错误处理

- 申请创建成功但流程启动失败：事务回滚，不保留申请单。
- 审批通过后正式执行失败：
  - 申请单状态置为 `EXECUTE_FAILED`
  - 保留流程已完成事实和执行错误信息，便于人工追溯
  - 不自动重试，避免重复写入
- 驳回原因必填，由 workflow 历史任务意见和本地申请单冗余保存。

## 测试策略

### 后端

- 申请单创建：六类操作分别校验快照、标题、流程参数。
- 事件监听：提交、通过、驳回、执行失败。
- 正式执行：通过审批事件触发对应命令服务。
- 回归：标准地址原查询链路不受影响。

### 前端

- 新菜单与新路由可访问。
- 写操作提交后提示文案正确。
- 三个 Tab 数据源切换正确。
- 审批详情能正确展示源/目标快照与审批状态。

## 交付物

- 地址审批申请单表、实体、Mapper、Service、Controller
- workflow 流程定义初始化 SQL / JSON
- 标准地址写接口审批化改造
- 待审批地址管理页面
- 后端单元测试与前端 Vitest 用例
