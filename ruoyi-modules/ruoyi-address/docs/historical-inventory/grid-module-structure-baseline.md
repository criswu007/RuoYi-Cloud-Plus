# 网格模块历史结构开发基线

> 本文档用于把网格模块的历史样例、装载 SQL 和字段注释整理成可直接进入开发讨论的结构基线。原始过程稿已归档到 `/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/archive/address-docs/history-inventory/grid-module`。

## 1. 结论先行

- 当前在线库 `ftth_cloud_address` 中未发现正式网格业务表。
- 因此，网格模块开发不能以“先设计一套全新目标表”为起点，而应先以历史样例和装载 SQL 反推可落地的结构基线。
- 当前首版可识别的历史主表包括：`grid_TOJF`、`grid_manager_tojf`、`ORGANIZATION_TOJF`、`GRID_ADDR_REL_TOJF`、`CUST_GRID_REL_TOJF_TD`、`cust_TOJF`、`cust_TOJF_TD`。
- `0323` 需求书已经把网格组织、网格、网格地址、客户划分、经理和外围同步规则补得比较完整，后续实现必须把这些需求规则和历史样例一起看，不能只看历史表长什么样。

## 2. 资料来源

- 根目录原始资料：`/Users/criswu/Desktop/广电/历史数据/网格模块`
- 历史装载 SQL：`DDL/建表sql`
- 字段中文注释：`DDL/字段中文注释`
- 数据样例：`数据样例`
- 本仓库归档过程稿：
  - `tmp/archive/address-docs/history-inventory/grid-module/source_inventory.md`
  - `tmp/archive/address-docs/history-inventory/grid-module/grid_module_schema_report.md`
  - `tmp/archive/address-docs/history-inventory/grid-module/grid_module_import_guide.md`

## 3. 核心历史表

| 历史表 | 角色 | 建议主键 | 说明 |
| --- | --- | --- | --- |
| `grid_TOJF` | 网格主表 | `GRID_ID` | 保存网格名称、编码、类型、公司、区域、部门、团队、经理等主数据。 |
| `grid_manager_tojf` | 网格经理表 | `PARTY_ROLE_ID` | 保存经理名称、手机号、区域归属、角色、状态。 |
| `ORGANIZATION_TOJF` | 网格组织表 | `PARTY_ID` | 保存组织树、层级、路径、类型。 |
| `GRID_ADDR_REL_TOJF` | 网格地址关系表 | 待确认 | 保存标准地址 `SEGM_ID` 与网格 `GRID_ID` 的关联。 |
| `CUST_GRID_REL_TOJF_TD` | 客户网格关系表 | 待确认 | 保存客户、网格、经理、部门、区域等归属关系。 |
| `cust_TOJF` | 客户基础表 | `CUST_ID` | 保存客户基础信息与网格挂接字段。 |
| `cust_TOJF_TD` | 客户基础表 TD 版本 | `cust_id` | 与 `cust_TOJF` 基本同义，但字段命名存在大小写差异。 |

## 4. 当前可直接使用的结构认识

### 4.1 网格主数据

- `grid_TOJF` 已能支撑网格的主键、名称、编码、类型、状态、组织归属和经理挂接。
- 可直接识别的组织维度包括：`BUSINESS_ID/BUSINESS_NAME`、`REGION_ID/REGION_NAME`、`DEPT_ID/DEPT_NAME`、`TEAM_ID/TEAM_NAME`。
- 这说明网格不是孤立对象，而是天然绑定组织树和经理关系。

### 4.2 网格组织

- `ORGANIZATION_TOJF` 已具备 `PARENT_PARTY_ID`、`PATH_CODE`、`ORG_LEVEL`、`ORG_TYPE` 等字段。
- 后续如果实现组织树、可见范围、父子节点删除校验，应优先对齐这套历史组织结构。

### 4.3 网格地址关系

- `GRID_ADDR_REL_TOJF` 至少确认了 `SEGM_ID`、`STAND_NAME`、`GRID_ID` 的关系方向。
- 这与当前接口约束“楼栋级标准地址归属网格”相吻合，因此后续实现应优先围绕“历史标准地址 ID 与网格 ID 关系”建模；但“楼栋级”必须通过 `segm_addr_type` / 地址类型映射解析，不能在代码里写死为固定层级数字。

### 4.4 网格客户与经理

- `CUST_GRID_REL_TOJF_TD` 已暴露客户、部门、区域、团队、网格、经理的挂接字段。
- `grid_manager_tojf` 已暴露经理主键、手机号、区域和状态。
- 这意味着“网格客户归属”和“网格经理管理关系”在历史结构上就是两层关系，不适合粗暴合并成单表。

## 5. 开发使用原则

- 网格模块开发优先沿用历史表名、字段名、关系方向和主键候选，不先发明一套脱离历史链路的新对象模型。
- 如果为了适配 `ruoyi-address` 需要新建业务表或扩展表，也应围绕这些历史主表做映射、同步和治理，而不是替换它们的主语义。
- `cust_TOJF` 与 `cust_TOJF_TD` 的大小写和装载差异需要在实施阶段统一策略，但在统一前不得随意删去其一。
- `GRID_ADDR_REL_TOJF`、`CUST_GRID_REL_TOJF_TD` 当前主键待确认，因此实现前仍需补一轮唯一性和导入规则核实。

## 6. 需求书补充的网格实现约束

- 网格模块需支持历史数据迁移和双向增量同步，不能只做静态主数据维护。
- 网格组织节点类型至少包含：根目录、地市、分公司、广电站。
- 网格组织查询只返回当前租户可见组织；删除组织前若存在子节点必须拦截；节点启停不影响子节点状态。
- 网格仅允许在 `广电站` 节点下新增。
- 删除网格前，必须同时校验两类客户来源：`网格地址划分衍生客户` 与 `网格客户手工划分客户`。
- 网格地址划分的核心关系是“楼栋级标准地址 -> 单一网格”，单条楼栋级标准地址只能归属一个网格；唯一性校验前必须先把“楼栋级”业务语义映射到真实地址类型。
- 网格地址划分后，需按“标准地址 -> 安装地址 -> 客户”链路去重生成客户与网格的关联。
- 手工网格客户划分优先级高于地址划分；一旦发生手工划分，后续地址划分不再覆盖，但仍允许再次手工调整。
- 网格客户查询接口默认只展示手工划分数据，因此存储层必须显式区分数据来源。
- 网格与客户、网格信息、网格经理信息都需要同步给大数据平台、BOSS、工单系统、企业微信。

## 7. 待补事项

- 确认各历史网格表是否继续落在 `ftth_cloud_address`，还是需要切换到新库。
- 确认 `GRID_ADDR_REL_TOJF`、`CUST_GRID_REL_TOJF_TD` 的唯一键、增量策略和回滚策略。
- 确认 `cust_TOJF` 与 `cust_TOJF_TD` 的并存关系，是双来源、双环境，还是同源不同装载口径。
- 确认字段注释中尚未解释清楚的类型字段，如 `GRID_TYPE`、`GRID_COVER_TYPE`、`GRID_CUS_TYPE`、`STATE`。
