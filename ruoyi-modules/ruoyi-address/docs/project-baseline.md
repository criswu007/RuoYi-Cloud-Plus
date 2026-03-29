# 标准地址与网格项目总控基线

> 本文档是 `project-config.md` 与 `development-plan.md` 的合并版，用于统一项目范围、开发顺序、风险和外部依赖。自本版起，库表开发基线请优先参考 [historical-schema-baseline.md](./historical-schema-baseline.md)。

## 1. 当前范围

- 服务形态：`ruoyi-address` 作为独立微服务承载标准地址与网格相关能力。
- 能力定位：标准地址模块必须与网管功能模块低耦合，可独立运行并向大数据平台、工程系统、BOSS、工单系统、企业微信等外围系统提供能力与数据支撑。
- 地址范围：层级与类型以 `segm_addr_type.level_id` 排序结果为准，`0323` 需求书当前列出 `19` 类地址类型。
- 选址平台能力当前要求支持到楼栋级选址，并支持从楼栋生成房间或选址尾级标准地址及安装地址；“楼栋级”是业务语义，不再绑定为固定数字级次，选址生成地址的真实类型需结合 `segm_addr_type` / 联调库字典核定。
- 本期范围：标准地址、安装地址、网格、非标地址监控、外部系统对接。
- 本期不做：工单系统本身，仅做工单对接；新旧系统最终替换关系暂不在本轮冻结。
- 基础保护：一二级标准地址视为外部预置只读数据，不允许新增、修改、删除、合并、拆分。
- 标准地址基础数据：需求书明确要求具备江苏全省范围标准地址全量信息，并同步到本项目作为标准地址基础数据。

## 2. 开发总原则

- 自本版起，标准地址与网格开发一律优先基于“当前线上非备份表结构 + 南京历史批次专题 + `0323` 需求书备注”三类材料联合判定，不再以旧版目标模型设计稿作为主依据。
- 业务规则仍以 `/Users/criswu/Desktop/广电/需求/技术需求书_标准地址_待明确问题0323.docx` 及备注为最高优先级；但若要落库、建模、兼容 legacy 接口，必须先回到历史结构核实。
- 标准地址查询口径需兼容 legacy：`1/2` 级行政区划优先按 `spec_region` / `spc_region` 口径处理，其余层级按 `ADDR_SEGM` / 历史标准地址事实表处理，层级定义以 `segm_addr_type.level_id` 为准。
- `城区/非城区` 与 `城乡属性` 必须拆开理解：前者复用 `ADDR_SEGM.is_city`，后者复用 `ADDR_SEGM.area_type`。
- 安装地址正常业务口径下默认必须关联标准地址；未关联视图仅用于历史迁移、脏数据治理和异常清理。
- 管理站主数据当前参考 `spc_station`，并按 `region_id` 划分；业务角色需在关系层补齐 `维修/安装/营业` 三类。
- 标准地址、安装地址、网格的设计都必须为后续资源分配、网络激活、告警群障、工单处理等上游模块留出稳定查询和联动入口。
- 需求书中的 AI 智能体、报表统计、菜单管理不直接等于 `ruoyi-address` 新建完整子系统；当前模块主要负责提供地址、网格、接口、菜单元数据和知识沉淀的承载基础。

## 3. 研发阶段

### M1 需求与结构基线冻结

- 交付物：范围清单、地址层级定义、字段字典、历史结构开发基线、接口边界基线。
- 关键动作：统一 `spec_region/spc_region` 口径；冻结 `8/9` 级规则；明确地址、安装地址、网格开发分别依赖哪些历史表。

### M2 标准地址核心能力

- 交付物：标准地址查询、详情、新增、修改、删除、导入、导出、日志。
- 关键动作：按历史字段落地拼装名称、简拼、层级校验、删除校验和导入回滚。

### M3 关联与扩展能力

- 交付物：安装地址关联、标签与属性扩展、管理站关联。
- 关键动作：补齐标准地址与安装地址双向关联链路，按历史结构兼容管理站和属性口径。

### M4 治理与复杂操作

- 交付物：合并、拆分、回滚、审计、异常治理能力。
- 关键动作：补齐层级变更、资源迁移、重名预警和回收策略。

### M5 网格模块与外围联动

- 交付物：网格组织、网格、网格地址、网格客户、网格经理及外围同步能力。
- 关键动作：按历史网格结构和样例表推进，不脱离 `GRID_* / ORGANIZATION_* / CUST_*` 历史口径单独起模型。

### M6 监控、对接、验收上线

- 交付物：非标地址监控、BOSS/工单/企业微信/大数据/地图对接、验收材料、上线与回滚方案。

## 4. 上下游责任边界

- 对资源配置模块：需提供按标准地址进行资源设计分配所需的标准地址、安装地址、地址属性和管理站信息。
- 对业务开通 / 工单模块：需提供标准地址查询、选址、安装地址查询、异常地址修正工单支撑能力。
- 对告警监控 / 客服模块：需支持通过地址与网格关系反查影响设备、用户、网格，为群障识别和自动拦截提供数据基础。
- 对外围 AI 能力：当前优先沉淀结构化接口文档、菜单元数据和可检索知识入口，不在 `ruoyi-address` 内直接扩完整 AI 中台。

## 5. 外部系统范围

- BOSS：legacy 入口兼容、选址复用、安装地址变更通知、网格客户划分联调。
- 工单系统：标准地址查询、异常地址修正工单、网格关系消费。
- 企业微信：选址复用、告警通知、网格数据消费。
- 大数据平台：标准地址主数据、网格/客户/经理同步与查询服务。
- 地图系统：地理编码、坐标与地图圈选能力。
- 需求书额外点名的外联系统：宽带质量监测平台、各地自建网管系统、客服系统、设备原厂网管；当前在 `ruoyi-address` 中至少需要预留地址 / 网格 / 选址 / 查询的可复用接口边界。

## 6. 全局工程约束

- 权限与审计：需支持基于角色的访问控制，权限至少细到菜单级；用户与日志管理需满足分权分域和至少三个月日志审计要求。
- 安全基线：接口默认走 HTTPS；敏感配置与密码类信息需加密存储；实现过程中不能留下 SQL 注入、XSS、SSRF、XXE、弱口令、代码执行、文件包含等明显风险。
- 性能指标：需面向全省场景，支持至少 `3000` 人同时使用；登录时间 `<=3s`，页面查询渲染 `<=5s`，在线事务处理 `<=5s`。
- 可用性与冗余：系统整体可用性目标 `99.99%`；应用层和数据层不得单点部署；需支持热备、异地容灾和故障切换。
- 数据库适配：实现上需尽量采用标准 SQL 和通用 JDBC 能力，避免把 `ruoyi-address` 写死到单一数据库语法；项目目标要求至少适配两款主流国产关系型数据库。
- 接口与文档：需求书明确要求对外提供完整 RESTful API 等接口说明文档，并对本系统接口及外联系统平台接口做健康度监控报警。
- 源码交付要求：关键函数和复杂逻辑要有清晰注释，并配套架构文档、模块说明文档；源码需可独立编译运行，且与最终交付版本一致。

### 6.1 标准地址命名与代码落地约束

- 标准地址主模型在 `Controller/Service/BO/DTO/VO/Entity/Mapper/SQL` 中优先使用 `segmId/parentSegmId/segmName/segmNo/standName/standNo/segmType/levelId/regionId/districtId/serviceRegionId/notes`，不再默认落成 `id/name/fullName/code/remark` 一类泛化命名。
- 安装地址主模型优先使用 `setAddrId/setAddrName/setAddrNo/setType/segmId/segmType/regionId/orgId/bossOp/notes`，不再把 `installName/standardAddressId/resourceType` 作为内部主命名继续扩散。
- `segmType` 与 `levelId` 必须拆开建模：`segmType` 对应真实存储字段 `ADDR_SEGM.segm_type` 或 `ADDR_SET_SEGM.segm_type`，`levelId` 仅表示根据 `segm_addr_type.level_id` 投影后的层级语义；除 legacy 展示或兼容层外，不允许把 `level` 当成真实落库字段直接使用。
- `status` 不允许默认按 `0/1` 简化理解；标准地址状态需优先按 `ADDR_SEGM_STATUS` 等线上字典值或明确映射处理，监控、工单、组织等其他资源再按各自枚举单独定义。
- 标准地址和安装地址主键默认按线上库 `varchar(24)` 字符串主键处理，不允许在未核实实表前默认写成 `Long`；批量参数、集合变量和路径占位名也应优先收口为 `segmIds/setAddrIds` 语义。
- 管理站、接入能力、城乡属性、房屋属性、覆盖户数等扩展字段命名应优先使用 `stationId/installStationId/busStationId/addrInTypeFtth/ftthPonType/addrInTypeLan/areaType/placeType/coverNum/singleProjectCode/supportingFeeCommunityFlag`，避免使用容易误读的抽象名。
- 若线上物理字段名本身存在历史遗留歧义，例如 `post_code`、`segm_name_fir`、`installstation_id`、`busstation_id`，代码层应采用“语义清晰的变量名 + 注释标注物理字段与释义”的方式收口，不能只沿用含义模糊的物理名或旧泛化名。
- legacy 兼容层可保留 `standardAddressId/fullName/accessMethod` 等旧字段名以满足外部协议，但内部主模型、Mapper 别名、SQL 列别名和新增接口合同不得继续扩散这类泛化命名；若路径占位名暂时仍为 `id` 或 `standardAddressId`，实现中必须在注释和变量命名上明确其真实语义分别是 `segmId` 或 `setAddrId`。

## 7. 标后测试门槛

- 中标公告结束后 `5` 个工作日内完成测试环境搭建与应用部署。
- 正式测试时间 `3` 小时。
- 共 `33` 个测试项，其中带 `★` 的测试项必须全部满足。
- `33` 项测试里通过数量不得少于 `30` 项。
- 对 `ruoyi-address` 直接相关的重点测试项包括：`2.8.4.12` 到 `2.8.4.31`，覆盖标准地址、安装地址、非标监控、选址、管理站、属性、网格和系统接口。

## 8. 风险与预案

- 新旧系统关系未冻结：需预留全量导入、增量同步和双轨运行扩展点。
- 历史字段质量不稳定：对 `station`、`area_type`、安装地址挂接等保留治理和兼容策略。
- 网格当前在线库未发现正式业务表：需要以历史样例、装载 SQL 和导入链路作为先行开发依据。
- 数据量较大：批量处理默认考虑分页、异步、失败明细导出与回滚。
- 需求命名与历史实表命名存在差异：实现前必须以联调库实际表名复核。
- 标后测试门槛高：实现和文档都不能只满足“能跑通”，还要对齐 star 项全通过和接口可配置可查看的验收口径。

## 9. 关键参考

- 历史结构开发基线：[historical-schema-baseline.md](./historical-schema-baseline.md)
- 接口能力基线：[interface-checklist.md](./interface-checklist.md)
- 网格历史结构摘要：[historical-inventory/grid-module-structure-baseline.md](./historical-inventory/grid-module-structure-baseline.md)
- 历史盘点索引：[historical-inventory/README.md](./historical-inventory/README.md)
- 详细接口参考册：[reference/vibe-coding-api-manual.md](./reference/vibe-coding-api-manual.md)
- 旧版合并前文档归档：
  - [archive/project-config.md](./archive/project-config.md)
  - [archive/development-plan.md](./archive/development-plan.md)
  - [archive/external-integration.md](./archive/external-integration.md)
