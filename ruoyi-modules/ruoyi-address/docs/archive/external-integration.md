# 外部系统对接清单（草案）

> 本文档只保留“对接对象、作用方向、责任边界”的摘要，便于跨系统沟通。正式接口能力、legacy 协议兼容和字段口径以 [interface-checklist.md](./interface-checklist.md) 为准。

## 目标与范围
- 标准地址服务需要对接：BOSS、工单系统、企业微信、大数据平台、地图系统。
- 本文档用于统一接口范围、同步策略与对接责任边界。
- 最新需求依据以 `/Users/criswu/Desktop/广电/需求/技术需求书_标准地址_待明确问题0323.docx` 及其备注为准。

## 对接清单

### 1. BOSS
- 作用：旧系统兼容入口、选址平台复用、安装地址变更通知、网格客户划分联调。
- 方向：
  - legacy 页面与 XML/SOAP 接口需兼容原协议。
  - 选址能力由地址服务提供给 BOSS 前台复用。
  - 安装地址修改后需主动通知 BOSS。
- 关键字段：`segmId`、`resObjectId`、`setAddrId`、`regionId`、`companyId`、`stationId`、`segmType`、`custId`、`setAddrName`、`FTTH_PON_TYPE`、`ADDR_IN_TYPE_FTTH`、`ADDR_IN_TYPE_LAN`。

### 2. 工单系统
- 作用：标准地址查询联调、异常地址修正工单、网格关联数据消费。
- 方向：
  - 地址服务向工单系统提供“先查小区、再返回楼栋供勾选”的标准地址查询能力。
  - 异常地址监控触发工单创建；工单系统回调处理状态。
  - 网格与客户、网格经理等关系数据按同步策略输出给工单系统。
- 关键字段：`standardAddressId`、`fullName`、`gridId`、`gridName`、`ruleId`、`remark`、`status`。

### 3. 企业微信
- 作用：选址平台复用、异常告警与通知推送、网格数据消费。
- 方向：
  - 地址服务向企业微信提供选址平台复用能力。
  - 地址服务向企业微信推送异常告警、通知消息和必要的网格同步数据。
- 关键字段：`standardAddressId`、`installAddressId`、`title`、`content`、`receiver`、`triggerTime`、`gridId`、`managerId`。

### 4. 大数据平台
- 作用：标准地址数据库查询、标准地址主数据同步、网格/客户/经理数据消费。
- 方向：
  - 标准地址需提供数据库查询接口或等价数据服务。
  - 网格与客户关联关系、网格信息、网格经理信息需按约定策略同步。
- 关键字段：`standardAddressId`、`fullName`、`level`、`code`、`regionCodes`、`gridId`、`customerId`、`managerId`、`syncTime`。

### 5. 地图系统
- 作用：选址平台地图圈选与地址解析（后续）。
- 方向：地图系统 -> 地址服务（解析结果）
- 关键字段：`geoCode`、`lat`、`lng`、`formattedAddress`。

## 待明确事项
- 各系统接口地址、鉴权方式、字段映射、回调机制。
- 同步策略（全量/增量/双向）与幂等方案。
- 异常重试与补偿策略。
- 工单系统、企业微信、大数据平台对“标准地址 / 安装地址 / 网格 / 经理 / 客户”各自消费范围的最终边界。
