# 标准地址模块导入接口全链路逻辑说明

## 1. 文档目的

本文面向 `ruoyi-modules/ruoyi-address` 的维护与联调场景，梳理“标准地址导入”接口从上传 Excel 到审批通过后正式落库的完整链路，并结合当前代码中的实体、Mapper、SQL DDL 说明涉及表结构与状态流转。

当前实现不是“上传即写正式表”，而是：

1. 上传阶段先落导入批次与导入行结果。
2. 每一条通过前置校验的 Excel 行单独发起一张审批单。
3. workflow 审批通过后，才真正写入 `ADDR_SEGM`。
4. 审批状态会反向同步到导入行结果和导入批次汇总。

## 2. 入口接口与请求合同

### 2.1 导入入口

- 接口路径：`POST /address/standard/import`
- 控制器：`StandardAddressController#importStandardAddressData`
- 入参：
  - `file`：Excel 文件，`@RequestPart("file")`
  - `updateSupport`：是否允许更新，`@RequestParam(defaultValue = "false")`
- 权限：`address:standard:import`
- 幂等：`@RepeatSubmit(interval = 5000)`

控制器只做两件事：

1. 通过 `ExcelUtil.importExcel(...)` 把 Excel 解析成 `List<StandardAddressImportVo>`。
2. 调用 `IStandardAddressService#importStandardAddressData(...)` 进入服务层。

返回值是 `StandardAddressImportResultVo`，但返回语义是“导入批次已创建、审批已提交/部分失败”，不是“正式地址已生效”。

### 2.2 模板字段

导入模板由 `POST /address/standard/import/template` 导出，对应 `StandardAddressImportVo` 的列如下：

| Excel 列 | 字段 | 说明 |
| --- | --- | --- |
| 分段地址类型 | `segmTypeName` | 对应 `segm_addr_type.name` |
| 是否城区 | `isCityLabel` | 只能识别“是/否” |
| 父级地址 | `parentStandName` | 优先匹配 `ADDR_SEGM.stand_name`，其次匹配 `spc_region.region_name` |
| 当级名称 | `segmName` | 当前层级地址名 |
| 所属维修管理站 | `maintenanceStationName` | 解析到 `ADDR_SEGM.station_id` |
| 所属安装管理站 | `installStationName` | 解析到 `ADDR_SEGM.installstation_id` |
| 所属营业管理站 | `businessStationName` | 解析到 `ADDR_SEGM.busstation_id` |
| 接入方式 | `accessModeName` | 解析到 `ADDR_SEGM.addr_in_type_ftth` |
| 接入能力 | `accessCapabilityName` | 解析到 `ADDR_SEGM.ftth_pon_type` |
| 城乡属性 | `areaTypeName` | 解析到 `ADDR_SEGM.area_type` |
| 房屋属性 | `placeTypeName` | 解析到 `ADDR_SEGM.place_type` |
| 是否配套小区 | `supportingFeeCommunityLabel` | 解析到 `ADDR_SEGM.segm_name_fir` |
| 覆盖户数 | `coverNumText` | 解析到 `ADDR_SEGM.cover_num` |
| 工程编号 | `singleProjectCode` | 解析到 `ADDR_SEGM.post_code` |

模板下拉项由 `StandardAddressDictionaryService#listImportTemplateOptions()` 生成，字典来源如下：

- 地址类型：`segm_addr_type`
- 接入方式/接入能力/城乡属性/房屋属性：`pub_restriction`
- 是/否项：服务内硬编码 `"是" / "否"`

## 3. 全链路总览

### 3.1 主流程

```text
上传 Excel
  -> Controller 解析为 StandardAddressImportVo 列表
  -> StandardAddressServiceImpl 创建导入批次
  -> 按行执行前置校验
     -> 校验失败：写导入行结果，状态 VALIDATE_FAILED
     -> 校验通过：写导入行结果，状态 WAITING_APPROVAL
                   -> 每行提交一张审批单 address_standard_approval
  -> 刷新批次汇总并返回前端

审批流推进
  -> StandardAddressWorkflowListener 监听流程事件
  -> 审批驳回：导入行状态改为 REJECTED_FAILED
  -> 审批通过：调用 StandardAddressApprovalExecutor
     -> StandardAddressImportService.executeApprovedImportRow(...)
     -> StandardAddressCommandService 新增/修改 ADDR_SEGM
     -> 同步 ES、写操作日志
  -> 导入行状态改为 APPROVED_SUCCESS 或 EXECUTE_FAILED
  -> 刷新批次汇总
```

### 3.2 当前实现的关键特征

- 导入批次维度和审批单维度是分离的。
- 一个 Excel 文件对应一个导入批次。
- 一个 Excel 行对应一条 `address_standard_import_detail` 记录。
- 一个校验通过的 Excel 行对应一张 `address_standard_approval` 审批单。
- 批次状态不是单独维护的真相源，而是由行结果表聚合出来。

## 4. 上传阶段详细逻辑

### 4.1 创建批次主记录

`StandardAddressServiceImpl#importStandardAddressData(...)` 是当前导入主入口。

它首先调用 `StandardAddressImportService#buildImportBatchRecord(...)` 构建批次记录，然后插入 `address_standard_import_batch`。

上传阶段批次初始状态：

- `status = 0`，表示进行中/待审批
- `total_count = Excel 总行数`
- `success_count = 0`
- `fail_count = 0`
- `update_support = 是否允许更新`

这里的“success/fail”不是最终正式入库结果，而是后续由行结果聚合刷新。

### 4.2 逐行前置校验

每一行调用 `StandardAddressImportService#validateImportRow(...)`，其本质是执行 `prepareRow(...)`，只校验，不写正式表。

校验逻辑如下。

#### 4.2.1 必填校验

以下字段为空会直接抛业务异常：

- `parentStandName`
- `segmName`
- `segmTypeName`

#### 4.2.2 地址类型与层级校验

先通过 `StandardAddressDictionaryService#resolveSegmTypeByName(...)` 把“分段地址类型”文本转成 `segm_type` 编码，再通过 `resolveAddrLevel(...)` 解析业务层级。

关键约束：

- 地址类型不存在，导入失败。
- 一二级标准地址为只读基础数据，禁止导入新增或更新。

这里的一二级判断不是直接看 `level_id = 1/2`，而是统一走字典服务映射后的业务 `addrLevel`。

#### 4.2.3 父级地址解析

父级地址优先按以下顺序解析：

1. `ADDR_SEGM.stand_name = parentStandName`
2. `spc_region.region_name = parentStandName`

也就是说，导入支持两类父级：

- 已存在的标准地址节点
- 一二级区域投影节点

如果两者都找不到，则报“父级地址不存在”。

#### 4.2.4 重复判定与更新支持

系统会按以下条件查询现有有效地址：

- `parent_segm_id = 父级ID`
- `segm_name = 当级名称`
- `delete_state = 0`

命中后：

- `updateSupport = false`：报“标准地址已存在，请开启更新支持后重试”
- `updateSupport = true`：后续按更新链路执行

#### 4.2.5 站点与字典属性解析

导入行会组装为 `StandardAddressBo`，主要映射如下：

| 业务字段 | 解析方式 | 目标字段 |
| --- | --- | --- |
| 是否城区 | `是/否 -> Y/N` | `isCity` |
| 维修站 | `spc_station(region_id, manage_type=2017101, name like)` | `stationId` |
| 安装站 | `spc_station(region_id, manage_type=2017102, name like)` | `installStationId` |
| 营业站 | `spc_station(region_id, manage_type=2017103, name like)` | `busStationId` |
| 接入方式 | `pub_restriction.keyword=ADDR_IN_TYPE_FTTH` | `addrInTypeFtth` |
| 接入能力 | `pub_restriction.keyword=FTTH_PON_TYPE` | `ftthPonType` |
| 城乡属性 | `pub_restriction.keyword=AREA_TYPE` | `areaType` |
| 房屋属性 | `pub_restriction.keyword=ADDR_PLACE_TYPE`，必要时回退 `ADDR_UNIT_TYPE` | `placeType` |
| 是否配套小区 | `是/否 -> Y/N` | `supportingFeeCommunityFlag` |
| 覆盖户数 | 非数字或空时默认 `1` | `coverNum` |
| 工程编号 | trim 后透传 | `singleProjectCode` |

注意：

- 管理站匹配是按 `region_id + manage_type + 名称模糊匹配`，命中多条时取排序后的第一条。
- 接入方式目前只接受光纤接入方式字典，不走 LAN 接入方式。

### 4.3 导入行结果落库

无论成功还是失败，上传阶段每一行都会落一条 `address_standard_import_detail`。

#### 4.3.1 前置校验失败

如果 `validateImportRow(...)` 抛异常：

- 写入 `address_standard_import_detail`
- `status = VALIDATE_FAILED`
- `fail_reason = 异常信息`
- `raw_payload = 当前 Excel 行 JSON 快照`

这类记录不会发起审批。

#### 4.3.2 前置校验通过

如果前置校验通过：

1. 先写入 `address_standard_import_detail`
2. 行状态先置为 `WAITING_APPROVAL`
3. 再调用 `approvalService.submitImportRowApproval(...)` 发起单行审批
4. 回填：
   - `approval_id`
   - `approval_no`
   - `approval_status`

如果审批单发起失败：

- 行状态更新为 `EXECUTE_FAILED`
- `fail_reason = 发起审批异常`
- `approval_status = EXECUTE_FAILED`

### 4.4 上传阶段返回值

上传接口返回的 `StandardAddressImportResultVo` 关键字段含义：

- `batchId / batchNo`：导入批次标识
- `status`：批次聚合状态，通常为待处理
- `totalCount`：Excel 总行数
- `successCount`：上传阶段“成功提审”的行数，不等于最终正式落库成功数
- `failCount`：上传阶段校验失败或提审失败的行数
- `pendingCount`：待审批的行数
- `failureExportable`：是否可导出失败明细

## 5. 审批阶段详细逻辑

### 5.1 审批单如何保存

每个校验通过的 Excel 行会生成一张 `address_standard_approval` 记录，操作类型固定为 `IMPORT`。

审批单里保存两类关键 JSON：

- `request_payload`
  - 包含 `batchId/itemId/rowNum/updateSupport/operName/fileName/row`
- `target_snapshot`
  - 保存当前单行导入数据快照

因此审批通过时，不需要重新读取原始 Excel 文件，直接用审批单快照恢复业务对象。

### 5.2 workflow 事件监听

`StandardAddressWorkflowListener` 监听 `address_standard_approve_v1` 流程事件，核心行为如下：

#### 5.2.1 提交或待办创建

当流程提交成功或待办创建时：

- 审批单状态更新为 `WAITING`
- 导入行状态同步为 `WAITING_APPROVAL`

#### 5.2.2 审批驳回/终止

当流程状态为驳回或终止：

- 审批单状态更新为 `REJECTED`
- 导入行状态更新为 `REJECTED_FAILED`
- `fail_reason = 驳回意见`
- 同时刷新批次汇总

#### 5.2.3 审批通过

当流程完成：

1. 先把审批单状态置为 `EXECUTING`
2. 调用 `approvalExecutor.execute(approval)`
3. 成功则：
   - 审批单状态置为 `APPROVED`
   - 导入行状态置为 `APPROVED_SUCCESS`
4. 失败则：
   - 审批单状态置为 `EXECUTE_FAILED`
   - 导入行状态置为 `EXECUTE_FAILED`
   - `fail_reason = 正式执行失败信息`

## 6. 正式入库链路

### 6.1 审批执行入口

审批通过后由 `StandardAddressApprovalExecutorImpl` 执行正式写入。

对于 `IMPORT` 操作：

- 如果 `request_payload.row` 不为空，走单行正式导入：
  - `StandardAddressImportService#executeApprovedImportRow(...)`
- 保留了整批导入能力，但当前上传阶段实际走的是“单行审批、单行执行”

### 6.2 审批通过后再次校验

`executeApprovedImportRow(...)` 内部仍然会再次执行 `prepareRow(...)`。

原因是：

- 上传提审和审批通过之间存在时间差
- 期间父级地址、字典、同名数据、管理站数据都可能变化

所以正式入库不是“直接信任提审时结果”，而是“基于审批快照重新校验一次当前库状态”。

### 6.3 正式写 `ADDR_SEGM`

正式入库调用的是：

- 新增：`StandardAddressCommandService#addStandardAddressForImport(...)`
- 更新：`StandardAddressCommandService#updateStandardAddressForImport(...)`

二者最终都收敛到 `buildEntity(...)`，核心写入规则如下：

#### 6.3.1 主键与层级

- 新增时 `segm_id` 由 `StandardAddressIdGenerator` 生成
- `segm_type` 取导入解析后的真实类型编码
- 父级必须存在且层级必须低于当前节点

#### 6.3.2 名称与简拼

- `stand_name = 父级stand_name + 当前segm_name`
- `segm_no = 当前名称简拼`
- `stand_no = 全称简拼`

名称简拼由 `StandardAddressNameService` 生成，规则包括：

- 中文转拼音首字母
- 字母统一大写
- 中文括号转英文括号

#### 6.3.3 区域与站点继承

默认情况下，子节点继承父节点：

- `region_id`
- `district_id`
- `service_region_id`

站点、接入属性、城乡属性、房屋属性、覆盖户数、工程编号等由导入行覆盖。

#### 6.3.4 更新时的级联刷新

更新标准地址后，如果名称或父级变化，`refreshChildrenStandInfo(...)` 会递归刷新所有子孙节点的：

- `stand_name`
- `stand_no`

### 6.4 ES 双写与补偿

标准地址正式写链路不会直接 `addrSegmMapper.insert/update` 后结束，而是经过 `AddressSearchSyncService`：

- 新增：`syncStandardCreate`
- 更新：`syncStandardUpdate`
- 删除：`syncStandardDelete`

当前策略是“先 ES、后 DB”：

1. 先写 ES
2. 再写数据库
3. 若数据库失败，尝试 ES 补偿
4. 补偿失败则写 repair 任务

因此导入审批通过后的副作用不仅有 `ADDR_SEGM`，还包括：

- 标准地址搜索索引
- 同步日志
- repair 任务

### 6.5 操作日志

导入正式成功后，会记录 `IMPORT` 类型操作日志，而不是普通 `INSERT/UPDATE` 日志。

日志内容包含：

- 导入新增还是导入更新
- 文件名
- Excel 行号

## 7. 表结构视角

## 7.1 核心表分层

按职责可分为四层：

| 层次 | 表 | 作用 |
| --- | --- | --- |
| 上传批次层 | `address_standard_import_batch` | 一个 Excel 文件一条记录 |
| 上传行结果层 | `address_standard_import_detail` | 一个 Excel 行一条记录 |
| 审批层 | `address_standard_approval` | 一个通过校验的 Excel 行一张审批单 |
| 正式数据层 | `ADDR_SEGM` | 审批通过后真正生效的标准地址事实表 |

支撑字典和解析的辅助表：

- `segm_addr_type`
- `pub_restriction`
- `spc_region`
- `spc_station`

### 7.2 `address_standard_import_batch`

该表的完整建表 SQL 未在当前目录直接找到，当前字段口径依据实体 `StandardAddressImportRecord` 与查询链路可确认如下：

| 字段 | 含义 |
| --- | --- |
| `id` | 批次主键 |
| `batch_no` | 批次号，格式 `STDADDRIMP{batchId}` |
| `file_name` | 导入文件名 |
| `status` | 批次状态：`0/1/2` |
| `total_count` | 总行数 |
| `success_count` | 成功行数 |
| `fail_count` | 失败行数 |
| `error_msg` | 首个错误信息 |
| `update_support` | 是否允许更新 |
| `del_flag` | 逻辑删除标记 |

批次状态说明：

- `0`：存在待审批行
- `1`：全部成功
- `2`：存在失败行

注意：批次状态最终以 `address_standard_import_detail` 聚合为准，不应只信任主表历史快照。

### 7.3 `address_standard_import_detail`

该表用于承接“单行导入生命周期”。

已知字段如下：

| 字段 | 含义 |
| --- | --- |
| `id` | 行结果主键 |
| `batch_id` | 所属导入批次 |
| `approval_id` | 审批单 ID |
| `approval_no` | 审批单号 |
| `approval_status` | 审批状态 |
| `row_num` | Excel 行号 |
| `file_name` | 导入文件名 |
| `update_support` | 是否允许更新 |
| `parent_stand_name` | 父级地址名称 |
| `segm_name` | 当级名称 |
| `segm_type` | 地址类型编码 |
| `addr_level` | 地址业务级别 |
| `status` | 行处理状态 |
| `fail_reason` | 失败原因 |
| `raw_payload` | 原始导入行 JSON |
| `del_flag` | 逻辑删除标记 |

行状态枚举：

- `VALIDATE_FAILED`
- `WAITING_APPROVAL`
- `APPROVED_SUCCESS`
- `REJECTED_FAILED`
- `EXECUTE_FAILED`

### 7.4 `address_standard_approval`

该表有完整 DDL，导入链路关注以下字段：

| 字段 | 含义 |
| --- | --- |
| `id` | 申请单 ID，同时也是 workflow `businessId` |
| `apply_no` | 申请单号 |
| `operation_type` | 导入场景固定为 `IMPORT` |
| `flow_code` | 流程编码 |
| `business_status` | workflow 业务状态 |
| `approval_status` | 页面归一化审批状态 |
| `instance_id` | 流程实例 ID |
| `current_task_id` | 当前待办任务 ID |
| `biz_title` | 业务标题 |
| `target_summary` | 导入场景通常是“第 N 行-地址名” |
| `request_payload` | 单行审批负载 JSON |
| `import_batch_payload` | 整批导入 JSON，当前单行审批链路基本不用 |
| `submit_fingerprint` | 防重复提交指纹 |
| `submit_guard_key` | 活跃提交流控键 |
| `approve_user_id/name/time` | 审批人信息 |
| `reject_reason` | 驳回意见 |
| `execute_message` | 正式执行结果 |

### 7.5 `ADDR_SEGM`

`ADDR_SEGM` 是正式生效的标准地址主事实表。导入链路涉及的关键字段如下：

| 字段 | 含义 | 导入来源 |
| --- | --- | --- |
| `segm_id` | 标准地址 ID | 系统生成或更新已有 |
| `parent_segm_id` | 父级地址 ID | 由父级解析得到 |
| `segm_name` | 当级名称 | Excel |
| `stand_name` | 标准地址全称 | 父级全称 + 当级名称 |
| `segm_no` | 当级简拼 | 系统计算 |
| `stand_no` | 全称简拼 | 系统计算 |
| `segm_type` | 地址类型 | `segm_addr_type` 映射 |
| `region_id` | 所属区域 | 默认继承父级 |
| `district_id` | 行政区域 | 默认继承父级 |
| `service_region_id` | 服务区域 | 默认继承父级 |
| `station_id` | 维修站 | 管理站匹配 |
| `installstation_id` | 安装站 | 管理站匹配 |
| `busstation_id` | 营业站 | 管理站匹配 |
| `status` | 状态 | 当前固定默认 `2140900` |
| `post_code` | 工程编号 | Excel |
| `is_city` | 是否城区 | Excel |
| `segm_name_fir` | 是否配套费小区 | Excel |
| `place_type` | 房屋属性 | 字典映射 |
| `cover_num` | 覆盖户数 | Excel |
| `addr_in_type_ftth` | 光纤接入方式 | 字典映射 |
| `ftth_pon_type` | 光纤接入能力 | 字典映射 |
| `area_type` | 城乡属性 | 字典映射 |
| `delete_state` | 删除标记 | 新增固定 `0` |

### 7.6 支撑字典表

#### `segm_addr_type`

用途：

- 根据“分段地址类型”名称反查 `addr_type_id`
- 通过 `level_id` 推导业务 `addrLevel`
- 控制一二级只读和父子层级关系

关键字段：

- `addr_type_id`
- `name`
- `level_id`

#### `pub_restriction`

用途：

- 把中文字典标签转成数据库存储值

导入中用到的 `keyword`：

- `ADDR_IN_TYPE_FTTH`
- `FTTH_PON_TYPE`
- `AREA_TYPE`
- `ADDR_PLACE_TYPE`

#### `spc_region`

用途：

- 作为一二级区域投影父节点解析来源

关键字段：

- `region_id`
- `region_name`
- `super_region_id`

#### `spc_station`

用途：

- 按区域和管理站类型匹配维修/安装/营业站

关键字段：

- `station_id`
- `china_name`
- `region_id`
- `manage_type`

## 8. 状态机与统计口径

### 8.1 单行状态机

```text
上传
  -> VALIDATE_FAILED         前置校验失败
  -> WAITING_APPROVAL        前置校验通过，已提审
      -> REJECTED_FAILED     审批驳回/终止
      -> EXECUTE_FAILED      审批通过但正式入库失败，或提审阶段审批单落库失败
      -> APPROVED_SUCCESS    审批通过且正式入库成功
```

### 8.2 批次聚合口径

`StandardAddressImportRecordServiceImpl#buildBatchSummary(...)` 的聚合规则是：

- `pendingCount = WAITING_APPROVAL`
- `successCount = APPROVED_SUCCESS`
- `failCount = VALIDATE_FAILED + REJECTED_FAILED + EXECUTE_FAILED`

批次状态计算规则：

1. 只要还有 `WAITING_APPROVAL`，批次就是 `PENDING(0)`。
2. 没有待审批且失败数大于 0，批次就是 `FAIL(2)`。
3. 没有待审批且失败数等于 0，批次就是 `SUCCESS(1)`。

## 9. 查询与失败导出链路

### 9.1 批次详情

`POST /address/import/batch/{batchId}`

返回 `StandardAddressImportBatchVo`，主要展示：

- 批次号
- 文件名
- 当前聚合状态
- 总数/成功数/失败数/待审批数
- 是否允许更新
- 首个错误信息

### 9.2 导入明细列表

`POST /address/import/batch/list`

底层 SQL 以 `address_standard_import_detail d left join address_standard_import_batch b` 为主，说明页面主视角是“导入行结果”，不是“批次主表”。

### 9.3 失败导出

`POST /address/import/batch/failure/export/{batchId}`

只导出以下状态的行：

- `VALIDATE_FAILED`
- `REJECTED_FAILED`
- `EXECUTE_FAILED`

不会导出 `WAITING_APPROVAL` 或 `APPROVED_SUCCESS`。

## 10. 关键业务约束与实现结论

### 10.1 明确结论

1. 当前标准地址导入是“按行审批”，不是“整批审批”。
2. 上传成功不代表正式地址已生效，只代表批次已创建且部分/全部行已进入审批。
3. 导入行结果表实际上承担了“导入明细 + 审批状态 + 最终执行结果”三重职责，是整条链路的核心事实表。
4. 批次表只是汇总表，最终状态必须以行结果聚合为准。
5. 正式写入 `ADDR_SEGM` 时仍然会重新校验一次当前库状态，避免审批期间数据漂移带来的脏写。
6. 导入正式成功后不仅写 MySQL，还会同步 ES 和操作日志。

### 10.2 需要特别注意的点

1. `successCount` 在上传接口返回时表示“提审成功数”，不是“最终生效数”，前端展示时不能误解。
2. 父级地址解析支持 `ADDR_SEGM` 和 `spc_region` 两套来源，联调时要确认模板里父级地址写的是全称口径。
3. 管理站匹配是模糊匹配取第一条，若同区域同类型下存在重名站点，存在误匹配风险。
4. 房屋属性解析时对 `ADDR_PLACE_TYPE` 做了 `ADDR_UNIT_TYPE` 回退，这属于兼容逻辑，不是纯粹单表口径。
5. 更新链路会递归刷新子孙节点 `stand_name/stand_no`，审批通过后的影响范围可能大于单行本身。

## 11. 代码定位建议

如需继续排查或扩展该链路，建议按以下顺序阅读源码：

1. `StandardAddressController#importStandardAddressData`
2. `StandardAddressServiceImpl#importStandardAddressData`
3. `StandardAddressImportService#validateImportRow / executeApprovedImportRow`
4. `StandardAddressApprovalService#submitImportRowApproval`
5. `StandardAddressWorkflowListener`
6. `StandardAddressApprovalExecutorImpl`
7. `StandardAddressCommandService`
8. `StandardAddressImportRecordServiceImpl`
9. `StandardAddressImportFailDetailMapper.xml`

以上九处基本覆盖了标准地址导入接口的完整主干链路。
