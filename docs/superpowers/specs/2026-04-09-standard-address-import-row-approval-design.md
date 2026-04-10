# 标准地址导入单行审批设计

## 背景

当前标准地址导入接口只负责解析 Excel 并整体提交审批，业务校验主要发生在审批通过后的正式执行阶段。这会导致以下问题：

- 上传时无法逐条给出明确校验结果
- 同一批次中前置校验失败、审批驳回、执行失败无法统一归档
- `address_standard_import_fail_detail` 只能承载失败数据，无法覆盖“一条 Excel 记录一条审批记录”的全链路状态

## 目标

- 保留上传级 `batchId`
- 一条 Excel 记录对应一条审批记录
- 上传时先逐条前置校验
- 校验失败的行立即记入导入结果，并支持失败导出
- 校验成功的行发起单条审批
- 审批通过后正式导入成功，审批驳回或执行失败则落失败

## 数据模型

保留 `address_standard_import_record` 作为批次表。

升级 `address_standard_import_fail_detail` 为“导入行结果表”，继续沿用原表名，但语义扩展为记录每一行导入数据的生命周期。新增字段：

- `approval_id`
- `approval_no`
- `approval_status`
- `file_name`
- `update_support`

`status` 升级为行级状态：

- `VALIDATE_FAILED`
- `WAITING_APPROVAL`
- `APPROVED_SUCCESS`
- `REJECTED_FAILED`
- `EXECUTE_FAILED`

## 流程

### 上传阶段

1. 创建批次记录，状态初始为进行中。
2. 逐条执行前置校验，规则复用正式导入规则，不写正式地址表。
3. 校验失败：
   - 写入导入行结果表，状态 `VALIDATE_FAILED`
   - 记录 `fail_reason`
4. 校验成功：
   - 先写入导入行结果表，状态 `WAITING_APPROVAL`
   - 基于单条记录创建审批单
   - 回填 `approval_id/approval_no/approval_status`

上传接口返回：

- `successCount = 已发起审批数`
- `failCount = 前置校验失败数`

### 审批阶段

审批通过：

- 调用正式导入服务执行单行写入
- 导入成功后将该行更新为 `APPROVED_SUCCESS`

审批驳回：

- 将该行更新为 `REJECTED_FAILED`
- `fail_reason` 记录驳回原因

执行失败：

- 将该行更新为 `EXECUTE_FAILED`
- `fail_reason` 记录执行异常

## 批次统计

批次摘要不再只依赖批次表静态字段，而是以导入行结果表聚合为准：

- `totalCount = 行结果总数`
- `successCount = APPROVED_SUCCESS`
- `failCount = VALIDATE_FAILED + REJECTED_FAILED + EXECUTE_FAILED`
- `pendingCount = WAITING_APPROVAL`

批次状态规则：

- 存在 `WAITING_APPROVAL` 时为进行中
- 无待审批且失败数为 0 时为成功
- 无待审批且失败数大于 0 时为失败

## 兼容与导出

- 现有失败导出接口继续保留，但改为导出所有失败态记录
- 列表查询接口仍基于批次维度展示，失败明细按批次导出
- 表名暂不修改，后续可再做命名收敛

## 实现边界

- 本次不改“一次上传一个 batchId”的接口契约
- 本次不改审批页面基础结构，只补单行审批来源与批次关联
- 本次保留 `address_standard_import_fail_detail` 既有导出入口与权限点
