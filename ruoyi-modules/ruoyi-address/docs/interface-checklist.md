# 标准地址与网格模块接口清单基线

## 使用说明
- 后续开发默认以本文件和下列接口基线源码为准，不再以当前前端 mock 逻辑为准。
- 自本版起，接口落地涉及库表和字段时，默认以 [historical-schema-baseline.md](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/docs/historical-schema-baseline.md) 为主开发基线，不再以旧版目标模型设计稿为准。
- 最新需求书正文与备注以 `/Users/criswu/Desktop/广电/需求/技术需求书_标准地址_待明确问题0323.docx` 为准，优先级高于旧版需求书、原型与前端 mock 表达。
- 历史字段语义、结构注释、值域和类型映射允许联合引用两类材料：线上最新非备份表盘点负责当前结构和字段注释，归档南京批次专题负责迁移差异、旧系统现象和历史样本补充。
- 若线上结构、归档专题与 `0323` 需求书备注冲突，以需求书为准；若需求书与联调库实表命名冲突，以联调库核验结果回写文档。
- 所有接口统一采用 `POST`，不再使用 `GET`。
- 仅限本系统内部管理端接口执行上述 `POST` 约束；旧系统兼容接口需保持原有 `GET/URL/JSON/XML/SOAP` 协议与报文形态，不强行改造。
- 标准地址模块接口基线源码：
- [StandardAddressAdminApi.java](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressAdminApi.java)
- 网格模块接口基线源码：
- [GridManagementAdminApi.java](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/GridManagementAdminApi.java)
- 面向 AI 开发的详细接口册：
- [reference/vibe-coding-api-manual.md](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/docs/reference/vibe-coding-api-manual.md)
- 标准地址补充 BO/VO：
- [StandardAddressAdminBo.java](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressAdminBo.java)
- [StandardAddressAdminVo.java](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressAdminVo.java)
- 网格模块 BO/VO：
- [GridManagementBo.java](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/GridManagementBo.java)
- [GridManagementVo.java](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/GridManagementVo.java)

## 如何调整
- 如果后续你要调整接口范围、路径、入参或出参，优先修改本文件中的接口清单描述。
- 然后同步修改对应接口基线源码和 BO/VO 字段，保证“文档清单”和“代码合同”始终一致。
- 如果调整涉及表结构和字段语义，先回到历史结构基线和历史盘点文档确认，避免接口先于历史结构另起命名。
- 如果只是字段级微调，最常改的是 `domain/bo`、`domain/vo` 下的补充模型文件。
- 如果是接口路径、方法或能力点调整，最常改的是 `controller` 下的两个接口基线文件。
- 自本版起，文档中的标准地址 / 安装地址字段命名优先沿用线上库 canonical 字段或其明确 camelCase 变体：
- 标准地址优先使用 `segmId/parentSegmId/segmName/segmNo/standName/standNo/segmType/levelId/regionId/districtId/serviceRegionId/notes/stationId/installStationId/busStationId/addrInTypeFtth/ftthPonType/addrInTypeLan/areaType/placeType/coverNum`。
- 安装地址优先使用 `setAddrId/setAddrName/setAddrNo/setType/segmId/segmType/regionId/orgId/notes/bossOp`。
- `id/name/fullName/code/remark/standardAddressId/installName/accessMethod/accessCapabilityCodes/urbanRuralAttr/houseProperty/coverageHouseholds/projectNumber` 等泛化命名不再作为新一轮详细设计默认名；若因兼容旧接口暂时保留，必须在文档中显式标注其对应物理字段。

## 系统级实现前置约束

- `ruoyi-address` 不是孤立后台页面，而是“可独立运行的地址与网格能力系统”；接口和数据设计必须能被 BOSS、工单系统、企业微信、大数据平台等外围系统复用。
- 标准地址、安装地址、网格接口都要为资源配置、网络激活、告警群障、客服拦截等上游模块预留稳定查询和反查能力。
- 所有接口需基于用户权限工作；权限粒度至少支撑菜单级访问控制，查询类接口默认受 `tenantId/deptId` 与数据权限范围过滤。
- 需保留完整接口说明文档，并对本系统接口及外联系统平台接口提供健康度监控报警能力。
- 安全基线默认按招标要求执行：HTTPS 传输、输入校验、敏感信息加密存储、日志审计可追溯。
- 用户与日志管理需满足至少三个月日志存储审计要求；地址操作日志、导入日志、外部接口报文日志都不能缺位。

## 内部核心接口
- 标准地址：
- 查询列表、详情、新增、修改、删除、批量下级地址预览、批量新增、导入、导出、导入撤回
- 合并、拆分
- 所属管理站、地址属性作为标准地址主资源扩展字段统一承载
- 标签管理：
- 标签列表、详情、新增、修改、删除、批量绑定、批量解绑
- 导入记录与操作日志：
- 导入记录列表、详情
- 操作日志列表、详情
- 安装地址：
- 查询列表、详情、新增、修改、删除
- 非标地址监控：
- 规则配置、规则启停、监控任务摘要、任务分页、任务详情、新增、修改、重跑、暂停、终止
- 异常地址：
- 异常记录列表、详情、修改、删除、忽略
- 工单：
- 工单列表、详情、创建、修正、驳回
- 选址平台：
- 模糊查询、地图圈选、房间查询、标准地址预览、安装地址预览、房间地址生成、安装地址生成
- 网格管理：
- 组织树、组织详情、新增、修改、删除、导入、导出
- 网格基础信息：
- 列表、详情、新增、修改、删除、导出
- 网格地址：
- 列表、批量绑定、导入、失败数据导出
- 网格客户：
- 列表（默认仅展示手工划分数据）、批量绑定
- 网格经理：
- 列表、详情、新增、修改、删除、绑定网格、导出

## 关键业务规则接口
- 所有查询、导出、选址、安装地址查询需按 `tenantId/deptId` 和数据权限范围过滤。
- 标准地址列表/查询兼容 legacy 数据来源：当筛选或返回 `1/2` 级行政区划时，统一按 `spc_region` 口径处理；其他级别按 `addr_segm` / 标准地址事实表处理；层级定义以 `segm_addr_type.level_id` 为准，且 `addr_segm` 侧层级条件应用于 `addr_segm.segm_type`。
- 查询、保存、校验统一以 `segm_addr_type.level_id` 为准，`0323` 需求书当前列出 `19` 类地址类型序列。
- 需求书中的城区/非城区 仅仅作为标准地址的一个属性，与地址层级并无关系，数据存储可以复用`addr_segm.is_city`; `area_type`表示 `城乡属性`，枚举来自 `pub_restriction.keyword='AREA_TYPE'`。
- “楼栋级”属于业务语义，不再与固定数字层级绑定；当前需求书列出的 `19` 类序列中，`11` 为建筑、`13` 为建筑单元、`16` 为房间、`19` 为尾级地址（选址生成），实现前仍需以联调库字典核验为准。
- 一二级标准地址属于外部基础数据，默认只读，不允许界面新增、修改、删除、合并、拆分。
- 标准地址名称拼装、层级校验、级联刷新。
- 标准地址修改后需级联刷新地址拼装名称、地址拼装简拼、下级地址及关联安装地址；地址拼装简拼按“中文转拼音首字母大写、中文括号转英文括号、数字不变”规则生成。
- 批量新增下级地址场景复用名称模糊查询接口选择父级地址，前端单次候选限制 `200` 条。
- 删除约束校验：先校验下级地址和关联资源（至少含网格、客户），再判断安装地址并触发二次确认。
- 删除实现需兼容逻辑删除与物理删除，并保留审计日志。
- 导出需支持“导出本页数据”“导出选中数据”“按用户选择字段导出”三种模式。
- 合并规则：目标地址与源地址的等级比较必须按真实 `segm_addr_type.level_id` 执行；目标地址层级数值必须小于源地址；同级地址禁止合并；低等级地址不能作为目标地址合并高等级地址；若合并后全局拼装地址重名需给出提示。
- 拆分规则：源地址复制出多个同级地址，属性默认继承不变，源地址默认回收删除；额外约束待实现细则冻结后补齐。
- 导入记录需按“单条导入数据”建模，不再只保留批次级记录。
- 非标地址监控至少覆盖 `格式规范性检测`、`行政区划合规性检测`、`地址要素完整性检测` 三类规则；格式规则以 `segm_addr_type` 为准，前三级行政区划以前三级区域主数据为准，`1-5` 级必填，`7/9/11/16` 级需满足向上包含规则。
- 安装地址与标准地址是独立实体，但 `0323` 需求书明确安装地址默认应关联标准地址；查询仍需支持按“是否关联标准地址”分组查看历史治理数据，并保留标准地址与安装地址双向同步能力。
- 标准地址所属管理站限三类，建议按关系表建模；主数据参考 `spc_station` 按 `region_id` 维护，类型字典来自 `pub_restriction.keyword='MANAGE_TYPE'`（`2017101=维修`、`2017102=安装`、`2017103=营业`）；源表当前不区分类型，需要在关系层补齐 `维修/安装/营业` 三种业务角色；地址属性建议扩展表建模，接入能力优先按多值结构设计。
- 网格历史迁移与双向增量同步能力需在实现层补齐，但接口语义已在清单中预留。
- 网格组织查询仅返回当前租户 / 权限可见范围；删除组织节点前需先校验无子节点。
- 网格仅允许在广电站节点下新增；删除网格时需同时校验“网格地址划分衍生客户”和“手工网格客户划分”两类客户关联来源。
- 网格地址划分按楼栋级标准地址一对一归属网格，楼栋下客户自动归属；其中“楼栋级”需通过 `segm_addr_type` / 类型映射判断，不允许实现中硬编码固定数字层级；客户手工绑定冲突时，以客户绑定为准。
- 网格客户列表默认仅展示手工划分数据，底层存储必须显式区分“地址划分 / 手工划分 / BOSS 调整”等来源。
- 导入类接口需保留全量导入与失败明细导出能力；当前阶段不提供部分成功数据回滚，只要求记录当前批次失败数据并支持导出。
- 旧系统兼容接口需保留 legacy 标识字段与编码口径，至少包括 `segmId/resObjectId/setAddrId/regionId/companyId/stationId/segmType`。
- 旧系统地址能力同时存在 `URL 参数`、`JSON`、`XML`、`SOAP` 四种协议形态，实现层需增加协议适配与原始报文留痕能力。
- 旧系统关于接入能力的字段口径比当前接口册更细，至少需兼容 `FTTH_PON_TYPE`、`ADDR_IN_TYPE_FTTH`、`ADDR_IN_TYPE_LAN` 及其字典 ID。

## 外部系统对接清单
- BOSS：URL 选址入口、旧地址树查询、旧地址模糊查询、旧标准地址创建、SOAP 标准地址变更通知、XML 地址同步 / 地址查询、关联设备信息、选址平台复用接口、网格客户划分接口。
- 工单系统：标准地址查询接口（先查小区、再返回楼栋供勾选）、异常地址修正工单创建与状态回写、网格关系数据同步。
- 企业微信：选址平台复用接口、告警与通知推送（模板、频率、触发条件待明确）、网格主数据同步。
- 大数据平台：标准地址数据库查询接口、标准地址主数据同步、网格 / 客户 / 经理数据同步（全量/增量策略待明确）。
- 地图系统：选址平台所需地理编码、地图圈选与坐标服务（接口与供应方待明确）。
- 其他上游依赖：宽带质量监测平台、各地自建网管系统、客服系统、设备原厂网管；当前至少在接口和数据模型上预留地址 / 网格 / 选址查询复用能力。

## 旧系统兼容接口清单
- 页面 URL 选址入口兼容：
- 保留 `checkAddressForBOSS` 页面入口，兼容 `systemsource/uuId/areaCode/cipherIndex/cipherType/cipherText/staffNbr` 参数与 `invokeTime` 时效校验。
- 旧地址树查询兼容：
- `GET /addr/compare.spr?method=getChildAddr&resObjectId=...`，返回 XML 树节点，节点需包含 legacy `id/text/child/segmType`。
- 旧地址模糊查询兼容：
- `GET /ibuz/buzInterface.spr?method=getAddress&type=1&limit=20&keyword=...`，返回 JSON 明文，其中 `data` 为字符串化结果集，结果项需兼容 `segmId/fullNameList/fullName/segmType/regionId/resourceSegment`。
- 旧标准地址创建兼容：
- `POST /ibuz/buzInterface.spr?method=saveAddressNew`，按楼栋地址下新增单元/楼层/房间并返回 `segm_id/set_id/stand_name/set_name/station_id` 等 legacy 字段。
- 旧选址结果对象兼容：
- 页面选址确认后需能组装 `obj.uuId/obj.segm_id/obj.stand_name/obj.set_id/obj.set_name` 及接入能力相关字段给 BOSS 前台。
- BOSS 标准地址变更通知兼容：
- 需支持向 BOSS 发送 SOAP 报文，`requestSystemNo=4/requestNo=4`，报文体携带安装地址与接入能力字段。
- BOSS 地址同步接口兼容：
- 需支持接收或响应 XML 格式的地址同步请求，兼容 `ADD/UPDATE/QUERY` 动作以及标准地址/安装地址两类对象。
- BOSS 地址查询接口兼容：
- 需支持按 `SEGM_ID + SEGM_TYPE` 单条查询地址，并返回标准地址全称、覆盖户数、管理站、接入方式与安装地址信息。
- 旧地址查询 URL 页面兼容：
- 保留 `query.jsp` 形式的页面查询入口，用于旧系统跳转调用。
- 引用型外部接口：
- `单客户信息同步/批量客户信息同步` 来自旧文档，但当前更像客户中心或 BOSS 侧能力，暂在本清单中保留参数口径，不默认纳入 `ruoyi-address` 内部 Controller 基线。

## 旧系统兼容参数速查

| 接口能力 | 协议/格式 | 关键请求参数 | 关键响应参数 | 备注 |
| --- | --- | --- | --- | --- |
| URL 选址入口 | URL Query | `systemsource` `method=checkAddressForBOSS` `uuId` `areaCode` `cipherIndex` `cipherType` `cipherText` `staffNbr` | 页面跳转成功或失败页 | 需校验解密后的 `invokeTime` 与服务端时间差 |
| 地址树查询 | `GET + XML` | `method=getChildAddr` `resObjectId` | `tree/item@id/text/child` `userdata[segmType]` | 旧接口主键使用字符串型 legacy ID |
| 地址名称模糊查询 | `GET + JSON` | `method=getAddress` `type=1` `limit` `keyword` | `sessionId` `data[]` 中的 `segmId/fullNameList/fullName/segmType/regionId/resourceSegment` | `data` 为字符串化 JSON 数组 |
| 标准地址创建 | `POST form + JSON` | `uuId` `areaCode` `staffNbr` `org_id` `fullname` `addressId` `adId` `adName` `returnValue` `objStr` | `companyId` `uuId` `segm_id` `stand_name` `set_id` `set_name` `station_id` `addr_in_type` | `objStr` 内嵌 `parentTypeId/parentAddressName/unitName/floorName/roomName` |
| 选址结果对象 | 页面对象 | `uuId` `segm_id` `stand_name` `set_id` `set_name` | `FTTH_PON_TYPE` `FTTH_PON_TYPE_ID` `ADDR_IN_TYPE_FTTH` `ADDR_IN_TYPE_FTTH_ID` `ADDR_IN_TYPE_LAN` `ADDR_IN_TYPE_LAN_ID` | 需与地址属性和安装地址数据联动组装 |
| 标准地址变更通知 | `SOAP + XML` | `requestSystemNo=4` `requestNo=4` `custId` `setAddrId` `setAddrName` 以及三组接入方式/能力字段 | `return-code` `return-message` | 属于我方向 BOSS 的主动通知能力 |
| 地址同步接口 | `XML` | `DOMAIN_SEND` `DOMAIN_RECEIVE` `REGIONCODE` `OPERATION_NAME` `BOSS_OP` `SEGM_ID` `SEGM_TYPE` | `SEGM_ID` `SEGM_NAME` `COVERNUM` `STATION_ID` `INTYPE` `STAND_NAME` `SET_ADDR` `RETURN_TYPE` `REASON` | 需兼容标准地址和安装地址两类对象 |
| 地址查询接口 | `XML` | `DOMAIN_SEND` `DOMAIN_RECEIVE` `REGIONCODE` `OPERATION_NAME=QUERY` `SEGM_ID` `SEGM_TYPE` | 同地址同步接口响应字段 | 文档明确“查询不应变更同步标识” |
| 地址查询 URL 页面 | 页面 URL | 固定 query 参数 | 页面返回 | 更偏旧前台兼容入口 |
| 单客户/批量客户信息同步 | `SOAP/XML 待实现侧确认` | `orgCode` `searchType` `key/value` 或 `rownum` `channelId` | `CustomerInfo/SubsInfo` | 当前先作为外部依赖参数保留，归属待确认 |

## 兼容性预案
- 新旧系统并行与否未定：接口需保留全量导入与增量同步能力。
- 网格历史数据迁移需与双向增量同步方案配套设计。
- 当前阶段先固化接口合同，具体实现可以按模块分阶段落地。

## 标后测试对实现的直接约束

- 中标公告结束后 `5` 个工作日内需完成测试环境搭建和应用部署。
- 正式测试时间 `3` 小时。
- 全部 `33` 个测试项中，带 `★` 的测试项必须全部通过，总通过数不得少于 `30` 项。
- `ruoyi-address` 直接相关的重点测试项包括：
- `2.8.4.12` 到 `2.8.4.19`：标准地址新增、修改、查询、导出、删除、合并、拆分。
- `2.8.4.20` 到 `2.8.4.30`：单条查看、标签、导入、日志、非标监控、选址、管理站、属性、安装地址查询、地址删除、安装地址修改。
- `2.8.4.31`：系统接口，需要能配置并查看工单系统、BOSS、企业微信、大数据平台相关接口数据。

## 待需求规则澄清事项
- 各外部系统的接口地址、鉴权方式、字段映射、回调机制。
- `单客户信息同步/批量客户信息同步` 是否由本模块承接，还是由客户中心 / BOSS 适配层承接。
- legacy `地址同步接口 / 地址查询接口` 的最终交互方向和动作边界，是否需要双向兼容。
- legacy `segmId/resObjectId/setAddrId/regionId/companyId/stationId/segmType` 的生成规则、迁移映射规则和后续维护责任方。
- URL 选址入口的密钥组维护方式、时效校验窗口、密钥轮换策略。
- `fullNameList/resourceSegment/data` 等 legacy 展示字段是否要求逐字符兼容旧系统。
- 监控规则完整清单、优先级和触发阈值。
- 合并、拆分、删除、工单修正的细则。
- 网格覆盖策略、客户自动归属策略和双向同步边界。
