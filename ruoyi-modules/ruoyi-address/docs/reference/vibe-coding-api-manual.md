# 标准地址与网格模块 Vibe-Coding 接口册

> 本册用于 Controller/Service/BO/VO 落地时查参数和报文细节，不是项目阅读入口。若要先把握范围与重点，请先看 [README.md](../README.md)、[historical-schema-baseline.md](../historical-schema-baseline.md) 与 [interface-checklist.md](../interface-checklist.md)。

## 1. 使用说明

本文件面向后续 AI 辅助开发、详细设计、Controller/Service/Mapper 落地。

历史证据使用方式：

- `online-schema-comment-inventory-*` 与 `ftth-cloud-address-schema-handbook.md` 用于当前线上非备份表结构盘点，负责当前结构、字段注释和类型映射。
- 已归档的南京批次专题可补充历史值域、迁移链路、安装地址挂接现象和旧系统兼容现象；凡涉及迁移、治理、双向同步或 legacy 行为，不应完全跳过这类材料。
- 若两类材料与 `/Users/criswu/Desktop/广电/需求/技术需求书_标准地址_待明确问题0323.docx` 冲突，以 `0323` 需求书正文和备注为准。
- 标准地址区域主数据表名统一按 `spc_region` 处理；需求书中的旧写法统一视为同一对象，不再在新增文档、代码和 SQL 中继续扩散。

当前约束已经明确：

- 所有接口统一使用 `POST`
- 不再使用 `GET`
- 上述约束仅适用于本系统内部管理端接口；旧系统兼容接口需保持原文档要求的 `GET/URL/JSON/XML/SOAP` 协议与报文格式，不强行改造成 `POST`
- 统一响应继续使用仓库现有 `R<T>`
- 分页响应继续使用仓库现有 `TableDataInfo<T>`
- 分页参数继续使用仓库现有 `PageQuery`
- 查询类接口为了兼容当前基线签名，默认采用 `application/x-www-form-urlencoded`
- 新增、修改、绑定、合并、拆分等写接口默认采用 `application/json`
- 导入接口使用 `multipart/form-data`
- 导出接口返回文件流，不返回 JSON

### 1.1 字段命名约定

自本版起，本册中的标准地址 / 安装地址字段命名默认优先沿用线上库 canonical 字段或其明确 camelCase 变体；只有 legacy 对外兼容章节，才保留旧系统原始字段名。

| 场景 | 旧泛化命名 | 推荐命名 | 对应线上字段 | 释义 |
| --- | --- | --- | --- | --- |
| 标准地址主键 | `id` | `segmId` | `ADDR_SEGM.segm_id` | 标准地址 / 分段地址主键 |
| 标准地址父级 | `parentId` | `parentSegmId` | `ADDR_SEGM.parent_segm_id` | 上级分段地址 |
| 当级名称 | `name` | `segmName` | `ADDR_SEGM.segm_name` | 当前层级分段名称 |
| 标准地址全称 | `fullName` | `standName` | `ADDR_SEGM.stand_name` | 标准地址全称 |
| 简拼 / 编码 | `code`、`fullNameSimpleSpell` | `segmNo`、`standNo` | `ADDR_SEGM.segm_no`、`ADDR_SEGM.stand_no` | 分段地址简拼、标准地址简拼，不再混用成单一 `code` |
| 层级字段 | `level` | `segmType` + `levelId` | `ADDR_SEGM.segm_type` + `segm_addr_type.level_id` | 存储字段是地址类型，展示 / 过滤时再投影 `levelId` |
| 备注 | `remark` | `notes` | `ADDR_SEGM.notes` / `ADDR_SET_SEGM.notes` | 备注字段 |
| 标准地址管理站 | `maintenanceStationId/installStationId/businessStationId` | `stationId/installStationId/busStationId` | `ADDR_SEGM.station_id/installstation_id/busstation_id` | 维修 / 安装 / 营业管理站 |
| 接入能力 | `accessMethod/accessCapabilityCodes` | `addrInTypeFtth/ftthPonType/addrInTypeLan` | `ADDR_SEGM.addr_in_type_ftth/ftth_pon_type/addr_in_type_lan` | 光纤接入方式、光纤接入能力、电缆接入方式 |
| 城乡 / 房屋 / 覆盖 | `urbanRuralAttr/houseProperty/coverageHouseholds` | `areaType/placeType/coverNum` | `ADDR_SEGM.area_type/place_type/cover_num` | 城乡属性、场所性质、楼栋覆盖户数 |
| 工程编号 / 配套费 | `projectNumber/supportingFeeFlag` | `singleProjectCode/supportingFeeCommunityFlag` | `ADDR_SEGM.post_code/segm_name_fir` | 单项工程编号、是否配套费小区 |
| 安装地址主键 / 名称 | `id/installName` | `setAddrId/setAddrName` | `ADDR_SET_SEGM.set_addr_id/set_addr_name` | 安装地址主键、安装地址名称 |
| 安装地址编号 / 类型 | `code`、`resourceType` | `setAddrNo/setType` | `ADDR_SET_SEGM.set_addr_no/set_type` | 安装地址编号、安装地址类型 |
| 安装地址关联标准地址 | `standardAddressId` | `segmId` | `ADDR_SET_SEGM.segm_id` | 关联标准地址主键 |

若线上物理字段名本身存在历史遗留歧义，例如 `post_code`、`segm_name_fir`、`installstation_id`、`busstation_id`，本册采用“语义清晰的推荐名 + 显式标注物理字段”的写法，不再只保留容易误读的泛化变量名。

基线源码入口：

- 标准地址接口基线：
[StandardAddressAdminApi.java](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressAdminApi.java)
- 网格接口基线：
[GridManagementAdminApi.java](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/GridManagementAdminApi.java)
- 标准地址补充模型：
[StandardAddressAdminBo.java](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressAdminBo.java)
[StandardAddressAdminVo.java](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressAdminVo.java)
- 网格补充模型：
[GridManagementBo.java](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/GridManagementBo.java)
[GridManagementVo.java](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/GridManagementVo.java)

## 2. 全局协议

### 2.1 统一响应

成功响应示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

无返回值示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

### 2.2 分页响应

```json
{
  "code": 200,
  "msg": "查询成功",
  "total": 2,
  "rows": [
    {}
  ]
}
```

### 2.3 分页参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `pageNum` | `Integer` | 当前页，从 1 开始 |
| `pageSize` | `Integer` | 每页条数 |
| `orderByColumn` | `String` | 排序字段 |
| `isAsc` | `String` | 排序方向，`asc` / `desc` |

查询接口推荐提交格式：

```http
Content-Type: application/x-www-form-urlencoded
```

示例：

```http
pageNum=1&pageSize=20&orderByColumn=createTime&isAsc=desc
```

### 2.4 常见格式要求

| 项目 | 要求 |
| --- | --- |
| 时间 | `yyyy-MM-dd HH:mm:ss` |
| 状态字段 | 不允许一概默认写成 `0/1`；标准地址 / 安装地址优先使用线上字典值或明确枚举，例如 `ADDR_SEGM_STATUS=2140900/2140901`；监控、任务、组织等非地址资源再按各自章节约定处理 |
| 标准地址 / 安装地址主键 | 默认使用 `String`，按线上 `varchar(24)` 主键理解，对应 `segmId/setAddrId` |
| 其他资源主键 | 仅标签、导入记录、日志、监控规则、工单、组织、网格等明确为数值主键的资源才使用 `Long` |
| 批量主键 | 标准地址 / 安装地址批量路径参数使用逗号分隔的字符串主键，如 `segmId1,segmId2`、`setAddrId1,setAddrId2`；其余资源按各自真实主键类型处理 |
| 导入 | `multipart/form-data`，字段名统一为 `file` |
| 导出 | 直接输出文件流 |

### 2.5 需求书补充的数据库设计关键约束

下表摘自 `/Users/criswu/Desktop/广电/需求/技术需求书_标准地址_待明确问题0323.docx` 的标准地址模块、网格模块正文及备注，优先级高于当前页面 mock 表达。凡涉及表结构、唯一约束、同步策略和状态流转的内容，后续库表设计必须显式覆盖。

| 关键约束 | 需求书来源 | 对数据库设计的直接影响 | 已回填到接口说明 |
| --- | --- | --- | --- |
| 标准地址需兼容老系统，支持历史数据平滑迁移，并预留全量同步、增量同步、双向增量同步 | `2.3.5.5` 备注 | 建议主表或同步扩展表保留 `sourceSystem`、`sourceId`、`syncVersion`、`syncStatus`、`lastSyncTime`、`batchNo` 等字段，并设计同步日志 / outbox | 标准地址查询、新增、导入、安装地址、网格模块说明 |
| 所有查询类能力均需按 `tenantId`、`deptId` 等数据范围限制 | `2.3.5.5` 备注、`2.3.5.5.14` 备注、`2.3.5.5.17` 备注 | 业务主表需要具备租户、部门或组织维度字段及索引；接口层不一定显式传参，但数据层必须可过滤 | 标准地址查询、选址、安装地址查询、网格查询说明 |
| 标准地址列表/查询在 legacy 口径上，`1/2` 级行政区划来自 `spc_region`，其他级别来自 `addr_segm`；层级定义以 `segm_addr_type.level_id` 为准 | `2.3.5.5.1` 备注 | 查询实现需支持行政区划投影或兼容视图；`addr_segm` 侧层级条件统一作用于 `addr_segm.segm_type`，不再继续保留旧命名分支 | 标准地址列表、详情、选址说明 |
| `城区/非城区` 标识复用 `addr_segm.is_city`；`addr_segm.area_type` 表示城乡属性，枚举来自 `pub_restriction.keyword='AREA_TYPE'` | `2.3.5.5.1` 正文与备注 | 地址模型至少需要区分 `isCity` 与 `areaType` 两类字段，`8/9` 级规则不能再直接用城乡属性代替城区判定 | 地址新增、修改、拆分、查询说明 |
| 地址名称修改后，需级联刷新地址拼装名称、地址拼装简拼，以及所有下级地址和关联安装地址 | `2.3.5.5.3` 正文与备注 | 主表应围绕 `segmName/standName/segmNo/standNo` 建模；其中 `standNo` 对应线上 `ADDR_SEGM.stand_no`，生成规则固定为“中文转拼音首字母大写、中文括号转英文括号、数字不变”；安装地址表需保存关联地址快照或支持级联更新 | 标准地址修改说明、安装地址说明 |
| 一、二级标准地址由外部提供，不允许界面新增、修改、删除、合并、拆分 | `2.3.5.5.2` 备注 | 建议保留只读标识、数据来源标识或系统级保护标识，并在操作权限位中显式返回不可编辑、不可合并、不可拆分 | 标准地址新增、修改、删除、详情说明 |
| 标准地址批量新增场景的父级地址选择需复用名称模糊查询接口，前端单次候选限制 `200` 条 | `2.3.5.5.2` 备注 | 搜索接口需支持作为批量新增的父级选择器复用，并在接口侧或页面侧设置数量上限，避免超大候选集 | 批量下级地址预览、批量新增说明 |
| 删除需区分逻辑删除 / 物理删除，并保留审计；校验顺序是“下级地址/关联资源”优先，“安装地址”二次确认其次 | `2.3.5.5.6` 正文备注、`2.8.4.29` | 主表建议保留逻辑删除标志、删除时间、删除人；审计日志不可省略；资源关联需有可反查关系 | 标准地址删除、操作日志说明 |
| 标准地址合并只能高等级地址合并低等级地址，同级禁止；合并时先迁移下级地址，再删除源地址；若全局拼装地址重名需显式提示 | `2.3.5.5.7` 备注 | 等级比较必须按真实 `segm_addr_type.level_id` 执行，同时需要父子树关系可重挂接，建议有合并关系日志或历史映射表，保证追溯，并在唯一性校验层支持“合并前重名预警” | 标准地址合并说明 |
| 标准地址拆分后级别不变，源地址复制出多个同级地址，属性默认继承不变，源地址默认回收 | `2.3.5.5.8` 备注 | 拆分结果需能继承或复制源地址的层级、部分属性及来源关系；建议保留拆分来源标识与源地址回收状态 | 标准地址拆分说明 |
| 标准地址导出需支持“导出本页数据”“导出选中数据”“按用户选择字段导出”三种模式 | `2.3.5.5.5` 备注 | 导出接口除筛选条件外，还需承载当前页 / 选中 ID / 字段选择等信息 | 标准地址导出说明 |
| 导入记录需按“每一条导入数据”记明细，而不是仅按批次记；失败数据返回 Excel | `2.3.5.5.11` 备注 | 设计按 `import_batch` + `import_detail` 两层表收口；明细表以单条地址为粒度记录状态、错误原因、操作人，并保留失败明细下载能力；当前阶段不提供部分成功数据回滚 | 导入记录、失败导出、导入说明 |
| 标准地址所属管理站分维修、安装、营业三类，单地址最多同时归属三种不同管理站；历史主数据当前参考 `spc_station` 并按 `region_id` 划分 | `2.3.5.5.15` 备注 | 建议使用关系表并以 `address_id + station_type` 唯一约束实现“一类一个”；管理站主表需兼容“源表无类型、关系表补业务角色”的现状，类型字典取 `pub_restriction.keyword='MANAGE_TYPE'`（`2017101/2017102/2017103`） | 标准地址详情/新增/修改补充说明 |
| 标准地址属性包含接入方式、接入能力、城乡属性、房屋属性、覆盖户数、工程编号、是否配套费小区等 | `2.3.5.5.16` | 建议属性扩展表一对一承载；其中“接入能力”可能为多选，优先考虑关系表或数组 / JSON 字段 | 标准地址详情/新增/修改补充说明 |
| 安装地址与标准地址是分开的实体；`0323` 备注明确安装地址默认关联标准地址，并要求保留双向同步能力 | `2.3.5.5.17` 正文与备注 | 安装地址表应独立建表，并围绕 `setAddrId/setAddrName/setType/segmId/segmType/regionId/orgId` 等线上字段设计；`associationStatus` 属于派生查询口径，不应替代 `segm_id` 作为主关联字段；业务接口默认写入 `BOUND`，仅迁移/治理态允许保留 `UNBOUND` 视图 | 安装地址查询、删除、修改说明 |
| 选址场景从楼栋生成新标准地址时，需区分普通 `房间` 与 `尾级地址（选址生成）` 两类类型，不允许直接硬编码为 `16-房间` | `2.3.5.5.14`、`2.3.5.5.1` | 生成前需根据 `segm_addr_type`、legacy 创建接口报文和联调库字典核定真实落点；若业务要求生成选址尾级地址，应优先映射到对应尾级类型 | 选址创建房间地址、旧标准地址创建兼容说明 |
| 网格地址划分按楼栋级标准地址做一对一归属，楼栋下所有客户自动划入该网格；若与客户手工绑定冲突，以客户绑定为准 | `2.3.5.6.6`、`2.3.5.6.7` | `grid_address_rel` 需对楼栋级地址唯一，但“楼栋级”必须通过 `segm_addr_type` / 类型映射解析；`grid_customer_rel` 需保留绑定来源与优先级，显式覆盖地址派生结果 | 网格地址绑定、网格客户绑定说明 |
| 网格模块同样需要历史迁移与双向增量同步 | `2.3.5.6` 备注 | 网格、网格客户、网格经理关系表均应具备同步字段与变更日志 | 网格修改、网格地址导入、网格外部接口说明 |
| 网格组织查询仅返回当前租户可见组织；删除组织需先校验无子节点；网格仅支持在广电站节点下新增；删除网格需校验地址划分与手工划分两类客户来源；网格客户列表默认仅展示手工划分数据 | `2.3.5.6.1`、`2.3.5.6.3`、`2.3.5.6.5`、`2.3.5.6.7` | 组织树、网格主表、客户关系表都需要显式区分来源、组织层级和删除拦截条件 | 网格组织、网格新增/删除、网格客户说明 |
| 新系统需兼容老系统标准地址接口能力，保留 legacy `segmId/resObjectId/setAddrId/regionId/companyId/stationId/segmType` 字段口径 | 旧接口兼容文档 | 主表、安装地址、管理站和组织映射需要保留 legacy 标识，且查询结果可直接组装老接口报文 | 旧系统兼容接口章节 |
| 旧系统接口同时存在页面 URL、JSON、XML、SOAP 四类协议 | 旧接口兼容文档 | 需要增加协议适配层、原始报文日志、重试与回放能力，不宜直接复用内部 Controller 入参模型 | 旧系统兼容接口章节 |
| 老接口中的接入能力字段拆分为 `FTTH_PON_TYPE`、`ADDR_IN_TYPE_FTTH`、`ADDR_IN_TYPE_LAN` 及对应字典 ID | 旧接口兼容文档 | 现有“接入方式 + 接入能力”抽象需要补齐更细粒度字段，否则无法无损组装旧报文 | 标准地址扩展信息、安装地址、旧系统兼容接口章节 |

### 2.6 历史证据基线补充

- `spc_station`、`spc_region`、`segm_addr_type`、`ADDR_SEGM.is_city`、`pub_restriction` 当前都可以与 `0323` 需求书合并引用，分别支撑管理站主数据、区域主数据、层级规则、城区标识和字典值说明。
- 合并引用时，字段结构与注释优先取历史盘点 / 结构手册，业务约束优先取 `0323` 需求书备注，例如“按 `region_id` 划分管理站”“管理站类型来自 `MANAGE_TYPE`”“`1/2` 级查询按 `spc_region` 口径”“`8/9` 级区分要看 `is_city` 而不是 `area_type`”。
- 当前文档、代码与 SQL 一律统一使用 `spc_region`，不再继续保留旧命名分支。

### 2.7 全局实现约束

- `ruoyi-address` 必须能作为独立能力系统运行，并向大数据平台、工程系统、BOSS、工单系统、企业微信等外围系统提供地址、网格、选址和查询能力。
- 标准地址、安装地址、网格接口都不是单纯后台页面接口，设计时需同步考虑资源配置、网络激活、群障识别、客服拦截和外围系统复用。
- 接口层和数据层都需要满足权限控制：至少支持菜单级 RBAC，并对查询类能力统一施加 `tenantId/deptId` 与数据权限范围过滤。
- 需保留完整接口说明文档，并对本系统接口与外联系统平台接口提供健康度监控报警能力。
- 日志与安全要求需内化到实现：至少三个月日志审计、HTTPS、敏感配置加密、输入校验、原始报文留痕和异常可追溯。
- 实现尽量采用标准 SQL 和通用 JDBC 能力，避免把 Controller/Service/Mapper 设计绑死到单一数据库方言。

### 2.8 标后测试与交付门槛

- 中标公告结束后 `5` 个工作日内需完成测试环境搭建和应用部署。
- 正式测试时间 `3` 小时。
- 共 `33` 个测试项，其中带 `★` 的测试项必须全部通过，总通过数不得少于 `30` 项。
- 对本模块实现最直接的验收压力集中在 `2.8.4.12` 到 `2.8.4.31`，对应标准地址、安装地址、非标监控、选址、管理站、属性、网格和系统接口。
- 因此后续代码实现不应只满足“接口能调通”，还要提前按测试步骤组织页面、导出、导入、详情展示、联动刷新和接口配置能力。

## 3. 参数与数据模型速查

### 3.1 阅读约定

- 本节专门回答三个问题：这个接口能传哪些字段、哪些字段必填、字段格式应该是什么。
- `必填/选填` 以当前后端接口合同口径为准，不只看 Java 注解；部分 BO 虽然暂未加校验注解，但业务上仍按“必填”执行。
- 列表查询和导出筛选默认复用同一套筛选字段；列表接口还需要额外叠加 [第 2.3 节](#23-分页参数) 的分页参数。
- 详情、删除、启停、失败明细导出这类路径参数统一看 [第 3.2 节](#32-通用路径分页与表单参数)。
- `条件必填` 表示是否必须传入取决于当前接口或其他字段取值，例如“二选一”“指定范围时必填”。
- 查询接口若使用 `application/x-www-form-urlencoded`，字段名与下文模型字段名完全一致。

### 3.2 通用路径、分页与表单参数

| 字段 | 位置 | 类型 | 必填 | 格式说明 | 适用场景 | 示例 |
| --- | --- | --- | --- | --- | --- | --- |
| `pageNum` | form | `Integer` | 列表接口建议必填 | 从 `1` 开始的正整数 | 分页查询 | `1` |
| `pageSize` | form | `Integer` | 列表接口建议必填 | 建议 `1-200` | 分页查询 | `20` |
| `orderByColumn` | form | `String` | 选填 | 白名单排序字段 | 分页查询 | `createTime` |
| `isAsc` | form | `String` | 选填 | `asc` 或 `desc` | 分页查询 | `desc` |
| `id` | path | `Long` | 必填 | 正整数主键 | 标签、监控规则、日志、工单、组织、网格等明确数值主键资源的详情或单对象操作 | `10001` |
| `ids` | path | `Long[]` | 必填 | 逗号分隔的 `Long` 集合 | 标签、监控任务、异常忽略等明确数值主键资源的批量操作 | `10001,10002` |
| `batchId` | path | `Long` | 必填 | 正整数主键 | 导入批次详情、失败明细导出 | `9001` |
| `segmId` | path | `String` | 必填 | `varchar(24)` 字符串主键 | 标准地址详情、标准地址单对象操作 | `000102010000000011800001` |
| `segmIds` | path | `String` | 必填 | 逗号分隔的 `varchar(24)` 字符串主键集合 | 标准地址批量删除 | `000102010000000011800001,000102010000000011800002` |
| `setAddrId` | path | `String` | 必填 | `varchar(24)` 字符串主键 | 安装地址详情、安装地址单对象操作 | `000102010000000099900001` |
| `setAddrIds` | path | `String` | 必填 | 逗号分隔的 `varchar(24)` 字符串主键集合 | 安装地址批量删除 | `000102010000000099900001,000102010000000099900002` |
| `standardAddressId` / `segmId` | path | `String` | 必填 | `varchar(24)` 字符串主键 | 地址标签查询、选址预览、房间列表；路由占位名可能仍写 `standardAddressId`，语义按 `segmId` 理解 | `000102010000000011800001` |
| `confirm` | query | `Boolean` | 选填 | `true` / `false`，默认 `false` | 删除标准地址时确认是否继续处理关联 | `false` |
| `updateSupport` | query | `Boolean` | 选填 | `true` / `false`，默认 `false` | 导入类接口是否允许覆盖更新 | `true` |
| `file` | multipart | `File` | 导入接口必填 | `multipart/form-data`，推荐 `xlsx/xls` | 各类导入接口 | `standard-address.xlsx` |

通用表单示例：

```http
pageNum=1&pageSize=20&orderByColumn=createTime&isAsc=desc
```

### 3.3 标准地址主资源参数

#### `StandardAddressBo`

| 字段 | 类型 | 查询 | 新增 | 修改 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `segmId` | `String` | 选填 | 不使用 | 必填 | `varchar(24)` 字符串主键 | 对应 `ADDR_SEGM.segm_id`，标准地址 / 分段地址主键 | `000102010000000011800001` |
| `parentSegmId` | `String` | 选填 | 必填 | 选填 | `varchar(24)` 字符串主键 | 对应 `ADDR_SEGM.parent_segm_id`，上级分段地址 | `000102010000000011800000` |
| `segmName` | `String` | 选填 | 必填 | 必填 | 非空字符串，建议不超过 200 字符 | 对应 `ADDR_SEGM.segm_name`，当前层级分段地址名称 | `101室` |
| `segmNo` | `String` | 选填 | 选填 | 选填 | 普通字符串 | 对应 `ADDR_SEGM.segm_no`，分段地址简拼 | `101S` |
| `standName` | `String` | 选填 | 选填 | 选填 | 完整地址字符串 | 对应 `ADDR_SEGM.stand_name`，标准地址全称；写接口通常由后端拼装 | `江苏省南京市鼓楼区中央路88号1单元101室` |
| `standNo` | `String` | 选填 | 自动生成 | 自动生成 | 普通字符串 | 对应 `ADDR_SEGM.stand_no`，标准地址简拼 | `JSSNJSNJSQGLQZYL88H1DY101S` |
| `segmType` | `Integer` | 选填 | 选填 | 选填 | `segm_addr_type.addr_type_id` | 对应 `ADDR_SEGM.segm_type`，真实存储的是地址类型 ID，而不是展示层级 | `180007` |
| `levelId` | `Integer` | 选填 | 选填 | 选填 | `segm_addr_type.level_id` | 由 `segmType` 投影得到的真实层级值，用于展示和规则校验 | `16` |
| `regionId` | `String` | 选填 | 选填 | 选填 | `varchar(24)` 字符串 ID | 对应 `ADDR_SEGM.region_id`，所属管理区域 | `000102010000000011823409` |
| `districtId` | `String` | 选填 | 选填 | 选填 | `varchar(24)` 或扩展区域标识 | 对应 `ADDR_SEGM.district_id`，行政区域 | `320106` |
| `serviceRegionId` | `String` | 选填 | 选填 | 选填 | `varchar(24)` 字符串 ID | 对应 `ADDR_SEGM.service_region_id`，所属社区 | `320106001001` |
| `status` | `Integer` | 选填 | 选填 | 选填 | `pub_restriction.serial_no(keyword='ADDR_SEGM_STATUS')` | 地址状态；线上主值如 `2140900=有效`、`2140901=无效` | `2140900` |
| `notes` | `String` | 不使用 | 选填 | 选填 | 普通文本，建议不超过 255 字符 | 对应 `ADDR_SEGM.notes`，备注说明 | `测试地址` |

需求书补充字段与线上主字段扩展（当前尚未全部同步到 Java BO，但库表设计和接口实现需预留）：

| 字段 | 类型 | 查询 | 新增/修改 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- |
| `isCity` | `String` | 选填 | 选填 | `Y/N`、`1/0` 或兼容历史值 | 对应 `ADDR_SEGM.is_city`，`城区/非城区` 标识 | `Y` |
| `stationId` | `String` | 选填 | 选填 | `varchar(24)` 字符串 ID | 对应 `ADDR_SEGM.station_id`，地址所属维修管理站 | `000102010000000011823409` |
| `installStationId` | `String` | 选填 | 选填 | `varchar(24)` 字符串 ID | 对应 `ADDR_SEGM.installstation_id`，地址所属安装管理站 | `000102010000000011823410` |
| `busStationId` | `String` | 选填 | 选填 | `varchar(24)` 字符串 ID | 对应 `ADDR_SEGM.busstation_id`，地址所属营业管理站 | `000102010000000011823411` |
| `addrInTypeFtth` | `Integer` | 选填 | 选填 | `pub_restriction.serial_no(keyword='ADDR_IN_TYPE_FTTH')` | 光纤接入方式 | `2140760` |
| `ftthPonType` | `Integer` | 选填 | 选填 | `pub_restriction.serial_no(keyword='FTTH_PON_TYPE')` | 光纤接入能力 | `2141301` |
| `addrInTypeLan` | `Integer` | 选填 | 选填 | `pub_restriction.serial_no(keyword='ADDR_IN_TYPE_LAN')` | 电缆接入方式 | `2140784` |
| `areaType` | `Integer` | 选填 | 选填 | `pub_restriction.serial_no(keyword='AREA_TYPE')` | 对应 `ADDR_SEGM.area_type`，城乡属性字段 | `2140511` |
| `placeType` | `Integer` | 选填 | 选填 | `pub_restriction.serial_no(keyword='ADDR_PLACE_TYPE')` | 对应 `ADDR_SEGM.place_type`，场所性质 / 房屋属性 | `2140800` |
| `coverNum` | `Integer` | 选填 | 选填 | 非负整数 | 对应 `ADDR_SEGM.cover_num`，楼栋覆盖户数 | `128` |
| `singleProjectCode` | `String` | 选填 | 选填 | 普通字符串 | 对应 `ADDR_SEGM.post_code`，字段注释为“单项工程编号” | `GC-2026-001` |
| `supportingFeeCommunityFlag` | `String` | 选填 | 选填 | `Y/N` 或兼容历史值 | 对应 `ADDR_SEGM.segm_name_fir`，字段注释为“是否配套费小区” | `Y` |

说明：

- `stationId/installStationId/busStationId` 的候选主数据当前参考历史库 `spc_station`，并结合 `region_id` 做组织范围约束；管理站类型字典来自 `pub_restriction.keyword='MANAGE_TYPE'`（`2017101=维修`、`2017102=安装`、`2017103=营业`）；源数据暂不区分业务类型，最终类型以地址和管理站关系表中的业务角色为准。
- 旧文档中的 `code/fullNameSimpleSpell/accessMethod/accessCapabilityCodes/urbanRuralAttr/houseProperty/coverageHouseholds/projectNumber/supportingFeeFlag` 不再作为主命名；若历史接口暂时保留这些字段名，必须在代码或适配层标注其对应物理字段。
- 旧文档中的 `provinceCode/cityCode/districtCode/streetCode/villageCode` 不是 `ADDR_SEGM` 当前主表的默认直存字段；若页面仍需使用，建议通过 `spc_region` 或额外区域映射投影返回，不再默认放入标准地址主 BO。

补充说明：标准地址列表若筛选或返回 `1/2` 级行政区划，统一按 `spc_region` 口径处理；其他级别按 `addr_segm` / 标准地址事实表处理，层级定义统一以 `segm_addr_type.level_id` 为准，且 `addr_segm` 侧层级条件应用于 `addr_segm.segm_type`。

地址层级口径（摘自 `0323` 需求书，直接影响 `level` 含义）：

- 底层结构基线：需求书备注明确“地址层级不参考下面表格；以 `segm_addr_type` 为准，层级按照 `level_id` 字段排序得出”。因此接口中的 `level` 主语义应理解为真实结构层级。
- 当前需求书列出的底层地址类型序列如下：

| `level` | 地址类型 | 备注 |
| --- | --- | --- |
| `1` | 省、自治区 | 行政区划 |
| `2` | 市 | 行政区划 |
| `3` | 市区 | 行政区划 |
| `4` | 县、区 | 行政区划 |
| `5` | 乡、镇、街道 | 行政区划 / 基础地域 |
| `6` | 村 | 非城区常见层级 |
| `7` | 路 | 基础道路层级 |
| `8` | 庄、组、队 | 非城区扩展层级 |
| `9` | 建筑群、小区 | 园区 / 小区 / 建筑群 |
| `10` | 期、区 | 小区内期区等层级 |
| `11` | 建筑 | 楼栋业务语义通常落在这一类 |
| `12` | 横向建筑 | 历史结构扩展层级 |
| `13` | 建筑单元 | 单元层级 |
| `14` | 层、楼 | 楼层层级 |
| `15` | 门牌号 | 门牌层级 |
| `16` | 房间 | 房间层级 |
| `17` | 伪地址 | 历史兼容层级 |
| `18` | 地址补充描述 | 地址补充信息 |
| `19` | 尾级地址（选址生成） | 选址场景生成的尾级地址 |

完整请求体示例：

```json
{
  "segmId": "000102010000000011800001",
  "parentSegmId": "000102010000000011800000",
  "segmName": "101室",
  "segmNo": "101S",
  "standName": "江苏省南京市鼓楼区中央路88号1单元101室",
  "standNo": "JSSNJSNJSQGLQZYL88H1DY101S",
  "segmType": 180007,
  "levelId": 16,
  "regionId": "000102010000000011823409",
  "districtId": "320106",
  "serviceRegionId": "320106001001",
  "status": 2140900,
  "notes": "测试地址"
}
```

返回补充字段：

| 字段 | 类型 | 格式说明 | 说明 |
| --- | --- | --- | --- |
| `createDate` | `Date` | `yyyy-MM-dd HH:mm:ss` | 创建日期，对应 `ADDR_SEGM.create_date` |
| `tagNames` | `List<String>` | 字符串数组 | 地址标签名称列表 |
| `stationName` | `String` | 普通字符串 | 维修管理站名称 |
| `installStationName` | `String` | 普通字符串 | 安装管理站名称 |
| `busStationName` | `String` | 普通字符串 | 营业管理站名称 |

#### `StandardAddressMergeBo`

| 字段 | 类型 | 必填 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `sourceSegmIds` | `List<String>` | 必填 | `varchar(24)` 字符串数组，至少 1 条 | 待合并的源标准地址集合 | `["000102010000000011800001", "000102010000000011800002"]` |
| `targetSegmId` | `String` | 必填 | `varchar(24)` 字符串主键 | 合并目标地址 | `000102010000000011800003` |

```json
{
  "sourceSegmIds": [
    "000102010000000011800001",
    "000102010000000011800002"
  ],
  "targetSegmId": "000102010000000011800003"
}
```

#### `StandardAddressSplitBo`

| 字段 | 类型 | 必填 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `sourceSegmId` | `String` | 必填 | `varchar(24)` 字符串主键 | 被拆分的源地址 | `000102010000000011800003` |
| `splitItems` | `List<StandardAddressSplitItemBo>` | 必填 | 至少 1 条 | 拆分后的新地址最小配置项列表 | 见下方 |

`splitItems[]` 元素字段口径：

| 字段 | 类型 | 必填 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `segmName` | `String` | 必填 | 非空字符串 | 新地址名称 | `1单元` |

```json
{
  "sourceSegmId": "000102010000000011800003",
  "splitItems": [
    {
      "segmName": "1单元"
    },
    {
      "segmName": "2单元"
    }
  ]
}
```

#### `StandardAddressBatchAddBo`

| 字段 | 类型 | 预览 | 批量新增 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- |
| `parentSegmId` | `String` | 必填 | 必填 | `varchar(24)` 字符串主键 | 批量生成的父级地址 | `000102010000000011800010` |
| `prefix` | `String` | 必填 | 必填 | 非空字符串 | 名称前缀 | `第` |
| `startNum` | `Integer` | 必填 | 必填 | 正整数 | 起始编号 | `1` |
| `endNum` | `Integer` | 必填 | 必填 | 正整数，且 `endNum >= startNum` | 结束编号 | `3` |
| `suffix` | `String` | 选填 | 选填 | 普通字符串 | 名称后缀 | `单元` |

```json
{
  "parentSegmId": "000102010000000011800010",
  "prefix": "第",
  "startNum": 1,
  "endNum": 3,
  "suffix": "单元"
}
```

### 3.4 标签参数

#### `StandardAddressTagBo`

| 字段 | 类型 | 查询 | 新增 | 修改 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` | `Long` | 选填 | 不使用 | 必填 | 正整数主键 | 标签主键 | `1` |
| `name` | `String` | 选填 | 必填 | 必填 | 非空字符串，建议不超过 32 字符 | 标签名称 | `重点覆盖` |
| `code` | `String` | 选填 | 选填 | 选填 | 英文、数字、下划线组合 | 标签编码 | `KEY_COVER` |
| `color` | `String` | 不使用 | 选填 | 选填 | `#RRGGBB` | 标签颜色 | `#409EFF` |
| `remark` | `String` | 不使用 | 选填 | 选填 | 普通文本 | 备注 | `重点场景标签` |

```json
{
  "id": 1,
  "name": "重点覆盖",
  "code": "KEY_COVER",
  "color": "#409EFF",
  "remark": "重点场景标签"
}
```

#### `StandardAddressTagBindBo`

| 字段 | 类型 | 绑定 | 解绑 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- |
| `segmIds` | `List<String>` | 必填 | 必填 | `varchar(24)` 字符串数组，至少 1 条 | 待处理的标准地址集合 | `["000102010000000011800001", "000102010000000011800002"]` |
| `tagIds` | `List<Long>` | 必填 | 必填 | `Long` 数组，至少 1 条 | 待绑定或解绑的标签集合 | `[1, 2]` |

```json
{
  "segmIds": [
    "000102010000000011800001",
    "000102010000000011800002"
  ],
  "tagIds": [1, 2]
}
```

### 3.5 导入记录与操作日志参数

#### `StandardAddressImportRecordBo`

| 字段 | 类型 | 列表查询 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `id` | `Long` | 选填 | 正整数主键 | 导入记录主键 | `9001` |
| `fileName` | `String` | 选填 | 文件名字符串 | 导入文件名称 | `standard-address-20260319.xlsx` |
| `status` | `String` | 选填 | `0` 进行中，`1` 成功，`2` 失败 | 导入状态 | `1` |
| `errorMsg` | `String` | 选填 | 普通文本 | 失败原因模糊检索 | `第 12 行名称为空` |
| `createBy` | `Long` | 选填 | 正整数用户主键 | 导入发起人 | `200001` |

查询示例：

```http
fileName=standard-address&status=1&pageNum=1&pageSize=20
```

需求书备注补充：

- 导入记录粒度应细化到“每一条导入数据”而不是仅按 Excel 批次展示。
- 如果 Excel 中有 `50` 条数据，导入记录查询中应能看到 `50` 条明细记录。
- 因此库表层推荐拆分为“导入批次表 + 导入明细表”，接口列表默认查明细，必要时再按批次聚合展示。

#### `StandardAddressOperationLogBo`

| 字段 | 类型 | 列表查询 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `id` | `Long` | 选填 | 正整数主键 | 日志主键 | `7001` |
| `segmId` | `String` | 选填 | `varchar(24)` 字符串主键 | 关联标准地址 | `000102010000000011800001` |
| `operationType` | `String` | 选填 | 枚举字符串 | `MERGE`、`SPLIT`、`DELETE`、`IMPORT`、`UPDATE`、`INSERT` | `MERGE` |
| `operator` | `String` | 选填 | 登录名或姓名 | 操作人 | `zhangsan` |
| `details` | `String` | 选填 | 普通文本 | 操作详情关键字 | `合并到目标地址` |

查询示例：

```http
segmId=000102010000000011800001&operationType=MERGE&pageNum=1&pageSize=20
```

### 3.6 安装地址参数

#### `InstallationAddressBo`

| 字段 | 类型 | 查询 | 新增 | 修改 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `setAddrId` | `String` | 选填 | 不使用 | 必填 | `varchar(24)` 字符串主键 | 对应 `ADDR_SET_SEGM.set_addr_id`，安装地址主键 | `000102010000000099900001` |
| `setAddrName` | `String` | 选填 | 必填 | 必填 | 非空字符串，建议不超过 400 字符 | 对应 `ADDR_SET_SEGM.set_addr_name`，安装地址名称 | `中央路88号1单元101室弱电箱` |
| `setAddrNo` | `String` | 选填 | 选填 | 选填 | 普通字符串 | 对应 `ADDR_SET_SEGM.set_addr_no`，安装地址编号 | `SET-320106-0001` |
| `setType` | `Integer` | 选填 | 选填 | 选填 | `0=伪地址`、`2=到户地址` | 对应 `ADDR_SET_SEGM.set_type`，安装地址类型 | `2` |
| `segmId` | `String` | 选填 | 选填 | 选填 | `varchar(24)` 字符串主键 | 对应 `ADDR_SET_SEGM.segm_id`，关联标准地址主键 | `000102010000000011800001` |
| `segmType` | `Integer` | 选填 | 选填 | 选填 | `segm_addr_type.addr_type_id` | 对应 `ADDR_SET_SEGM.segm_type`，关联地址分段类型 | `180007` |
| `regionId` | `String` | 选填 | 选填 | 选填 | `varchar(24)` 字符串 ID | 对应 `ADDR_SET_SEGM.region_id`，所属区域 ID | `000102010000000011823409` |
| `orgId` | `String` | 选填 | 选填 | 选填 | `varchar(80)` 字符串 ID | 对应 `ADDR_SET_SEGM.org_id`，组织机构 ID | `320106001` |
| `bossOp` | `String` | 选填 | 选填 | 选填 | 普通字符串 | 对应 `ADDR_SET_SEGM.boss_op`，BOSS 操作人 | `BOSS001` |
| `notes` | `String` | 不使用 | 选填 | 选填 | 普通文本 | 对应 `ADDR_SET_SEGM.notes`，备注 | `FTTH入户安装` |

需求书补充字段与派生查询字段（当前尚未全部同步到 Java BO，但接口设计需预留）：

| 字段 | 类型 | 查询 | 新增/修改 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- |
| `associationStatus` | `String` | 选填 | 选填 | `BOUND/UNBOUND` 或等价字典值 | 按是否关联标准地址区分查询 | `BOUND` |
| `deviceId` | `String` | 选填 | 选填 | 设备编码字符串 | 需求书要求可按设备 ID 查询 | `DEV-001` |
| `syncDate` | `Date` | 选填 | 选填 | `yyyy-MM-dd HH:mm:ss` | 对应 `ADDR_SET_SEGM.synchronous_date`，最近同步时间 | `2026-03-18 10:00:00` |
| `oldSegmId` | `String` | 选填 | 选填 | `varchar(24)` 字符串主键 | 对应 `ADDR_SET_SEGM.old_segm_id`，旧标准地址 ID | `000102010000000011700001` |

说明：

- 当前线上 `ADDR_SET_SEGM` 主表中没有 `resourceId/resourceType/installTime/bossSyncFlag` 这类标准资源字段；若后续业务需要承载设备、资源或同步状态，应通过外围关联表、扩展表或适配层补充，不应误写成当前主表默认字段。
- 旧文档中的 `standardAddressId/installName/remark` 不再作为安装地址主模型默认命名，统一收口为 `segmId/setAddrName/notes`。

```json
{
  "setAddrId": "000102010000000099900001",
  "setAddrName": "中央路88号1单元101室弱电箱",
  "setAddrNo": "SET-320106-0001",
  "setType": 2,
  "segmId": "000102010000000011800001",
  "segmType": 180007,
  "regionId": "000102010000000011823409",
  "orgId": "320106001",
  "bossOp": "BOSS001",
  "notes": "FTTH入户安装"
}
```

返回补充字段：

| 字段 | 类型 | 格式说明 | 说明 |
| --- | --- | --- | --- |
| `hasStandardAddress` | `Boolean` | `true/false` | 是否已经关联标准地址 |
| `standName` | `String` | 完整地址字符串 | 关联标准地址全称，对应 `ADDR_SEGM.stand_name` |
| `createDate` | `Date` | `yyyy-MM-dd HH:mm:ss` | 创建日期，对应 `ADDR_SET_SEGM.create_date` |
| `deviceId` | `String` | 设备编码字符串 | 关联设备 ID |
| `deviceName` | `String` | 普通字符串 | 设备名称，需求备注说明由 BOSS 提供 |
| `associationStatus` | `String` | 状态字典值 | 关联状态 |

### 3.7 监控规则、任务、异常记录与工单参数

#### `StandardAddressMonitorRuleBo`

| 字段 | 类型 | 查询 | 新增 | 修改 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` | `Long` | 选填 | 不使用 | 必填 | 正整数主键 | 规则主键 | `9001` |
| `name` | `String` | 选填 | 必填 | 必填 | 非空字符串 | 规则名称 | `异常字符检测` |
| `ruleType` | `String` | 选填 | 必填 | 必填 | 枚举字符串 | `REGEX`、`DICT`、`CUSTOM` | `REGEX` |
| `ruleContent` | `String` | 选填 | 必填 | 必填 | 文本或表达式 | 规则核心内容 | `[^\\u4e00-\\u9fa5A-Za-z0-9号栋单元室-]` |
| `status` | `String` | 选填 | 选填 | 选填 | `0` 正常，`1` 停用 | 规则状态 | `0` |
| `remark` | `String` | 不使用 | 选填 | 选填 | 普通文本 | 备注 | `校验异常字符` |

```json
{
  "id": 9001,
  "name": "异常字符检测",
  "ruleType": "REGEX",
  "ruleContent": "[^\\u4e00-\\u9fa5A-Za-z0-9号栋单元室-]",
  "status": "0",
  "remark": "校验异常字符"
}
```

#### `StandardAddressMonitorRecordBo`

| 字段 | 类型 | 查询 | 修改 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- |
| `id` | `Long` | 选填 | 必填 | 正整数主键 | 异常记录主键 | `9101` |
| `segmId` | `String` | 选填 | 选填 | `varchar(24)` 字符串主键 | 关联标准地址 | `000102010000000011800001` |
| `ruleId` | `Long` | 选填 | 选填 | 正整数主键 | 命中规则 | `9001` |
| `status` | `String` | 选填 | 必填 | `0` 待处理，`1` 已忽略，`2` 已处理 | 处理状态 | `2` |
| `notes` | `String` | 选填 | 选填 | 普通文本 | 备注或处理说明 | `人工修正完成` |

```json
{
  "id": 9101,
  "segmId": "000102010000000011800001",
  "ruleId": 9001,
  "status": "0",
  "notes": "待处理"
}
```

#### `StandardAddressAdminBo.MonitorTaskBo`

| 字段 | 类型 | 查询 | 新增 | 修改 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` | `Long` | 选填 | 不使用 | 必填 | 正整数主键 | 任务主键 | `9201` |
| `taskName` | `String` | 选填 | 必填 | 必填 | 非空字符串 | 任务名称 | `每日非标地址扫描` |
| `taskType` | `String` | 选填 | 必填 | 必填 | 枚举字符串 | `ONCE`、`SCHEDULE`、`MANUAL` | `SCHEDULE` |
| `executeRule` | `String` | 选填 | 必填 | 必填 | Cron 表达式或业务规则字符串 | 执行规则 | `0 0 2 * * ?` |
| `executeTime` | `Date` | 选填 | 条件必填 | 条件必填 | `yyyy-MM-dd HH:mm:ss` | 单次任务或指定时间任务执行时间 | `2026-03-20 02:00:00` |
| `monitorScope` | `String` | 选填 | 必填 | 必填 | 枚举字符串 | `ALL`、`PART` 等 | `ALL` |
| `relatedRuleIds` | `List<Long>` | 不使用 | 必填 | 必填 | `Long` 数组 | 关联规则集合 | `[9001, 9002]` |
| `addressIds` | `List<Long>` | 不使用 | 条件必填 | 条件必填 | `Long` 数组 | 当 `monitorScope=PART` 时必填 | `[10001, 10002]` |
| `taskDesc` | `String` | 不使用 | 选填 | 选填 | 普通文本 | 任务说明 | `每天凌晨执行` |
| `taskStatus` | `String` | 选填 | 选填 | 选填 | 枚举字符串 | `RUNNING`、`PAUSED`、`STOPPED`、`FAILED` | `RUNNING` |
| `failureReason` | `String` | 选填 | 不使用 | 不使用 | 普通文本 | 最近一次失败原因 | `调度中心不可用` |

```json
{
  "id": 9201,
  "taskName": "每日非标地址扫描",
  "taskType": "SCHEDULE",
  "executeRule": "0 0 2 * * ?",
  "executeTime": "2026-03-20 02:00:00",
  "monitorScope": "ALL",
  "relatedRuleIds": [9001, 9002],
  "addressIds": [10001, 10002],
  "taskDesc": "每天凌晨执行",
  "taskStatus": "RUNNING",
  "failureReason": null
}
```

#### `StandardAddressAdminBo.WorkOrderBo`

| 字段 | 类型 | 列表查询 | 创建工单 | 修正工单 | 驳回工单 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `id` | `Long` | 选填 | 不使用 | 不使用 | 不使用 | 正整数主键 | 工单主键，修正/驳回时主键走路径 | `9301` |
| `workOrderNo` | `String` | 选填 | 不使用 | 不使用 | 不使用 | 普通字符串 | 工单号 | `WO202603190001` |
| `abnormalAddress` | `String` | 选填 | 不使用 | 不使用 | 不使用 | 普通文本 | 异常地址关键字 | `中央路88号@@101` |
| `workOrderStatus` | `String` | 选填 | 不使用 | 不使用 | 不使用 | 枚举字符串 | 工单状态 | `PENDING` |
| `abnormalWarningIds` | `List<Long>` | 不使用 | 必填 | 不使用 | 不使用 | `Long` 数组，至少 1 条 | 用于批量创建工单 | `[9101, 9102]` |
| `originalAddress` | `String` | 不使用 | 不使用 | 不使用 | 不使用 | 普通文本 | 主要用于返回展示 | `中央路88号@@101` |
| `detailAddress` | `String` | 不使用 | 不使用 | 条件必填 | 不使用 | 普通文本 | 修正后的明细地址，与 `correctedAddress` 二选一至少一项 | `中央路88号1单元101室` |
| `gridId` | `Long` | 选填 | 不使用 | 选填 | 不使用 | 正整数主键 | 修正后关联网格 | `3001` |
| `correctedAddress` | `String` | 不使用 | 不使用 | 条件必填 | 不使用 | 完整地址字符串 | 与 `detailAddress` 二选一至少一项 | `江苏省南京市鼓楼区中央路88号1单元101室` |
| `reason` | `String` | 不使用 | 不使用 | 不使用 | 必填 | 非空文本 | 驳回原因 | `原始信息不足` |

```json
{
  "id": 9301,
  "workOrderNo": "WO202603190001",
  "abnormalAddress": "中央路88号@@101",
  "workOrderStatus": "PENDING",
  "abnormalWarningIds": [9101, 9102],
  "originalAddress": "中央路88号@@101",
  "detailAddress": "中央路88号1单元101室",
  "gridId": 3001,
  "correctedAddress": "江苏省南京市鼓楼区中央路88号1单元101室",
  "reason": "原始信息不足"
}
```

### 3.8 选址平台参数

#### `StandardAddressAdminBo.SelectionSearchBo`

| 字段 | 类型 | 必填 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `keyword` | `String` | 必填 | 非空关键字 | 地址关键字 | `中央路88号` |
| `levelMax` | `Integer` | 选填 | 正整数，默认按展示层级理解 | 最大搜索层级；若实现直接按真实 `level_id` 过滤，必须在接口约定中显式说明，避免与展示级次混淆 | `10` |
| `limit` | `Integer` | 选填 | 正整数，建议不超过 50 | 返回条数限制 | `20` |

```json
{
  "keyword": "中央路88号",
  "levelMax": 10,
  "limit": 20
}
```

#### `StandardAddressAdminBo.SelectionMapSearchBo`

| 字段 | 类型 | 必填 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `geometryType` | `String` | 必填 | `POLYGON`、`RECTANGLE`、`CIRCLE` 等 | 几何类型 | `POLYGON` |
| `coordinates` | `List<List<Double>>` | 必填 | 坐标数组，顺序遵循 GIS 要求 | 地图圈选坐标点集合 | `[[118.781,32.061],[118.782,32.062],[118.783,32.061]]` |

```json
{
  "geometryType": "POLYGON",
  "coordinates": [
    [118.781, 32.061],
    [118.782, 32.062],
    [118.783, 32.061]
  ]
}
```

#### `StandardAddressAdminBo.SelectionPreviewBo`

| 字段 | 类型 | 安装地址预览 | 生成安装地址 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- |
| `segmId` | `String` | 必填 | 必填 | `varchar(24)` 字符串主键 | 目标标准地址 | `000102010000000011800001` |
| `roomIds` | `List<Long>` | 选填 | 选填 | `Long` 数组 | 选中的房间集合 | `[1, 2, 3]` |

```json
{
  "segmId": "000102010000000011800001",
  "roomIds": [1, 2, 3]
}
```

#### `StandardAddressSelectionRoomBo`

| 字段 | 类型 | 生成房间地址 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `parentSegmId` | `String` | 必填 | `varchar(24)` 字符串主键 | 楼栋业务语义对应的标准地址主键；真实类型需经 `segm_addr_type` 解析，不允许硬编码固定层级数字 | `000102010000000011800010` |
| `roomName` | `String` | 必填 | 非空字符串 | 房间号或房间名 | `101室` |
| `setAddrName` | `String` | 选填 | 普通文本 | 安装地址名称，不传时可默认使用 `roomName` 或按规则自动拼装 | `101室弱电箱` |

```json
{
  "parentSegmId": "000102010000000011800010",
  "roomName": "101室",
  "setAddrName": "101室弱电箱"
}
```

补充说明：选址从楼栋生成新标准地址时，真实类型需结合 `segm_addr_type` 与联调库字典核定。若当前业务采用“选址尾级地址”口径，应优先落到 `19-尾级地址（选址生成）`；若联调明确要求生成规范化房间层，再映射为 `16-房间`。

### 3.9 网格组织参数

#### `GridManagementBo.GridOrgBo`

| 字段 | 类型 | 查询/导出 | 新增 | 修改 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` | `Long` | 选填 | 不使用 | 必填 | 正整数主键 | 组织主键 | `1` |
| `parentId` | `Long` | 不使用 | 选填 | 选填 | 正整数主键，根节点可传 `0` 或空 | 父级组织主键 | `0` |
| `orgName` | `String` | 选填 | 必填 | 必填 | 非空字符串 | 组织名称 | `鼓楼分公司` |
| `status` | `String` | 选填 | 选填 | 选填 | `0` 正常，`1` 停用 | 组织状态 | `0` |
| `remark` | `String` | 不使用 | 选填 | 选填 | 普通文本 | 备注 | `一级组织` |

```json
{
  "id": 1,
  "parentId": 0,
  "orgName": "鼓楼分公司",
  "status": "0",
  "remark": "一级组织"
}
```

### 3.10 网格主资源参数

#### `GridManagementBo.GridBo`

| 字段 | 类型 | 查询/导出 | 新增 | 修改 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` | `Long` | 选填 | 不使用 | 必填 | 正整数主键 | 网格主键 | `3001` |
| `gridId` | `String` | 选填 | 必填 | 必填 | 非空编码字符串 | 网格编码 | `GL-ZYL-001` |
| `gridName` | `String` | 选填 | 必填 | 必填 | 非空字符串 | 网格名称 | `鼓楼-中央路网格` |
| `gridProperty` | `String` | 选填 | 必填 | 必填 | 枚举或字典值 | 网格属性 | `FTTH` |
| `gridManagerId` | `Long` | 不使用 | 选填 | 选填 | 正整数主键 | 网格经理主键 | `4001` |
| `gridManager` | `String` | 选填 | 不使用 | 不使用 | 普通字符串 | 网格经理名称 | `张三` |
| `belongStation` | `String` | 选填 | 必填 | 必填 | 非空字符串 | 归属站点 | `鼓楼营维中心` |
| `orgId` | `Long` | 选填 | 选填 | 选填 | 正整数主键 | 所属组织 | `2` |

```json
{
  "id": 3001,
  "gridId": "GL-ZYL-001",
  "gridName": "鼓楼-中央路网格",
  "gridProperty": "FTTH",
  "gridManagerId": 4001,
  "gridManager": "张三",
  "belongStation": "鼓楼营维中心",
  "orgId": 2
}
```

### 3.11 网格地址关联参数

#### `GridManagementBo.GridAddressRelationBo`

| 字段 | 类型 | 列表查询 | 绑定 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- |
| `relationId` | `Long` | 选填 | 不使用 | 正整数主键 | 关联关系主键 | `6001` |
| `addressId` | `Long` | 选填 | 不使用 | 正整数主键 | 标准地址主键 | `10001` |
| `addressName` | `String` | 选填 | 不使用 | 完整地址字符串 | 地址名称关键字 | `江苏省南京市鼓楼区中央路88号1单元101室` |
| `gridId` | `Long` | 选填 | 必填 | 正整数主键 | 目标网格主键 | `3001` |
| `gridName` | `String` | 选填 | 不使用 | 普通字符串 | 网格名称关键字 | `鼓楼-中央路网格` |
| `addressIds` | `List<Long>` | 不使用 | 必填 | `Long` 数组，至少 1 条 | 待绑定地址集合 | `[10001, 10002]` |

补充约束：

- `addressIds` 在网格地址划分场景默认应是楼栋级标准地址集合，而不是任意层级地址；“楼栋级”需通过类型映射解析，不能直接按固定数值判断。
- 同一楼栋级标准地址只能归属一个网格，数据库应在关系层保证唯一性。

```json
{
  "relationId": 6001,
  "addressId": 10001,
  "addressName": "江苏省南京市鼓楼区中央路88号1单元101室",
  "gridId": 3001,
  "gridName": "鼓楼-中央路网格",
  "addressIds": [10001, 10002]
}
```

### 3.12 网格客户参数

#### `GridManagementBo.GridCustomerBo`

| 字段 | 类型 | 列表查询 | 绑定 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- |
| `customerId` | `String` | 选填 | 不使用 | 客户编码字符串 | 客户标识 | `CUST001` |
| `customerName` | `String` | 选填 | 不使用 | 普通字符串 | 客户姓名 | `李四` |
| `phone` | `String` | 选填 | 不使用 | 手机号字符串 | 联系电话 | `13800000000` |
| `address` | `String` | 选填 | 不使用 | 地址关键字 | 客户地址 | `江苏省南京市鼓楼区中央路88号1单元101室` |
| `gridId` | `Long` | 选填 | 必填 | 正整数主键 | 目标网格主键 | `3001` |
| `gridName` | `String` | 选填 | 不使用 | 普通字符串 | 网格名称 | `鼓楼-中央路网格` |
| `customerIds` | `List<String>` | 不使用 | 必填 | 字符串数组，至少 1 条 | 待绑定客户集合 | `["CUST001", "CUST002"]` |

补充约束：

- 客户与网格是强关联的一对一关系。
- 如果客户手工绑定结果与“网格地址划分”自动归属冲突，以客户手工绑定关系为准，因此库表层建议保留 `bindingSource`、`overrideFlag` 或等价字段。

```json
{
  "customerId": "CUST001",
  "customerName": "李四",
  "phone": "13800000000",
  "address": "江苏省南京市鼓楼区中央路88号1单元101室",
  "gridId": 3001,
  "gridName": "鼓楼-中央路网格",
  "customerIds": ["CUST001", "CUST002"]
}
```

### 3.13 网格经理参数

#### `GridManagementBo.GridManagerBo`

| 字段 | 类型 | 查询/导出 | 新增 | 修改 | 绑定网格 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `id` | `Long` | 选填 | 不使用 | 必填 | 不使用 | 正整数主键 | 经理主键 | `4001` |
| `managerId` | `String` | 选填 | 选填 | 选填 | 不使用 | 编码字符串 | 经理编码 | `MGR001` |
| `managerName` | `String` | 选填 | 必填 | 必填 | 不使用 | 非空字符串 | 经理名称 | `张三` |
| `orgId` | `Long` | 选填 | 必填 | 必填 | 选填 | 正整数主键 | 所属组织 | `2` |
| `orgName` | `String` | 选填 | 不使用 | 不使用 | 不使用 | 普通字符串 | 组织名称 | `中央路片区` |
| `phone` | `String` | 选填 | 选填 | 选填 | 不使用 | 手机号字符串 | 联系电话 | `13900000000` |
| `gridId` | `Long` | 选填 | 不使用 | 不使用 | 不使用 | 正整数主键 | 单一网格筛选条件 | `3001` |
| `gridName` | `String` | 选填 | 不使用 | 不使用 | 不使用 | 普通字符串 | 网格名称关键字 | `鼓楼-中央路网格` |
| `gridIds` | `List<Long>` | 不使用 | 选填 | 选填 | 必填 | `Long` 数组 | 关联网格集合 | `[3001, 3002]` |

```json
{
  "id": 4001,
  "managerId": "MGR001",
  "managerName": "张三",
  "orgId": 2,
  "orgName": "中央路片区",
  "phone": "13900000000",
  "gridId": 3001,
  "gridName": "鼓楼-中央路网格",
  "gridIds": [3001, 3002]
}
```

### 3.14 旧系统兼容接口参数

本节单独维护“新系统对外兼容老系统接口”所需的参数口径。以下模型不强制映射到内部 `Controller BO`，更适合作为协议适配层 DTO、报文转换对象或开放接口出入参基线。

#### `LegacyBossSelectionUrlReq`

| 字段 | 类型 | 必填 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `systemsource` | `String` | 必填 | 固定传 `BOSS` | 调用来源系统标识 | `BOSS` |
| `method` | `String` | 必填 | 固定传 `checkAddressForBOSS` | 旧页面选址入口方法名 | `checkAddressForBOSS` |
| `uuId` | `String` | 必填 | 全局唯一字符串 | 本次选址请求唯一标识 | `a0c446fe582e468188754029fca024ea` |
| `areaCode` | `String` | 必填 | 区域编码字符串 | 旧系统区域编码 | `nj.js.cn` |
| `cipherIndex` | `String` | 必填 | 密钥序号字符串 | 标识使用哪一把密钥加密 | `3` |
| `cipherType` | `String` | 选填 | 当前文档中允许为空 | 加密算法标记 | `` |
| `cipherText` | `String` | 必填 | URL 编码后的密文 | 需能解密出 `invokeTime` | `4RuG0idQ0QVC...` |
| `staffNbr` | `String` | 必填 | 操作工号字符串 | BOSS 前台操作工号 | `148901500242` |

补充说明：

- 解密后的明文至少应包含 `invokeTime`，格式通常为 `yyyyMMddHHmmss`。
- 旧文档要求服务端校验 `invokeTime` 与当前服务器时间差，暂按 `10` 秒容差理解。

#### `LegacyBossSelectionResultObj`

| 字段 | 类型 | 必填 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `uuId` | `String` | 必填 | 与入口参数一致 | 透传回旧系统前台 | `a0c446fe582e468188754029fca024ea` |
| `segm_id` | `String` | 必填 | 旧标准地址字符串主键 | 标准地址 legacy ID | `000102140000000123828580` |
| `stand_name` | `String` | 必填 | 标准地址全称 | 选址返回的标准地址名称 | `江苏省 泰州市 泰兴公司 泰兴镇 ... 8栋` |
| `set_id` | `String` | 条件必填 | 旧安装地址字符串主键 | 安装地址 legacy ID | `000102740000000028194362` |
| `set_name` | `String` | 条件必填 | 安装地址名称 | 选址返回的安装地址名称 | `+兴化公司+乡镇+...+777室` |
| `FTTH_PON_TYPE_ID` | `String` | 选填 | 字典 ID 字符串 | 光纤接入能力字典 ID | `2141301` |
| `FTTH_PON_TYPE` | `String` | 选填 | 字典值 | 光纤接入能力 | `1G-PON` |
| `ADDR_IN_TYPE_FTTH_ID` | `String` | 选填 | 字典 ID 字符串 | 光纤接入方式字典 ID | `2140760` |
| `ADDR_IN_TYPE_FTTH` | `String` | 选填 | 字典值 | 光纤接入方式 | `FTTH_双纤` |
| `ADDR_IN_TYPE_LAN_ID` | `String` | 选填 | 字典 ID 字符串 | 电缆接入方式字典 ID | `2140780` |
| `ADDR_IN_TYPE_LAN` | `String` | 选填 | 字典值 | 电缆接入方式 | `CMTS` |

#### `LegacyJsAddrTreeReq`

| 字段 | 类型 | 必填 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `method` | `String` | 必填 | 固定传 `getChildAddr` | 地址树查询方法名 | `getChildAddr` |
| `resObjectId` | `String` | 必填 | 旧区域 / 地址 ID | 上级地址或顶级区域 ID | `000102140000000000000001_gGmCYp` |

#### `LegacyJsAddrTreeItemVo`

| 字段 | 类型 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `id` | `String` | XML `item@id` | 子地址 legacy ID | `000102140000000021118536_csMPsX` |
| `text` | `String` | XML `item@text` | 节点名称 | `南京市` |
| `child` | `String` | `0/1` | 是否还有下级节点 | `1` |
| `segmType` | `String` | XML `userdata` 值 | 旧地址类型编码 | `180001` |

#### `LegacyJsAddressSearchReq`

| 字段 | 类型 | 必填 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `method` | `String` | 必填 | 固定传 `getAddress` | 模糊查询方法名 | `getAddress` |
| `type` | `String` | 必填 | 旧文档固定为 `1` | 查询类型 | `1` |
| `limit` | `String` | 必填 | 正整数，默认 `20` | 最大返回条数 | `20` |
| `keyword` | `String` | 必填 | 多级名称用 `+` 拼接 | 关键词检索串 | `江苏省+泰州市+泰兴公司+泰兴镇+澜墅` |

#### `LegacyJsAddressSearchItemVo`

| 字段 | 类型 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `totalNum` | `String` | 数字字符串 | 本次结果总数 | `6` |
| `segmId` | `String` | 旧标准地址 ID | 地址 legacy ID | `000102140000000123828580` |
| `fullNameList` | `String` | 含旧前端高亮标签的 HTML 字符串 | 旧系统展示用高亮名称 | `江苏省</font> 泰州市...</font>` |
| `fullNameNoAnalysis` | `String` | 普通字符串 | 文档样例中通常为空 | `` |
| `resourceSegment` | `String` | 字符串化 JSON 片段 | 旧资源段信息 | `{\"message\":\"OK\"` |
| `segmType` | `String` | legacy 类型编码 | 地址类型 | `180013` |
| `fullName` | `String` | 完整地址字符串 | 标准地址全称 | `江苏省 泰州市 ... 8栋` |
| `regionId` | `String` | legacy 区域 ID | 归属区域 | `000102000000000042762893` |

#### `LegacyJsSaveAddressReq`

| 字段 | 类型 | 必填 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `uuId` | `String` | 必填 | 随机唯一字符串 | 请求唯一标识 | `ss012sfsds5462sdf89` |
| `areaCode` | `String` | 必填 | 区域编码字符串 | 旧系统区域编码 | `TZTXGD.JS.CN` |
| `staffNbr` | `String` | 必填 | 工号字符串 | 操作账号 | `10016` |
| `org_id` | `String` | 必填 | 组织 ID 字符串，可为空字面值 | 旧组织标识 | `null` |
| `fullname` | `String` | 必填 | 标准地址全称 | 创建时关联的标准地址全称 | `江苏省 泰州市 ... 0组` |
| `addressId` | `String` | 必填 | 分段地址 ID | 上级分段地址 legacy ID | `000102140000000012217548` |
| `adId` | `String` | 必填 | 默认传 `1` | 旧参数保留 | `1` |
| `adName` | `String` | 必填 | 标准地址全称 | 与 `fullname` 一致 | `江苏省 泰州市 ... 0组` |
| `returnValue` | `String` | 必填 | 安装地址全称 | 新生成安装地址名称 | `江苏省 泰州市 ... 777室` |
| `objStr.parentTypeId` | `String` | 必填 | legacy 地址类型编码 | 上级地址类型 | `180013` |
| `objStr.parentAddressName` | `String` | 必填 | 完整地址字符串 | 上级地址名称 | `江苏省 泰州市 ... 0组` |
| `objStr.unitName` | `String` | 必填 | 单元名称 | 生成单元 | `777单元` |
| `objStr.floorName` | `String` | 必填 | 楼层名称 | 生成楼层 | `777层` |
| `objStr.roomName` | `String` | 必填 | 房间名称 | 生成房间 | `777室` |

#### `LegacyJsSaveAddressResp`

| 字段 | 类型 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `companyId` | `String` | 旧公司 ID | 归属公司 | `3035` |
| `uuId` | `String` | 原样回传 | 请求标识 | `ss012sfsds5462sdf88` |
| `segm_id` | `String` | 旧标准地址 ID | 新标准地址 legacy ID | `000002140000000130065430` |
| `access_mode` | `String` | 普通字符串 | 接入模式补充 | `` |
| `stand_name` | `String` | 完整地址字符串 | 新标准地址名称 | `江苏省 泰州市 ... 777室` |
| `set_id` | `String` | 旧安装地址 ID | 新安装地址 legacy ID | `000102740000000028194362` |
| `set_name` | `String` | 完整地址字符串 | 新安装地址名称 | `+兴化公司+乡镇+...+777室` |
| `result` | `String` | `OK/FAIL` 等 | 结果标识 | `OK` |
| `flag` | `String` | 旧系统结果标记 | 成功标志 | `1` |
| `mesg` | `String` | 普通字符串 | 返回信息 | `OK` |
| `station_id` | `String` | 旧管理站 ID | 地址所属管理站 | `000102010000000011823409` |
| `addr_in_type` | `String` | 普通字符串 | 地址接入方式 | `` |

#### `LegacyBossAddressChangedReq`

| 字段 | 类型 | 必填 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `requestSystemNo` | `String` | 必填 | 固定传 `4` | 旧系统来源编码 | `4` |
| `requestNo` | `String` | 必填 | 固定传 `4` | 旧接口编号 | `4` |
| `custId` | `String` | 必填 | 客户编号字符串 | 客户标识 | `000000` |
| `setAddrId` | `String` | 必填 | 安装地址 legacy ID | 安装地址标识 | `123456` |
| `setAddrName` | `String` | 必填 | 安装地址名称 | 安装地址全称 | `南京市白下区...` |
| `FTTH_PON_TYPE` | `String` | 选填 | 字典值 | 光纤接入能力 | `1G-PON` |
| `ADDR_IN_TYPE_FTTH` | `String` | 选填 | 字典值 | 光纤接入方式 | `FTTH_双纤` |
| `ADDR_IN_TYPE_LAN` | `String` | 选填 | 字典值 | 电缆接入方式 | `CMTS` |
| `FTTH_PON_TYPE_ID` | `String` | 选填 | 字典 ID 字符串 | 光纤接入能力 ID | `2141301` |
| `ADDR_IN_TYPE_FTTH_ID` | `String` | 选填 | 字典 ID 字符串 | 光纤接入方式 ID | `2140760` |
| `ADDR_IN_TYPE_LAN_ID` | `String` | 选填 | 字典 ID 字符串 | 电缆接入方式 ID | `2140780` |

#### `LegacyBossAddressChangedResp`

| 字段 | 类型 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `return-code` | `String` | `0/-1` | `0` 成功，`-1` 失败 | `0` |
| `return-message` | `String` | 普通字符串 | 返回信息 | `标准地址变更成功` |

#### `LegacyBossXmlAddressSyncReq`

| 字段 | 类型 | 必填 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- |
| `DOMAIN_SEND` | `String` | 必填 | 系统编码 | 请求发送方 | `BOSS` |
| `DOMAIN_RECEIVE` | `String` | 必填 | 系统编码 | 请求接收方 | `GIS` |
| `REGIONCODE` | `String` | 必填 | 区域编码 | 本地网区域编码 | `NJ` |
| `OPERATION_NAME` | `String` | 必填 | `ADD/UPDATE/QUERY` | 同步动作 | `QUERY` |
| `REQUEST_INFORMATION.BOSS_OP` | `String` | 选填 | 操作人员字符串 | 前台录入人员 | `zhangsan` |
| `REQUEST_INFORMATION.SEGM_ID` | `String` | 条件必填 | 旧地址 ID | 单条同步或查询时使用 | `000102140000000123828580` |
| `REQUEST_INFORMATION.SEGM_TYPE` | `String` | 必填 | `0/1` | `0` 标准地址，`1` 安装地址 | `0` |
| `REQUEST_INFORMATION.ADDR_NUM` | `String` | 选填 | 数字字符串 | 同步条数 | `1` |
| `REQUEST_INFORMATION.EXTENDS1` | `String` | 选填 | 扩展字段 | 预留字段 | `` |
| `REQUEST_INFORMATION.EXTENDS2` | `String` | 选填 | 扩展字段 | 预留字段 | `` |

#### `LegacyBossXmlAddressSyncResp`

| 字段 | 类型 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `REQUEST_INFORMATION.SEGM_ID` | `String` | 旧标准地址 ID | 标准地址标识 | `000102140000000123828580` |
| `REQUEST_INFORMATION.SEGM_NAME` | `String` | 普通字符串 | 标准地址名称 | `澜墅8栋` |
| `REQUEST_INFORMATION.BOSS_OP` | `String` | 普通字符串 | 前台录入人员 | `zhangsan` |
| `REQUEST_INFORMATION.COVERNUM` | `String` | 数字字符串 | 覆盖户数 | `128` |
| `REQUEST_INFORMATION.STATION_ID` | `String` | 旧管理站 ID | 所属管理站 | `000102010000000011823409` |
| `REQUEST_INFORMATION.INTYPE` | `String` | 普通字符串 | 地址接入方式 | `FTTH` |
| `REQUEST_INFORMATION.STAND_NAME` | `String` | 完整地址字符串 | 标准地址全称 | `江苏省 南京市 ...` |
| `REQUEST_INFORMATION.SET_ADDR.SET_ADDR_ID` | `String` | 旧安装地址 ID | 安装地址标识 | `000102740000000028194362` |
| `REQUEST_INFORMATION.SET_ADDR.SET_ADDR_NAME` | `String` | 完整地址字符串 | 安装地址名称 | `南京市...101室` |
| `REQUEST_INFORMATION.SET_ADDR.FTTH_PON_TYPE` | `String` | 字典值 | 光纤接入能力 | `1G-PON` |
| `REQUEST_INFORMATION.SET_ADDR.ADDR_IN_TYPE_FTTH` | `String` | 字典值 | 光纤接入方式 | `FTTH_双纤` |
| `REQUEST_INFORMATION.SET_ADDR.ADDR_IN_TYPE_LAN` | `String` | 字典值 | 电缆接入方式 | `CMTS` |
| `REQUEST_INFORMATION.SET_ADDR.FTTH_PON_TYPE_ID` | `String` | 字典 ID 字符串 | 光纤接入能力 ID | `2141301` |
| `REQUEST_INFORMATION.SET_ADDR.ADDR_IN_TYPE_FTTH_ID` | `String` | 字典 ID 字符串 | 光纤接入方式 ID | `2140760` |
| `REQUEST_INFORMATION.SET_ADDR.ADDR_IN_TYPE_LAN_ID` | `String` | 字典 ID 字符串 | 电缆接入方式 ID | `2140780` |
| `RETURN_TYPE` | `String` | 成功/失败 | 处理结果 | `成功` |
| `REASON` | `String` | 不超过 `255` 字符 | 失败原因或补充说明 | `OK` |

#### `LegacyCustomerSyncReq`

| 字段 | 类型 | 单条同步 | 批量同步 | 格式说明 | 说明 | 示例 |
| --- | --- | --- | --- | --- | --- | --- |
| `orgCode` | `String` | 必填 | 必填 | 分公司编号字符串 | 组织编码 | `NJ01` |
| `searchType` | `String` | 必填 | 必填 | `indivSynch/batchSynch` | 同步方式 | `indivSynch` |
| `key` | `String` | 必填 | 不使用 | `customerNo/serialNo/accountNo/tvNo/icCard` | 单条同步检索类型 | `customerNo` |
| `value` | `String` | 必填 | 不使用 | 普通字符串 | 单条同步检索内容 | `C123456` |
| `rownum` | `String` | 不使用 | 必填 | 数字字符串 | 批量同步数量 | `100` |
| `channelId` | `String` | 选填 | 选填 | 渠道 ID 字符串 | 渠道标识 | `CH01` |

说明：

- 旧文档中还定义了 `CustomerInfo`、`SubsInfo` 大量客户字段，但当前更偏客户域能力。
- 现阶段建议先在接口册中保留其参数口径，作为跨模块或外部依赖接口，不默认纳入 `ruoyi-address` 内部 Controller 基线。

## 4. 标准地址模块

本章重点说明接口用途、路由和业务语义；每个接口具体可传字段、必填项、格式和参数示例，统一以上文第 3 节对应模型为准。

## 4.1 标准地址主资源

### 4.1.1 标准地址列表

- 方法/路径：`POST /address/standard/list`
- 请求格式：`application/x-www-form-urlencoded`
- 功能描述：根据 `segmName`、`standName`、`segmNo`、`standNo`、`segmType`、`levelId`、`parentSegmId`、`regionId`、`districtId`、`serviceRegionId`、`status` 以及分页排序条件查询标准地址列表，并兼容需求书中提到的 `分公司/组织范围`、`地址属性`、`管理站归属` 等扩展筛选。返回字段除基础地址信息外，还应覆盖标签、管理站名称、地址属性摘要、创建时间等，供综合管理、下级地址查看、标签操作和导出复用。
- 格式要求：分页字段必须和过滤字段一起提交；排序字段建议只允许白名单字段；模糊搜索优先对 `segmName/standName` 生效；`segmType` 是真实存储字段，`levelId` 是按 `segm_addr_type.level_id` 投影后的层级口径；查询结果必须按登录态 `tenantId/deptId` 及数据权限范围过滤；若筛选或返回 `1/2` 级行政区划，统一按 `spc_region` 口径处理，其他级别按 `addr_segm` / 标准地址事实表处理，且 `addr_segm` 侧层级条件应用于 `addr_segm.segm_type`；“下级地址查看”场景必须支持按某个祖先地址查询其全部后代地址，再按层级过滤，不能仅按直属 `parentSegmId` 查询替代。

请求示例：

```http
POST /address/standard/list
Content-Type: application/x-www-form-urlencoded

pageNum=1&pageSize=20&orderByColumn=createDate&isAsc=desc&segmName=中央路&levelId=7&status=2140900
```

返回示例：

```json
{
  "code": 200,
  "msg": "查询成功",
  "total": 1,
  "rows": [
    {
      "segmId": "000102010000000011800001",
      "parentSegmId": "000102010000000011800000",
      "segmName": "101室",
      "segmNo": "101S",
      "standName": "江苏省南京市鼓楼区中央路88号1单元101室",
      "standNo": "JSSNJSNJSQGLQZYL88H1DY101S",
      "segmType": 180007,
      "levelId": 16,
      "regionId": "000102010000000011823409",
      "districtId": "320106",
      "serviceRegionId": "320106001001",
      "status": 2140900,
      "notes": "测试地址",
      "createDate": "2026-03-19 10:00:00",
      "tagNames": ["重点覆盖", "已入网"]
    }
  ]
}
```

### 4.1.2 标准地址详情

- 方法/路径：`POST /address/standard/{segmId}`；若基线代码暂时仍保留 `{id}` 占位名，语义也必须按 `segmId` 理解
- 请求格式：无请求体
- 功能描述：根据标准地址主键 `segmId` 查询单条标准地址详情，主要返回基础地址信息、状态、标签名称，并补充需求书要求的 `standNo`、所属管理站、地址属性、是否只读（一二级外部地址）、关联安装地址摘要等字段，供详情页、编辑页回显和外围系统引用。
- 格式要求：`segmId` 必须存在，且必须属于当前租户可见数据；详情接口应能区分当前地址是否允许新增下级、是否允许修改删除、是否允许合并拆分，以及是否存在下级地址/关联资源/关联安装地址；一二级外部地址建议通过 `readOnlyFlag/canMerge/canSplit` 等权限位显式返回；详情需能回显 `segmId/parentSegmId/segmName/standName/standNo/segmType/levelId/isCity/areaType/stationId/installStationId/busStationId` 等核心字段。

返回示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "segmId": "000102010000000011800001",
    "parentSegmId": "000102010000000011800000",
    "segmName": "101室",
    "standName": "江苏省南京市鼓楼区中央路88号1单元101室",
    "standNo": "JSSNJSNJSQGLQZYL88H1DY101S",
    "segmType": 180007,
    "levelId": 16,
    "status": 2140900,
    "readOnlyFlag": "N",
    "canMerge": true,
    "canSplit": true,
    "stationName": "中央路维修站",
    "tagNames": ["重点覆盖"]
  }
}
```

### 4.1.3 新增标准地址

- 方法/路径：`POST /address/standard`
- 请求格式：`application/json`
- 功能描述：在指定父级地址下新增标准地址。调用时至少要提供 `parentSegmId` 和 `segmName`，其余 `segmType/regionId/districtId/serviceRegionId/stationId` 等字段可由业务规则自动补齐或校验；`standName/standNo/segmNo` 原则上由后端根据父地址和命名规则生成；如果同时维护管理站和地址属性，也应在同一保存事务内落库。
- 格式要求：`segmName` 不能为空；如果提交 `segmType` 或 `levelId`，则必须按 `segm_addr_type` 校验父子层级连续关系，并遵守当前需求书定义的 `19` 类底层结构类型；一二级预置地址不允许通过此接口创建；城区 / 非城区作为标准地址的单独属性存在，与实际层级并无关系，需按 `isCity` 或等价配置规则校验，`areaType` 仅作为城乡属性独立保存；新增记录默认带入当前 `tenantId/deptId` 与数据来源标识；`standNo` 按“中文转拼音首字母大写、中文括号转英文括号、数字不变”规则自动生成。

请求示例：

```json
{
  "parentSegmId": "000102010000000011800000",
  "segmName": "101室",
  "segmType": 180007,
  "levelId": 16,
  "regionId": "000102010000000011823409",
  "districtId": "320106",
  "serviceRegionId": "320106001001",
  "status": 2140900,
  "notes": "新增地址"
}
```

### 4.1.4 修改标准地址

- 方法/路径：`POST /address/standard/update`
- 请求格式：`application/json`
- 功能描述：根据地址主键修改标准地址。常见修改项包括 `segmName`、`status`、`notes`、`parentSegmId`、`regionId/districtId/serviceRegionId`、所属管理站和地址属性。针对需求书特别强调的“当前地址名称”变更场景，系统需同步刷新当前地址的 `standName`、`standNo` 以及所有下级标准地址的 `parentSegmId`、`standName`、`standNo`；同时把所有关联安装地址的对应字段一并更新。
- 格式要求：`segmId` 必填；修改后仍需满足层级规则、唯一性约束和预置地址限制；一二级预置地址不允许通过界面修改；若修改影响 `isCity/areaType`、`segmType/levelId` 或名称，需重新校验 `8/9` 级差异规则并重算简拼；级联刷新建议在单事务或可靠异步补偿机制内完成，避免标准地址与安装地址快照不一致。

请求示例：

```json
{
  "segmId": "000102010000000011800001",
  "parentSegmId": "000102010000000011800000",
  "segmName": "101室",
  "status": 2140900,
  "notes": "修正门牌名称"
}
```

### 4.1.5 删除标准地址

- 方法/路径：`POST /address/standard/remove/{segmIds}`；若基线代码暂时仍保留 `{ids}` 占位名，语义也必须按 `segmIds` 理解
- 请求格式：路径参数 + 可选表单参数
- 功能描述：删除一个或多个标准地址。适合列表页批量删除，也兼容需求书中“逻辑删除 + 物理删除”的两阶段能力。删除前先校验是否存在下级地址或已关联资源；这里的“关联资源”按需求书备注至少包括网格、客户等。如果前置校验通过，再判断地址及其下级地址是否关联安装地址；如有关联，则进入二次确认删除流程。
- 格式要求：`segmIds` 使用逗号分隔的 `varchar(24)` 字符串主键集合；`confirm=true` 表示用户已确认继续处理关联安装地址；存在下级地址或已关联资源时必须直接拦截；实现层需保留删除审计，且优先采用逻辑删除，满足清理条件后再物理删除。

请求示例：

```http
POST /address/standard/remove/000102010000000011800001,000102010000000011800002?confirm=true
```

### 4.1.6 合并标准地址

- 方法/路径：`POST /address/standard/merge`
- 请求格式：`application/json`
- 功能描述：将多个重复或错误地址合并到一个目标地址。根据需求书备注，合并前需要校验等级关系；合并执行时需先把所有待合并地址的下级地址迁移到目标地址下，再处理源地址回收，同时同步迁移安装地址关联并记录合并日志。
- 格式要求：`sourceSegmIds` 不能为空；`targetSegmId` 不能出现在源地址集合中；目标地址与待合并地址的等级比较必须按真实 `segm_addr_type.level_id` 执行，目标地址等级数字必须小于待合并地址，即业务语义上“目标等级更高”；同等级地址禁止互相合并；低等级地址不能作为目标地址去合并高等级地址；若迁移完成后全局 `standName` 或关键展示名称重名，需要在提交前显式预警。

请求示例：

```json
{
  "sourceSegmIds": [
    "000102010000000011800001",
    "000102010000000011800002"
  ],
  "targetSegmId": "000102010000000011800003"
}
```

### 4.1.7 拆分标准地址

- 方法/路径：`POST /address/standard/split`
- 请求格式：`application/json`
- 功能描述：将一个源标准地址拆分成多条新地址。适合原始地址过粗、需要细化到单元、楼层或房间的场景。需求书明确“拆分后的地址级别不变”，且 `0323` 备注明确“源地址复制出多个同级地址、属性保持不变、源地址默认删除”，因此拆分主要改变同级名称集合，而不是改变层级；触发拆分后，系统需自动新增拆分后的标准地址，并保留拆分来源关系便于追溯。
- 格式要求：`sourceSegmId` 必填；`splitItems` 至少一条；每条拆分项当前只允许提交 `segmName`，父级、级别、管理站、接入方式等字段统一由服务层从源地址继承；拆分结果与源地址必须保持同级；默认继承源地址属性，源地址进入回收 / 删除流程，建议在库表层预留来源 / 状态字段。

请求示例：

```json
{
  "sourceSegmId": "000102010000000011800003",
  "splitItems": [
    {
      "segmName": "1单元"
    },
    {
      "segmName": "2单元"
    }
  ]
}
```

### 4.1.8 批量下级地址预览

- 方法/路径：`POST /address/standard/batchPreviewChild`
- 请求格式：`application/json`
- 功能描述：根据父地址、前缀、起止编号和后缀，预演即将生成的下级地址名称和完整地址，不真正写库。
- 格式要求：`startNum <= endNum`；一次预览数量不宜过大；返回字段主要包括 `segmName`、`standName`、`segmType`、`levelId`；用于选择父级地址的查询能力应复用名称模糊查询接口，前端单次候选限制 `200` 条。

请求示例：

```json
{
  "parentSegmId": "000102010000000011800010",
  "prefix": "第",
  "startNum": 1,
  "endNum": 3,
  "suffix": "单元"
}
```

返回示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "segmName": "第1单元",
      "standName": "江苏省南京市鼓楼区中央路88号第1单元",
      "segmType": 180009,
      "levelId": 13
    }
  ]
}
```

### 4.1.9 批量新增下级地址

- 方法/路径：
- `POST /address/standard/batchAddChild`
- `POST /address/standard/batchAddStandardAddressChildren`
- 请求格式：`application/json`
- 功能描述：批量落库创建一批下级地址，常用于批量生成单元、楼层、房间。
- 格式要求：请求体与预览接口一致；后续实现时建议在事务内执行，并限制单次最大生成数量；配套父级地址选择仍复用名称模糊查询接口，前端单次候选限制 `200` 条。

### 4.1.10 导出标准地址

- 方法/路径：`POST /address/standard/export`
- 请求格式：`application/x-www-form-urlencoded`
- 功能描述：根据筛选条件导出标准地址列表，导出的字段应与列表页字段保持一致，至少应包含地址主键、名称、全名、层级、状态、创建时间和标签信息。结合需求书补充，导出能力还需支持“导出本页数据”“导出选中数据”“按用户选择字段导出”三种模式。
- 格式要求：响应为 Excel 文件流；查询条件和列表接口一致；若传 `ids` 则按选中数据导出，若不传 `ids` 则按筛选条件导出；字段选列建议通过请求体或导出模板配置实现。

### 4.1.11 导入标准地址

- 方法/路径：`POST /address/standard/import`
- 请求格式：`multipart/form-data`
- 功能描述：导入标准地址 Excel 文件，批量写入地址数据，并记录成功数、失败数和错误信息。需求书要求系统提供固定模板下载，模板内置下拉框等限制，导入失败时返回失败明细 Excel；历史数据导入场景中还需兼容有线侧导出的 `csv` 基础数据转换。
- 格式要求：文件字段名固定为 `file`；`updateSupport` 表示是否允许更新；一二级预置地址不允许通过导入修改；导入记录必须细化到单条数据粒度；当前阶段不提供部分成功数据回滚，只要求把当前批次失败数据记录并支持 Excel 导出。

请求示例：

```http
POST /address/standard/import?updateSupport=false
Content-Type: multipart/form-data
```

### 4.1.12 下载导入模板

- 方法/路径：`POST /address/standard/import/template`
- 请求格式：无请求体
- 功能描述：下载标准地址导入模板，供导入弹窗直接使用。
- 格式要求：响应为 Excel 文件流；模板列头、级别下拉和字段命名需与当前 `spc_region + addr_segm` 口径及最新导入合同保持一致。

## 4.2 标签管理

### 4.2.1 标签列表

- 方法/路径：`POST /address/tag/list`
- 请求格式：`application/x-www-form-urlencoded`
- 功能描述：根据 `name`、`code`、`color` 及分页排序条件查询标签列表，返回标签基础信息和创建时间。
- 格式要求：标签名称模糊查询优先；返回字段至少包含 `id`、`name`、`code`、`color`、`remark`、`createTime`。

### 4.2.2 标签详情

- 方法/路径：`POST /address/tag/{id}`
- 功能描述：根据标签主键查询标签详情，用于编辑回显和地址打标签弹窗。

### 4.2.3 新增标签

- 方法/路径：`POST /address/tag`
- 请求格式：`application/json`
- 功能描述：新增一个地址标签。常见字段是名称、编码、颜色、备注。
- 格式要求：`name` 必填，且应保持唯一。

### 4.2.4 修改标签

- 方法/路径：`POST /address/tag/update`
- 请求格式：`application/json`
- 功能描述：修改标签基础信息。
- 格式要求：`id` 必填；修改后名称不能与其他标签重复。

### 4.2.5 删除标签

- 方法/路径：`POST /address/tag/remove/{ids}`
- 功能描述：删除一个或多个标签。
- 格式要求：删除时应同步清理标准地址和标签之间的关联关系。

### 4.2.6 查询地址已绑定标签

- 方法/路径：`POST /address/tag/standardAddress/{standardAddressId}`
- 功能描述：查询某个标准地址当前已经绑定的标签，返回标签列表，供详情页或编辑页展示。路径变量 `standardAddressId` 在语义上等价于 `segmId`。

### 4.2.7 批量绑定标签

- 方法/路径：`POST /address/tag/bind`
- 请求格式：`application/json`
- 功能描述：给一批标准地址绑定一批标签。
- 格式要求：请求体中 `segmIds` 和 `tagIds` 都不能为空；实现上需要保证幂等。

请求示例：

```json
{
  "segmIds": [
    "000102010000000011800001",
    "000102010000000011800002"
  ],
  "tagIds": [1, 2]
}
```

### 4.2.8 批量解绑标签

- 方法/路径：`POST /address/tag/unbind`
- 请求格式：`application/json`
- 功能描述：移除一批地址与标签之间的绑定关系。
- 格式要求：和绑定接口一样，两个 ID 集合都不能为空；重复解绑应幂等。

## 4.3 导入记录与操作日志

### 4.3.1 导入记录列表

- 方法/路径：`POST /address/import-record/list`
- 请求格式：`application/x-www-form-urlencoded`
- 功能描述：根据 `fileName`、`status`、`segmName`、`createBy` 和分页条件查询导入失败明细，返回批次号、文件名、行号、父级地址、当级名称、地址级别、失败原因和创建时间等字段。该列表默认按“单条失败明细”粒度展示，满足原型中的失败明细分页页需求。
- 格式要求：必须依赖数据库真实分页；支持按导入文件、地址名称、操作时间、操作人等条件组合筛选；页面中的批次信息需通过联表回填 `batchNo/fileName`。

### 4.3.2 导入批次详情

- 方法/路径：`POST /address/import-record/batch/{batchId}`
- 功能描述：根据导入批次主键查询批次摘要，主要用于导入结果弹窗、失败明细页顶部摘要和失败导出入口。
- 格式要求：返回字段至少包含 `batchId/batchNo/fileName/status/totalCount/successCount/failCount/updateSupport/errorMsg/createBy/createTime`。

### 4.3.3 失败明细导出

- 方法/路径：`POST /address/import-record/failure/export/{batchId}`
- 请求格式：无请求体
- 功能描述：导出指定导入批次下的失败明细 Excel，供用户修正后重新导入。
- 格式要求：只导出当前批次失败数据；响应为 Excel 文件流；不得把成功数据或其他批次失败数据混入导出结果。

### 4.3.4 操作日志列表

- 方法/路径：`POST /address/operation-log/list`
- 请求格式：`application/x-www-form-urlencoded`
- 功能描述：根据 `segmId`、`operationType`、`operator`、分页排序条件查询操作日志。返回字段至少包括日志主键、标准地址主键、操作类型、操作人、操作时间、操作详情。

### 4.3.5 操作日志详情

- 方法/路径：`POST /address/operation-log/{id}`
- 功能描述：查看一条日志的详细操作信息，用于审计追溯。

## 4.4 安装地址

### 4.4.1 安装地址列表

- 方法/路径：`POST /address/installation/list`
- 请求格式：`application/x-www-form-urlencoded`
- 功能描述：根据 `setAddrName`、`setAddrNo`、`setType`、`segmId`、`segmType`、`regionId`、`orgId` 以及分页条件查询安装地址，并补充支持需求书中提到的 `deviceId`、`standName` 关键字、`syncDate`、`associationStatus` 等条件。返回 `setAddrId`、`setAddrName`、`setAddrNo`、`setType`、`segmId`、`standName`、`regionId`、`orgId`、`notes`、`createDate` 及关联设备信息等字段。结合 `0323` 版备注，安装地址默认应关联标准地址，列表中的 `UNBOUND` 视图仅用于历史迁移、脏数据治理或异常清理。
- 格式要求：查询结果必须按当前登录组织范围过滤；安装地址与标准地址为独立实体，接口返回中应显式给出关联状态，避免仅靠 `segmId` 是否为空推断；`deviceId` 关联信息需兼容后续 BOSS 侧关联表回填；同时建议返回双向同步状态或最近同步时间，便于排查标准地址与安装地址联动异常。

### 4.4.2 安装地址详情

- 方法/路径：`POST /address/installation/{setAddrId}`；若基线代码暂时仍保留 `{id}` 占位名，语义也必须按 `setAddrId` 理解
- 功能描述：根据安装地址主键 `setAddrId` 查询单条安装地址详情，用于安装地址详情页和编辑页。详情除基础信息外，还应返回关联标准地址、关联设备信息、安装时间、同步状态等，满足需求书中“BOSS 提供关联设备信息”的描述。
- 格式要求：`setAddrId` 必须存在，且必须属于当前登录组织可见范围；详情返回建议至少覆盖 `setAddrId/setAddrName/setAddrNo/setType/segmId/segmType/regionId/orgId/notes/bossOp` 以及关联设备、同步状态、关联标准地址摘要等核心信息。

### 4.4.3 新增安装地址

- 方法/路径：`POST /address/installation`
- 请求格式：`application/json`
- 功能描述：创建安装地址，并与标准地址建立关联。默认核心写入字段是 `setAddrName/setType/segmId/segmType/regionId/orgId/notes`；外部资源、设备、同步状态等信息建议通过扩展表或外围适配层补充。结合 `0323` 版备注，正常管理端新增应默认完成标准地址关联；历史迁移阶段如需保留待治理数据，建议通过离线脚本或迁移流程落库，而不是作为常规页面写接口。
- 格式要求：`segmId` 应按业务强制必填；若确需兼容迁移态 `UNBOUND` 数据，需显式标注来源、治理状态和后续补关链路，不能与常规业务新增混用；新增成功后需同步初始化标准地址与安装地址双向同步状态。

### 4.4.4 修改安装地址

- 方法/路径：`POST /address/installation/update`
- 请求格式：`application/json`
- 功能描述：更新安装地址的名称、备注或标准地址关联关系。根据需求书补充，修改安装地址名称后还需通过接口通知 BOSS 系统相应客户安装地址变更，供前台和支撑人员联动使用。
- 格式要求：如果修改了 `segmId`、`setAddrName` 或设备信息，应同时记录同步状态和同步结果，避免本地数据与 BOSS 数据不一致；若变更了 `segmId`，还需同步维护标准地址侧关联快照与双向同步版本。

### 4.4.5 删除安装地址

- 方法/路径：`POST /address/installation/remove/{setAddrIds}`；若基线代码暂时仍保留 `{ids}` 占位名，语义也必须按 `setAddrIds` 理解
- 功能描述：删除一个或多个安装地址。
- 格式要求：`setAddrIds` 使用逗号分隔的 `varchar(24)` 字符串主键集合；删除前应校验是否仍有外部资源引用；按需求书原文，批量删除重点面向“未关联标准地址”的安装地址，因此实现时至少需要拦截已建立有效标准地址关联的数据；若系统最终仅保留治理态 `UNBOUND` 数据，则该接口主要服务历史异常数据清理，而非常规业务删除。

## 4.5 监控规则

### 4.5.1 监控规则列表

- 方法/路径：`POST /address/monitor/rule/list`
- 请求格式：`application/x-www-form-urlencoded`
- 功能描述：根据 `name`、`ruleType`、`status` 和分页排序条件查询监控规则，返回规则名称、类型、内容、状态、备注、创建时间等字段。结合需求书，规则类型至少要覆盖格式规范性检测、行政区划合规性检测、地址要素完整性检测，并预留智能异常检测扩展。

### 4.5.2 监控规则详情

- 方法/路径：`POST /address/monitor/rule/{id}`
- 功能描述：查询单条监控规则详情。

### 4.5.3 新增监控规则

- 方法/路径：`POST /address/monitor/rule`
- 请求格式：`application/json`
- 功能描述：新增一条规则，用于后续非标地址检测。
- 格式要求：建议校验规则名称唯一；`ruleType` 必须在受支持枚举内。

### 4.5.4 修改监控规则

- 方法/路径：`POST /address/monitor/rule/update`
- 请求格式：`application/json`
- 功能描述：修改规则内容、状态、备注。

### 4.5.5 删除监控规则

- 方法/路径：`POST /address/monitor/rule/remove/{ids}`
- 功能描述：删除一批规则。
- 格式要求：如果规则被任务引用，应由实现层决定是禁止删除还是先解绑。

### 4.5.6 启用监控规则

- 方法/路径：`POST /address/monitor/rule/enable/{ids}`
- 功能描述：批量把规则状态改为启用。

### 4.5.7 禁用监控规则

- 方法/路径：`POST /address/monitor/rule/disable/{ids}`
- 功能描述：批量把规则状态改为禁用。

## 4.6 监控任务

### 4.6.1 任务摘要

- 方法/路径：`POST /address/monitor/task/summary`
- 请求格式：无请求体
- 功能描述：汇总返回规则总数、启用规则数、待处理异常数、已忽略异常数、已处理异常数、最近一次手工触发新增异常数。

返回示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "totalRuleCount": 10,
    "enabledRuleCount": 8,
    "pendingRecordCount": 120,
    "ignoredRecordCount": 20,
    "processedRecordCount": 300,
    "lastCreatedCount": 18
  }
}
```

### 4.6.2 立即执行监控

- 方法/路径：`POST /address/monitor/task/execute`
- 功能描述：立即触发一次监控任务扫描，返回本次新增异常数量。

### 4.6.3 任务列表

- 方法/路径：`POST /address/monitor/task/list`
- 请求格式：`application/x-www-form-urlencoded`
- 功能描述：根据 `taskName`、`taskType`、`taskStatus`、`executeRule` 和分页条件查询监控任务，返回任务名称、类型、执行规则、执行时间、处理数、异常数、状态等字段。需求书说明的业务流程是“智能检测配置 -> 监控任务执行 -> 异常地址预警 -> 工单处理”，因此任务列表需要能回看规则、执行批次和结果概览。

### 4.6.4 任务详情

- 方法/路径：`POST /address/monitor/task/{id}`
- 功能描述：返回监控任务完整定义，包括 `monitorScope`、`relatedRuleIds`、`addressIds`、`taskDesc`、`failureReason` 等。

### 4.6.5 新增任务

- 方法/路径：`POST /address/monitor/task`
- 请求格式：`application/json`
- 功能描述：新增一条监控任务。适合配置全量扫描、定时扫描或指定范围扫描任务。

### 4.6.6 修改任务

- 方法/路径：`POST /address/monitor/task/update`
- 请求格式：`application/json`
- 功能描述：修改任务名称、执行规则、关联规则、关联地址和状态。

### 4.6.7 重跑任务

- 方法/路径：`POST /address/monitor/task/rerun/{id}`
- 功能描述：重跑指定任务，用于手工修复后复验或重新扫描。

### 4.6.8 暂停任务

- 方法/路径：`POST /address/monitor/task/pause/{id}`
- 功能描述：把任务切换为暂停状态，停止后续调度。

### 4.6.9 终止任务

- 方法/路径：`POST /address/monitor/task/terminate/{id}`
- 功能描述：终止一个任务，通常用于任务不可继续执行或配置作废场景。

## 4.7 异常地址记录

### 4.7.1 异常记录列表

- 方法/路径：`POST /address/monitor/record/list`
- 请求格式：`application/x-www-form-urlencoded`
- 功能描述：根据 `segmId`、`ruleId`、`status` 和分页条件查询异常地址记录，返回 `id`、`segmId`、`ruleId`、`standName`、`ruleName`、`status`、`notes`、`createDate`。该列表是异常地址预警页的数据来源，后续应支持直接生成标准地址修正工单。

### 4.7.2 异常记录详情

- 方法/路径：`POST /address/monitor/record/{id}`
- 功能描述：查看单条异常记录详情。

### 4.7.3 修改异常记录

- 方法/路径：`POST /address/monitor/record/update`
- 请求格式：`application/json`
- 功能描述：更新异常记录的处理状态、备注等字段。

### 4.7.4 删除异常记录

- 方法/路径：`POST /address/monitor/record/remove/{ids}`
- 功能描述：删除一个或多个异常记录。

### 4.7.5 忽略异常记录

- 方法/路径：`POST /address/monitor/record/ignore/{ids}`
- 功能描述：批量把异常记录置为“已忽略”。

## 4.8 工单

### 4.8.1 工单列表

- 方法/路径：`POST /address/work-order/list`
- 请求格式：`application/x-www-form-urlencoded`
- 功能描述：根据 `workOrderNo`、`abnormalAddress`、`workOrderStatus`、`gridId` 以及分页条件查询工单列表，返回工单号、异常地址、状态、时间、原始地址、修正地址等字段。需求书明确该模块属于“异常地址修正工单工作流”，因此列表和详情需体现工作流状态、处理人、处理时间和驳回原因。

### 4.8.2 工单详情

- 方法/路径：`POST /address/work-order/{id}`
- 功能描述：查看单个工单完整详情，包括工单基础信息和操作日志。

### 4.8.3 创建工单

- 方法/路径：`POST /address/work-order`
- 请求格式：`application/json`
- 功能描述：根据异常记录 ID 集合批量创建工单。
- 格式要求：`abnormalWarningIds` 至少一条；建议实现幂等，避免重复提单。

### 4.8.4 修正工单

- 方法/路径：`POST /address/work-order/correct/{id}`
- 请求格式：`application/json`
- 功能描述：提交人工修正后的地址，必要时同时指定网格归属。
- 格式要求：应至少传 `detailAddress` 或 `correctedAddress`；如指定 `gridId`，需校验网格存在性。

### 4.8.5 驳回工单

- 方法/路径：`POST /address/work-order/reject/{id}`
- 请求格式：`application/json`
- 功能描述：对无法处理或信息不足的工单执行驳回操作。
- 格式要求：`reason` 不能为空，并写入工单操作日志。

## 4.9 选址平台

### 4.9.1 地址关键字搜索

- 方法/路径：`POST /address/selection/search`
- 请求格式：`application/x-www-form-urlencoded`
- 功能描述：根据 `keyword`、`levelMax`、`limit` 搜索标准地址，返回匹配的标准地址列表。适合选址搜索框自动补全。根据需求书备注，搜索范围必须限定在当前所属区县公司 / 当前租户部门数据范围内；对工单系统联调场景，常见检索方式是“先查小区，再展示楼栋信息供勾选返回”。
- 格式要求：关键字为空时可按实现策略返回空或默认结果；建议只查正常状态地址；若传 `levelMax`，默认按业务展示层级过滤，若实现改为按真实 `level_id` 过滤，必须在接口合同中显式说明并与前端保持一致；若地址级别返回口径尚未最终确认，当前先兼容返回所属区县公司下可见标准地址集合。

请求示例：

```http
POST /address/selection/search
Content-Type: application/x-www-form-urlencoded

keyword=中央路88号&levelMax=10&limit=20
```

### 4.9.2 地图圈选查询

- 方法/路径：`POST /address/selection/mapSearch`
- 请求格式：`application/json`
- 功能描述：根据几何类型和坐标集合，在地图范围内搜索地址，返回地址 ID、完整标签、层级标签、公司标签、经纬度等字段。需求书备注说明 GIS 能力本期优先级较低，且地址采集依赖外部地图接口，因此本接口需明确区分“本期基础能力”和“外部地图增强能力”。
- 格式要求：`geometryType` 必填；坐标集合必须满足 GIS 几何要求；若地图接口未接通，需有降级策略或明确错误码。

### 4.9.3 房间列表

- 方法/路径：`POST /address/selection/rooms/{standardAddressId}`
- 功能描述：根据楼栋或可拆分地址查询房间列表，返回 `roomId`、`roomNo`、`segmId`。路径变量 `standardAddressId` 在语义上等价于 `segmId`。

### 4.9.4 标准地址预览

- 方法/路径：`POST /address/selection/preview/standard/{standardAddressId}`
- 功能描述：预览标准地址的展示信息，供选址平台右侧面板或确认弹窗使用。路径变量 `standardAddressId` 在语义上等价于 `segmId`。

### 4.9.5 安装地址预览

- 方法/路径：`POST /address/selection/preview/installation`
- 请求格式：`application/json`
- 功能描述：根据标准地址和可选房间集合，预览将要生成的安装地址名称、分段地址信息和展示说明。

### 4.9.6 创建房间地址

- 方法/路径：`POST /address/selection/room`
- 请求格式：`application/json`
- 功能描述：在楼栋业务语义对应的地址下创建房间标准地址，并同时生成安装地址。该能力对应需求书中“在楼栋级别选择后，输入房间号，生成新的标准地址和安装地址”的场景，生成的新标准地址应遵循既有层级规则，并与安装地址保持同步关联。
- 格式要求：`parentSegmId` 必填，且必须是“楼栋”业务语义对应的标准地址；其真实地址类型需通过 `segm_addr_type` / 联调字典解析，不允许写死固定数值；`roomName` 必填且同一楼栋下唯一；`setAddrName` 可选，不传则默认取房间号或按安装地址命名规则生成；新生成标准地址应优先核验落 `16-房间` 还是 `19-尾级地址（选址生成）`。

### 4.9.7 生成安装地址

- 方法/路径：`POST /address/selection/installation`
- 请求格式：`application/json`
- 功能描述：确认预览结果后，正式生成安装地址。该接口既服务本系统选址页面，也作为后续提供给 BOSS 系统和企业微信的选址能力基础接口。

## 4.10 标准地址扩展信息补充说明

需求书中单列了“标准地址所属管理站”和“标准地址属性管理”两个能力点。当前接口基线没有再拆独立路由，而是默认并入标准地址主资源的详情、保存、修改、导出能力统一承载，后续开发时需按以下口径落地：

- 所属管理站分为 `维修管理站`、`安装管理站`、`营业管理站` 三种类型；单条地址最多同时归属这三种不同管理站各一个。
- 管理站历史主数据当前参考 `spc_station` 并结合 `region_id` 划分；管理站类型字典来自 `pub_restriction.keyword='MANAGE_TYPE'`，其中 `2017101=维修`、`2017102=安装`、`2017103=营业`；由于源表暂不区分业务类型，系统需要在地址与管理站关系层补齐 `维修/安装/营业` 三类业务角色。
- `spc_station`、`spc_region` 已具备字段级结构索引，并且现在可与 `0323` 需求书备注合并引用；当前实现、文档与 SQL 统一使用 `spc_region` 表名。
- 地址属性至少包含 `接入方式`、`接入能力`、`城乡属性`、`房屋属性`、`覆盖户数`、`工程编号`、`是否配套费小区`。
- 如果要兼容老系统外部接口，还需进一步细化并保留 `FTTH_PON_TYPE`、`ADDR_IN_TYPE_FTTH`、`ADDR_IN_TYPE_LAN` 及其对应字典 ID，不宜只保留一个泛化的 `accessMethod` 字段。
- 因此 [标准地址详情](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/docs/reference/vibe-coding-api-manual.md#4112-标准地址详情)、[新增标准地址](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/docs/reference/vibe-coding-api-manual.md#4113-新增标准地址)、[修改标准地址](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/docs/reference/vibe-coding-api-manual.md#4114-修改标准地址)、[导出标准地址](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/docs/reference/vibe-coding-api-manual.md#4110-导出标准地址) 必须覆盖这些字段。
- 对数据库设计而言，管理站关系建议独立关系表，地址属性建议使用扩展表；若接入能力为多选，优先使用明细关系表而不是单字符串硬编码。

## 4.11 标准地址外围接口补充说明

- 标准地址查询接口需提供给工单系统，典型用法是“先按小区名称查询，再返回楼栋信息供勾选”。
- 选址平台能力需提供给 BOSS 系统和企业微信复用。
- 标准地址数据信息需提供数据库查询接口或等价数据服务给大数据平台。
- 上述对外能力虽然当前未在本册中逐个拆成独立开放平台章节，但后续设计外部接口清单时，应以本册中 `标准地址查询 / 选址 / 工单修正 / 安装地址` 相关能力为内核映射。

## 4.12 旧系统兼容接口

本节用于固化“新系统需要对外兼容老系统调用口径”的能力，不替代本系统内部管理端接口。凡是 legacy 兼容接口，优先保留原协议、原路径、原字段名和原返回结构。

### 4.12.1 URL 选址入口兼容

- 入口形态：页面 URL。
- 典型参数：`systemsource=BOSS`、`method=checkAddressForBOSS`、`uuId`、`areaCode`、`cipherIndex`、`cipherType`、`cipherText`、`staffNbr`。
- 功能描述：兼容旧 BOSS 页面通过加密时间戳跳转选址平台的能力。服务端需要根据 `cipherIndex` 找到生效密钥，解密 `cipherText`，校验密文中的 `invokeTime` 是否在允许时间差范围内，通过后再重定向到实际业务页面。
- 实现要点：密钥轮换、超时校验、失败跳错页、成功透传 `uuId/staffNbr/areaCode`，以及对入口请求做安全审计。

### 4.12.2 旧地址树查询兼容

- 方法/路径：`GET /addr/compare.spr?method=getChildAddr&resObjectId=...`
- 请求格式：URL Query
- 响应格式：XML
- 功能描述：按 legacy 区域 / 地址 ID 查询下级地址树节点，供旧 APP 或旧选址前台逐级展开地址树。
- 兼容要求：响应中的节点 `id`、`text`、`child` 和 `userdata[segmType]` 均需保持旧口径，不能替换成内部 `Long` 主键或新枚举值。

### 4.12.3 旧地址名称模糊查询兼容

- 方法/路径：`GET /ibuz/buzInterface.spr?method=getAddress&type=1&limit=20&keyword=...`
- 请求格式：URL Query
- 响应格式：JSON 明文
- 功能描述：根据多级地址关键字模糊匹配地址结果，旧系统约定多级名称用 `+` 号拼接，例如“省+市+公司+乡镇+小区”。
- 兼容要求：响应仍返回 `sessionId + data` 结构，其中 `data` 为字符串化 JSON 结果集；结果项至少保留 `segmId/fullNameList/fullName/segmType/regionId/resourceSegment`。
- 实现建议：检索能力优先由搜索索引支撑，再在适配层还原旧报文格式。

### 4.12.4 旧标准地址创建兼容

- 方法/路径：`POST /ibuz/buzInterface.spr?method=saveAddressNew`
- 请求格式：`application/x-www-form-urlencoded`
- 响应格式：JSON
- 功能描述：在已有楼栋或上级分段地址下，根据 `objStr.unitName/floorName/roomName` 生成新的标准地址和安装地址，并返回 legacy 标识。
- 兼容要求：请求体字段名、`objStr` 嵌套字段名、响应字段 `segm_id/set_id/station_id` 等需保持旧命名；不能只返回内部接口册定义的新字段。
- 数据要求：内部模型需能从上级地址、地址层级、管理站、安装地址联动组装返回对象。

### 4.12.5 旧选址结果对象兼容

- 输出形态：页面对象 `obj`
- 功能描述：旧选址页面确认结果后，需要将标准地址、安装地址及接入能力相关字段组装为 `obj` 回传给 BOSS 前台。
- 兼容要求：至少保留 `obj.uuId/obj.segm_id/obj.stand_name/obj.set_id/obj.set_name/obj.FTTH_PON_TYPE_ID/obj.FTTH_PON_TYPE/obj.ADDR_IN_TYPE_FTTH_ID/obj.ADDR_IN_TYPE_FTTH/obj.ADDR_IN_TYPE_LAN_ID/obj.ADDR_IN_TYPE_LAN`。

### 4.12.6 BOSS 标准地址变更通知兼容

- 协议：SOAP
- 方向：我方主动通知 BOSS
- 功能描述：当标准地址或安装地址变更且需要同步给 BOSS 时，按旧文档中的 SOAP 包装结构发送消息，核心业务字段放在 `requestContent` 的 CDATA `oss-request` 中。
- 兼容要求：`requestSystemNo=4`、`requestNo=4` 以及 `custId/setAddrId/setAddrName` 和三组接入能力字段需保持旧口径；响应需解析 `return-code/return-message`。
- 实现建议：不要把该能力直接写成普通管理端 Controller，建议单独做外部适配器和重试补偿。

### 4.12.7 BOSS 地址同步接口兼容

- 协议：XML
- 方向：双向同步或按联调方案实现
- 功能描述：兼容旧 BOSS 以 XML 报文发起的地址同步请求，支持 `ADD/UPDATE/QUERY` 动作，并根据 `SEGM_TYPE` 区分标准地址和安装地址。
- 兼容要求：请求和响应中的 `DOMAIN_SEND/DOMAIN_RECEIVE/REGIONCODE/OPERATION_NAME/REQUEST_INFORMATION` 结构应原样保留；响应内需返回 `COVERNUM/STATION_ID/INTYPE/STAND_NAME/SET_ADDR/...` 等字段。
- 设计影响：该接口说明新系统必须保留 legacy 标识字段、管理站 ID、覆盖户数以及接入方式字典值，且能按旧字段直接序列化。

### 4.12.8 BOSS 地址查询接口兼容

- 协议：XML
- 方向：BOSS 查询我方
- 功能描述：按单条 `SEGM_ID + SEGM_TYPE` 查询地址详情。旧文档明确“地址查询不需要变更标准地址、安装地址的同步标识”，因此此接口应是纯查询型。
- 兼容要求：返回结构与地址同步接口高度一致，但调用语义上不能误触发同步状态更新。

### 4.12.9 旧地址查询 URL 页面兼容

- 入口形态：固定页面 URL
- 功能描述：兼容旧前台通过 `query.jsp` 打开地址查询页面的能力。
- 兼容要求：若当前系统保留该入口，应通过适配页或路由转发方式兼容，不建议把旧页面直接嵌入内部管理端。

### 4.12.10 客户信息同步接口说明

- 旧文档还定义了 `单客户信息同步` 和 `批量客户信息同步`，包含 `CustomerInfo`、`SubsInfo` 等大量客户域字段。
- 当前判断：这部分更像 BOSS/客户中心对外能力，不直接归属标准地址主数据模型。
- 本册处理方式：参数口径已在 [第 3.14 节](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/docs/reference/vibe-coding-api-manual.md#314-旧系统兼容接口参数) 保留；当前先列为待需求规则澄清事项，不阻塞标准地址主模型设计。

### 4.12.11 旧系统兼容待需求规则澄清事项

- `单客户信息同步 / 批量客户信息同步` 是否由 `ruoyi-address` 模块直接承接，还是仅由外部适配层承接。
- legacy `地址同步接口 / 地址查询接口` 的方向是否需要双向都实现，`ADD/UPDATE/QUERY` 各动作的触发时机和责任方需明确。
- legacy `segmId/resObjectId/setAddrId/regionId/companyId/stationId/segmType` 的来源规则需明确：是沿用迁移值、按旧规则生成，还是维护独立映射关系。
- `fullNameList/resourceSegment/data` 等旧查询返回字段，是否必须与旧系统逐字符兼容，还是允许“结构一致、展示近似”。
- URL 选址入口中 `cipherIndex/cipherText/invokeTime` 的密钥管理方式、容差时间和轮换策略需明确。
- SOAP 标准地址变更通知和 XML 地址同步 / 查询接口的编码、字符集、超时、重试、幂等与报错码口径需明确。
- 老接口中的 `companyId/regionId/station_id/org_id` 与新系统组织、区域、管理站模型之间的映射规则需明确。
- legacy 查询能力是否要求继续支持旧页面 `query.jsp` 的完整前台行为，还是只保留兼容跳转入口。

## 5. 网格模块

本章重点说明接口用途、路由和业务语义；每个接口具体可传字段、必填项、格式和参数示例，统一以上文第 3 节对应模型为准。

## 5.1 网格组织

### 5.1.1 组织树

- 方法/路径：`POST /grid/org/tree`
- 请求格式：无请求体
- 功能描述：查询完整组织树，返回 `id`、`parentId`、`orgName`、`status`、`children`，供左侧树组件和组织选择框使用。结合需求书，组织树查询需受当前操作人员权限控制，只返回当前租户、当前可见组织范围。

### 5.1.2 组织详情

- 方法/路径：`POST /grid/org/{id}`
- 功能描述：根据组织主键查询详情，用于组织详情页和编辑页。

### 5.1.3 新增组织

- 方法/路径：`POST /grid/org`
- 请求格式：`application/json`
- 功能描述：新增一个组织节点，常用于公司、片区、站点等层级组织维护。
- 格式要求：同级 `orgName` 应唯一；`parentId` 允许为空或 0 表示顶层。

### 5.1.4 修改组织

- 方法/路径：`POST /grid/org/update`
- 请求格式：`application/json`
- 功能描述：修改组织名称、状态、备注或父级归属。
- 格式要求：调整父级时必须避免形成环形树。

### 5.1.5 删除组织

- 方法/路径：`POST /grid/org/remove/{id}`
- 功能描述：删除单个组织。
- 格式要求：若组织下存在子组织，则必须直接拦截删除；如还关联网格、经理等资源，也应优先拦截或先迁移。

### 5.1.6 导入组织

- 方法/路径：`POST /grid/org/importData`
- 请求格式：`multipart/form-data`
- 功能描述：批量导入组织结构调整数据。
- 格式要求：需支持重复导入校验、历史迁移兼容和层级顺序校验。

### 5.1.7 导出组织

- 方法/路径：`POST /grid/org/export`
- 功能描述：根据筛选条件导出组织树或组织平铺列表。

## 5.2 网格主资源

### 5.2.1 网格列表

- 方法/路径：`POST /grid/info/list`
- 请求格式：`application/x-www-form-urlencoded`
- 功能描述：根据 `gridId`、`gridName`、`gridProperty`、`gridManager`、`orgId` 以及分页条件查询网格列表，返回网格编码、名称、属性、经理、归属站点、组织等字段。需求书明确该列表默认查询“当前组织下”的网格，因此实现时需自动叠加当前组织 / 权限范围过滤。

### 5.2.2 网格详情

- 方法/路径：`POST /grid/info/{id}`
- 功能描述：查看单个网格详情。

### 5.2.3 新增网格

- 方法/路径：`POST /grid/info`
- 请求格式：`application/json`
- 功能描述：新增一个网格主资源。
- 格式要求：`gridId` 应唯一；`gridName`、`gridProperty`、`belongStation` 建议必填；组织和经理需要校验存在性；结合需求书备注，仅允许在“广电站”节点下新增网格。

### 5.2.4 修改网格

- 方法/路径：`POST /grid/info/update`
- 请求格式：`application/json`
- 功能描述：更新网格信息，涉及组织归属、经理归属、网格属性等。根据需求书，核心修改项至少包括网格名称和网格关联的网格经理；同时需兼容历史数据迁移、双向增量同步以及外围系统同步口径。
- 格式要求：实现时要预留历史迁移和双向增量同步；如果网格经理发生变化，建议同步刷新网格经理关联表和对外同步任务。

### 5.2.5 删除网格

- 方法/路径：`POST /grid/info/remove/{ids}`
- 功能描述：删除一个或多个网格。
- 格式要求：需求书明确要求“删除前强校验该网格下是否有关联客户，如有则不允许删除”；客户关联校验需同时覆盖“网格地址划分衍生客户”和“手工网格客户划分”两类来源；在此基础上，可继续补充地址、经理等关联校验，但客户校验属于硬约束。

### 5.2.6 导出网格

- 方法/路径：`POST /grid/info/export`
- 功能描述：导出网格列表。

## 5.3 网格地址

### 5.3.1 网格地址列表

- 方法/路径：`POST /grid/address/list`
- 请求格式：`application/x-www-form-urlencoded`
- 功能描述：根据 `addressId`、`addressName`、`gridId`、`gridName` 及分页条件查询地址和网格之间的关联关系，返回关联 ID、地址 ID、地址名称、网格 ID、网格名称。

### 5.3.2 批量关联网格地址

- 方法/路径：`POST /grid/address/bind`
- 请求格式：`application/json`
- 功能描述：把一批地址绑定到某个网格，是网格归属的核心写入口。根据需求书，地址划分对象应是楼栋级标准地址；关联成功后，楼栋下所有客户需要自动划入该网格。
- 格式要求：`gridId` 必填；`addressIds` 至少一条；`addressIds` 应校验为楼栋级标准地址，其中“楼栋级”必须通过 `segm_addr_type` / 类型映射解析，不能硬编码固定层级数字；单条楼栋级标准地址仅可关联一个网格；地址绑定后需按“标准地址 -> 安装地址 -> 客户”链路衍生客户归属，并做好去重；后续实现应处理已有归属覆盖策略、客户自动归属、历史迁移与双向同步。

请求示例：

```json
{
  "gridId": 3001,
  "addressIds": [10001, 10002]
}
```

### 5.3.3 导入网格地址

- 方法/路径：`POST /grid/address/importData`
- 请求格式：`multipart/form-data`
- 功能描述：批量导入地址与网格的归属关系。需求书要求提供固定调整模板下载，管理员按模板填充后上传，系统自动校验并刷新网格与标准地址关联关系。
- 格式要求：需要支持历史数据迁移、重复导入校验和双向增量同步基线；导入成功后要自动刷新地址归属与派生客户归属。

### 5.3.4 导出失败数据

- 方法/路径：`POST /grid/address/exportFail/{recordId}`
- 功能描述：根据导入记录导出失败明细，方便修正后重导。

## 5.4 网格客户

### 5.4.1 网格客户列表

- 方法/路径：`POST /grid/customer/list`
- 请求格式：`application/x-www-form-urlencoded`
- 功能描述：根据 `customerId`、`customerName`、`phone`、`address`、`gridId` 和分页条件查询客户归属情况，返回客户标识、姓名、电话、地址、网格信息等字段。结合需求书备注，该列表默认仅展示“手工网格客户划分”数据，不直接混入地址划分自动衍生结果。

### 5.4.2 绑定客户到网格

- 方法/路径：`POST /grid/customer/bind`
- 请求格式：`application/json`
- 功能描述：把一批客户直接绑定到一个网格，适合手工纠偏或特殊业务场景。需求书明确客户与网格是强关联一对一关系，且若该关系与网格地址划分结果冲突，以客户直接绑定为准。
- 格式要求：`gridId` 必填；`customerIds` 至少一条；实现时要处理幂等和跨组织校验；需要显式标记该关系来源为“手工客户划分 / BOSS 调整”等，便于覆盖自动归属；一旦发生手工划分，后续地址划分结果不应再覆盖，但允许再次手工调整。

## 5.5 网格经理

### 5.5.1 网格经理列表

- 方法/路径：`POST /grid/manager/list`
- 请求格式：`application/x-www-form-urlencoded`
- 功能描述：根据 `managerId`、`managerName`、`orgId`、`gridId` 和分页条件查询网格经理列表，返回经理编号、姓名、组织、电话、已绑定网格等字段。列表查询范围默认受当前组织及权限控制。

### 5.5.2 网格经理详情

- 方法/路径：`POST /grid/manager/{id}`
- 功能描述：查看单个网格经理详情，重点返回 `managerId`、`managerName`、`orgId`、`orgName`、`phone`、`gridIds`。

### 5.5.3 新增网格经理

- 方法/路径：`POST /grid/manager`
- 请求格式：`application/json`
- 功能描述：新增一个网格经理，并可同时绑定多个网格。
- 格式要求：`managerName`、`orgId` 建议必填；`gridIds` 可为空；手机号格式建议统一校验。

### 5.5.4 修改网格经理

- 方法/路径：`POST /grid/manager/update`
- 请求格式：`application/json`
- 功能描述：修改经理基础信息、组织归属和关联网格。

### 5.5.5 删除网格经理

- 方法/路径：`POST /grid/manager/remove/{id}`
- 功能描述：删除网格经理。
- 格式要求：需求书明确要求删除前强校验该网格经理已无关联网格；只有解除全部网格绑定后才允许删除。

### 5.5.6 绑定网格

- 方法/路径：`POST /grid/manager/bindGrid/{id}`
- 请求格式：`application/json`
- 功能描述：为指定经理绑定或重绑一批网格。
- 格式要求：请求体重点使用 `gridIds`；实现时要处理幂等和跨组织校验。

### 5.5.7 导出网格经理

- 方法/路径：`POST /grid/manager/export`
- 功能描述：导出网格经理列表。

## 5.6 网格模块对外接口补充说明

- 网格客户划分接口需要提供给 BOSS 系统，供营业员在 BOSS 前台直接修改单个客户所属网格。
- 网格与客户关联关系数据接口需定时同步给大数据平台、BOSS 系统、工单系统、企业微信。
- 网格信息、网格经理信息数据接口也需定时同步给大数据平台、BOSS 系统、工单系统、企业微信。
- 因此网格主数据、客户关系、经理关系三类表建议统一设计变更时间、同步状态、版本号等字段，方便后续增量同步。

## 6. 给 AI 的直接提示词

```text
请基于 RuoYi-Cloud-Plus 中 ruoyi-address 模块的接口基线开发。
接口文档以 `ruoyi-modules/ruoyi-address/docs/reference/vibe-coding-api-manual.md` 为准。
所有接口统一使用 POST，不允许 GET。
统一响应使用 R，分页响应使用 TableDataInfo，分页参数使用 PageQuery。
查询类接口默认按 application/x-www-form-urlencoded 绑定 Bo + PageQuery。
新增、修改、绑定、拆分、合并等写接口默认按 application/json 实现。
导入接口使用 multipart/form-data，字段名固定为 file。
不要擅自新增接口清单之外的业务接口。
优先复用 ruoyi-address 已有 StandardAddress 相关 Service/Mapper 风格。
网格模块当前没有现成实现，请按 controller -> service -> mapper -> domain 的顺序补齐。
实现时重点处理层级校验、幂等、删除约束、导入失败明细记录与导出、历史迁移、双向增量同步和操作日志。
同时覆盖租户/部门数据范围过滤、地址拼装简拼级联刷新、单条导入明细记录、管理站关系、地址属性扩展、`isCity + areaType` 组合判定、批量新增 `200` 条候选限制、安装地址默认关联标准地址、楼栋级网格唯一归属及客户覆盖优先级；其中“楼栋级”统一按业务语义 + 类型映射解析，不按固定数值硬编码。
```

## 7. 维护建议

- 如果改路径或 HTTP 方法，先改接口基线源码，再改本文。
- 如果改字段结构，先改 `BO / VO`，再改本文示例。
- 如果要给外部团队、AI 或供应商继续开发，优先发这份文档，不要只发前端页面截图。
