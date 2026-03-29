# 南京标准地址核心正式盘点报告

> 报告定位：本报告为“历史库持续盘点-标准地址核心”领域包在南京范围的首版正式盘点结论，结论仅基于已采集的证据材料，不对未采集字段与未明确业务规则部分作推断。
>
> 事实依据：
> - 证据材料：[nanjing-standard-address-core-evidence.md](./evidence/nanjing-standard-address-core-evidence.md)
> - 任务体系设计：[2026-03-24-historical-schema-inventory-design.md](/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/archive/address-working-notes/superpowers/specs/2026-03-24-historical-schema-inventory-design.md)
>
> 盘点批次标识：以表名后缀 `_nj_20260317` 为本批次历史基线标识。

## 1. 盘点范围

- 范围城市：南京
- 数据库（catalog）：`ftth_cloud_address`（见证据材料 CONNECTED 段落）
- 本批次纳入的 8 张表（按领域包归位为“标准地址核心”）：
  - `tmp_addr_segm_nj_20260317`
  - `tmp_addr_set_segm_nj_20260317`
  - `spc_station`
  - `spc_region`
  - `segm_addr_type`
  - `pub_restriction`
  - `spc_regional_company`
  - `staff`

## 2. 表清单与角色定位

说明：角色定位遵循《历史库持续盘点任务体系设计》对“标准地址核心”的归位定义；其中 `tmp_addr_set_segm_nj_20260317` 的归位有明确的临时性说明（见本节备注与第 3 节结论）。

| 表名 | 角色定位 | 说明 | 行数（证据） |
|---|---|---|---:|
| `tmp_addr_segm_nj_20260317` | 主事实表 | 南京标准地址历史主表，承载标准地址对象与父子层级 | 364000 |
| `tmp_addr_set_segm_nj_20260317` | 关联事实表 | 安装地址与标准地址挂接关系的事实表，本批次为“主链路盘点需要”暂归入标准地址核心 | 1566000 |
| `spc_station` | 主数据表 | 管理站主数据，本批次已补充字段注释与结构索引，可参与关联一致性检查与主数据结构盘点 | 1229 |
| `spc_region` | 主数据表 | 区域主数据，本批次已补充字段注释与结构索引，可参与关联一致性检查与主数据结构盘点 | 102 |
| `segm_addr_type` | 辅助表（类型解释表） | 地址类型与层级规则解释（字典/解释性质） | 18 |
| `pub_restriction` | 辅助表（字典/限制表） | 历史字典与限制项解释 | 6361 |
| `spc_regional_company` | 辅助表（组织映射表） | 区域公司与权限口径辅助（组织映射） | 79 |
| `staff` | 辅助表（人员辅助表） | 人员辅助表，本批次仅确认已纳入盘点，字段级用途待补证据 | 891 |

备注（必须声明）：
- `tmp_addr_set_segm_nj_20260317` 当前归入“标准地址核心”仅为主链路盘点需要：本次盘点必须覆盖“安装地址到标准地址”的挂接现状，以支撑标准地址主链路的事实闭环；其更完整的关系、资源侧含义与后续接口输入，仍应在后续“安装地址与资源关系”领域包展开时再做完整盘点与归档。

## 3. 标准地址主链路事实结论

### 3.1 主事实表：`tmp_addr_segm_nj_20260317`

- 主对象标识字段（已采集）：
  - `id`：`bigint(20) unsigned`，非空（证据字段清单）
  - `segm_id`：`varchar(1024)`，可空（证据字段清单）
- 父子层级关系（必须覆盖）：
  - 自关联字段：`parent_segm_id`：`varchar(64)`，可空（证据字段清单）
  - 证据显示存在 `parent_segm_id` 为空的记录：`missing=74661`（NULL_CHECKS 输出）
  - 结论：`tmp_addr_segm_nj_20260317` 具备“父子层级”建模字段，但 `parent_segm_id` 的空值需要区分“根节点”与“缺失父节点”两类语义；目前证据仅能确认空值规模，无法仅据此判定为数据缺失或合理根节点。
- 与区域、站点的挂接（必须覆盖）：
  - `region_id` 存在于字段清单中；关联缺失检查 `tmp_addr_segm_missing_region_link_count` 输出为空（RELATION_CHECKS 段落）。
    - 结论：在本次证据采集口径下，未检出“标准地址缺失区域挂接”的异常记录（输出为空）；但由于证据未展示具体 SQL 与计数格式，暂不将其等价为绝对 0，需要在后续补证据时以统一计数格式再确认。
  - `station_id` 存在于字段清单中；关联缺失检查显示 `missing=1`（RELATION_CHECKS 段落）。
    - 结论：存在 1 条标准地址记录缺失站点挂接（或站点关联无法命中），需作为数据质量问题纳入修正/兼容策略。
  - `installstation_id`、`busstation_id` 的关联缺失检查均为 `missing=0`（RELATION_CHECKS 段落）。

### 3.2 关联事实表：`tmp_addr_set_segm_nj_20260317` 到 `tmp_addr_segm_nj_20260317`

- 关键字段（已采集）：
  - `set_addr_id`：`varchar(255)`，非空（证据字段清单）
  - `segm_id`：`varchar(255)`，可空（证据字段清单）
- 挂接结论（必须覆盖）：
  - 证据存在“安装地址挂接标准地址缺失”的统计：`missing=1290542`（RELATION_CHECKS:tmp_addr_set_segm_missing_standard_count）。
  - 结论：在本批次数据中，`tmp_addr_set_segm_nj_20260317` 到 `tmp_addr_segm_nj_20260317` 的挂接存在大规模未命中（约 129 万级）。该结论仅基于缺失统计本身，无法进一步判定未命中原因（例如：`segm_id` 为空、编码体系不一致、标准地址不在本批次表中、或挂接规则非按 `segm_id`），需要后续补充“缺失原因拆分”证据。
- `set_type` 分布异常（证据原样）：
  - `set_type=NULL, total=1562375`
  - `set_type=0, total=3625`
  - 结论：`set_type` 在绝大多数记录为空，属于本批次必须保留的异常/待确认项（见第 9 节）。

## 4. 各表结构与字段口径摘要

说明：本节仅对“已在证据材料中出现的字段/分布/约束”做摘要；对未采集到字段清单的表，仅给出可核验的信息与待补证据项，不做编造。

### 4.1 `tmp_addr_segm_nj_20260317`（主事实表）

- 行数：364000
- 已采集字段（节选，来自证据字段清单）：
  - 标识/层级：`id`（非空）、`segm_id`、`parent_segm_id`
  - 类型/名称：`segm_type`、`segm_name`、`segm_no`、`stand_name`、`stand_no`、`addr_type`
  - 归属：`region_id`、`station_id`、`installstation_id`、`busstation_id`
  - 状态：`status`、`delete_state`、`delete_time`
  - 坐标：`x`、`y`
  - 其他：存在大量辅助字段（如同步/创建/修改信息、网络覆盖与能力字段等），本报告不逐一解释其业务语义，后续需要按接口设计输入再分层梳理（见第 8 节）。
- `segm_type` 分布（证据输出）：
  - 主要集中在 `180007`（336020）与 `180013`（20379），其余类型数量显著更少。
  - 结论：`segm_type` 的枚举含义目前未在证据中给出，需要结合 `segm_addr_type` 或业务规则补充解释，否则无法直接用于接口层的可读展示与筛选。
- 重复性证据：
  - `segm_id` 重复检查 top20 为空（DUPLICATE_CHECKS 输出为 `[EMPTY RESULT]`）。
  - 结论：在本次证据采集口径下未检出 `segm_id` 的重复样本；但仍需明确“重复检查的过滤条件/范围”后才能作为强结论固化。

### 4.2 `tmp_addr_set_segm_nj_20260317`（关联事实表）

- 行数：1566000
- 已采集字段（节选，来自证据字段清单）：
  - 标识：`id`（非空）、`set_addr_id`（非空）
  - 名称/编码：`set_addr_name`、`set_addr_no`
  - 挂接：`segm_id`、`segm_type`、`region_id`
  - 状态与同步：`status`、`synchronous_date`、`delete_state`、`delete_time`
  - 组织/区域：`area_id`、`org_id`
- `set_type` 分布异常：见第 3.2 节与第 9 节。
- 重复性证据：
  - `set_addr_id` 重复检查 top20 为空（DUPLICATE_CHECKS 输出为 `[EMPTY RESULT]`）。
  - 结论：在本次证据采集口径下未检出 `set_addr_id` 的重复样本；同样需要后续补充“检查条件”以固化强结论。

### 4.3 `spc_station`（主数据表）

- 行数：1229
- 字段口径：本轮已补充字段级注释，可识别 `station_id`、`station_no`、`china_name`、`region_id`、`manage_type`、`mnt_region_id` 等字段的中文语义；详见同目录结构手册。
- 关联位置：标准地址主事实表存在 `station_id`、`installstation_id`、`busstation_id` 等站点相关字段，并已完成缺失统计（见第 3.1 节）。
- 当前边界：虽然字段注释已完整，但站点层级、组织归属、管理角色等业务口径仍不能仅凭本表单独冻结。

### 4.4 `spc_region`（主数据表）

- 行数：102
- 字段口径：本轮已补充字段级注释，可识别 `region_id`、`region_no`、`region_name`、`grade_id`、`type_id`、`res_type_id`、`parent_id` 等字段的中文语义；详见同目录结构手册。
- 关联位置：标准地址主事实表存在 `region_id` 字段，并已完成缺失检查输出为空（见第 3.1 节）。
- 当前边界：区域类型、资源类型等编码字段仍需结合字典或历史规则确认，不能仅凭字段注释直接冻结业务规则。

### 4.5 `segm_addr_type`（辅助表：类型解释表）

- 行数：18
- 证据分布：存在 `level_id` 的分布输出（DICTIONARY_CHECKS），显示 `level_id` 取值较为离散（如 1、5、8、10、15、20、30、35、50、60、65、70、80、90、95、99、100 等）。
- 结论：`segm_addr_type` 具备解释“层级/类型”的潜在字典能力，但证据未提供其与 `tmp_addr_segm_nj_20260317.addr_type`、`segm_type` 的映射关系与字段名，暂不对映射口径下结论（见第 9 节）。

### 4.6 `pub_restriction`（辅助表：字典/限制表）

- 行数：6361
- 证据分布：`code` 分布中存在大量 `NULL`（4977），并出现 `UNKNOW`、`O`、以及数字与形如 `35600108` 的值。
- 结论：该表可能承担“限制/字典解释”能力，但目前仅能确认 `code` 值域复杂且存在大量空值；是否可直接作为接口枚举或校验规则，需要补充字段说明与关联使用点。

### 4.7 `spc_regional_company`（辅助表：组织映射表）

- 行数：79
- 证据分布：`segm_type_priv=180013, total=79`（DICTIONARY_CHECKS）。
- 结论：在本批次证据下，`spc_regional_company` 与 `segm_type_priv=180013` 的权限/类型口径强相关，但其字段含义、与 region/station 的连接键、以及“权限”具体语义仍待补证据（见第 9 节）。

### 4.8 `staff`（辅助表：人员辅助表）

- 行数：891
- 字段口径：证据材料仅提供行数，未提供字段清单/主键/工号口径。本报告不对字段作结论。
- 关联位置：`tmp_addr_segm_nj_20260317` 已采集到 `modify_op`、`create_op`；`tmp_addr_set_segm_nj_20260317` 已采集到 `boss_op`、`modify_op`。人员字段与 `staff` 的匹配规则与缺失率尚未采集证据。

## 5. 数据质量结论

本节结论均来自证据材料的“空值/重复/分布/关联缺失”输出，不包含推断。

- `tmp_addr_segm_nj_20260317`：
  - `parent_segm_id` 存在 74661 条空值（NULL_CHECKS）。
  - `segm_id` 重复检查 top20 为空（DUPLICATE_CHECKS），本次未检出重复样本。
  - 站点挂接缺失：`station_id` 缺失统计 `missing=1`；`installstation_id`、`busstation_id` 缺失均为 0（RELATION_CHECKS）。
- `tmp_addr_set_segm_nj_20260317`：
  - `set_type` 极高比例为空（分布见第 3.2 节），属于明显异常信号。
  - 安装地址挂接标准地址缺失规模大：`missing=1290542`（RELATION_CHECKS）。
  - `set_addr_id` 重复检查 top20 为空（DUPLICATE_CHECKS），本次未检出重复样本。
- 字典/辅助表：
  - `pub_restriction.code` 存在大量空值（4977），且值域混杂（DICTIONARY_CHECKS），使用时需谨慎对待“是否可作为强约束”。

## 6. 跨表一致性结论

本节聚焦“标准地址主链路”必须覆盖的 4 条链路，并明确可落地的证据结论与不确定项。

- 链路 1：`tmp_addr_segm_nj_20260317` 自关联父子关系
  - 证据：`parent_segm_id` 字段存在，且存在 74661 条空值（NULL_CHECKS）。
  - 一致性结论：当前只能确认层级字段存在与空值规模，无法仅据此判定父子树是否完整、是否存在“parent_segm_id 非空但父节点不存在”的断链问题（需补充断链统计证据）。
- 链路 2：`tmp_addr_set_segm_nj_20260317` -> `tmp_addr_segm_nj_20260317`
  - 证据：存在“missing_standard_count=1290542”（RELATION_CHECKS）。
  - 一致性结论：该挂接在本批次存在大规模未命中，是主链路一致性最大风险点；需补充按原因拆分（`segm_id` 为空 vs 编码不一致 vs 标准地址不在本批次等），否则接口设计阶段难以确定“默认挂接策略/兜底策略”。
- 链路 3：`tmp_addr_segm_nj_20260317` -> `spc_region`
  - 证据：缺失检查输出为空（RELATION_CHECKS:tmp_addr_segm_missing_region_link_count）。
  - 一致性结论：在本次证据口径下未检出缺失样本，但仍需后续以统一格式输出缺失计数，才能将其固化为“缺失为 0”的强结论。
- 链路 4：`tmp_addr_segm_nj_20260317` -> `spc_station`
  - 证据：`station_id` 缺失统计 `missing=1`；`installstation_id`、`busstation_id` 缺失为 0（RELATION_CHECKS）。
  - 一致性结论：主站点挂接存在极少量缺失（1 条），需要在接口层提供可解释的容错展示与治理入口；安装站点/业务站点在本次证据下未检出缺失。

## 7. 风险清单

风险类型必须固定为：结构风险、质量风险、映射风险、能力缺口。以下风险均以证据材料中的事实信号为触发点，不包含臆测结论。

### 7.1 结构风险

- `tmp_addr_segm_nj_20260317.parent_segm_id` 为 `varchar(64)`，而 `segm_id` 为 `varchar(1024)`：若父子关系以 `segm_id` 作为连接键，字段长度差异可能带来截断/不可逆映射风险。
- `tmp_addr_set_segm_nj_20260317.segm_id` 为 `varchar(255)`，而 `tmp_addr_segm_nj_20260317.segm_id` 为 `varchar(1024)`：同样存在长度不一致导致的潜在映射风险（需后续验证是否存在超长编码）。

### 7.2 质量风险

- 标准地址父子层级存在大规模 `parent_segm_id` 空值（74661）：若其中包含“应有父节点但缺失”的情况，将影响树查询、路径拼接、归属继承等能力。
- 标准地址站点挂接存在缺失样本（1）：虽规模小，但可能引发接口侧不可解释的“无站点归属”记录。
- 安装地址挂接标准地址未命中规模大（1290542）：将直接影响“从安装地址反查标准地址/从标准地址汇总安装地址”等接口能力的正确性与可用性。
- `tmp_addr_set_segm_nj_20260317.set_type` 绝大多数为空：可能影响安装地址类型/来源/生成规则的判定与筛选。
- `pub_restriction.code` 大量为空且值域混杂：若被用于强校验或展示枚举，可能造成不可解释与错误分类。

### 7.3 映射风险

- `segm_type` 主要值域集中于 `180007` 与 `180013`，但缺少枚举语义：接口设计时若直接暴露该值，将导致前端/调用方无法理解，且后续兼容成本高。
- `segm_addr_type` 的 `level_id` 分布存在，但缺少与 `tmp_addr_segm.addr_type`/`segm_type` 的明确映射口径：会影响“按层级查询/按类型筛选/树层级渲染”等接口的一致性。
- `tmp_addr_set_segm` 当前为主链路盘点临时归入：其真正的业务域边界与与资源侧关系未盘点，若在标准地址核心阶段过早固化其接口输入，可能导致后续返工。

### 7.4 能力缺口

- 缺少“父子断链（parent 非空但父节点不存在）”的统计证据，无法确认树结构是否可直接用于树查询接口。
- 缺少“安装地址挂接未命中原因拆分”的证据，无法定义标准的兜底策略（例如：按 region 推断、按 set_addr_name 模糊匹配等均不可在无证据前提下确定）。
- `staff` 的字段注释与人员/权限规则证据仍然不足；`spc_station`、`spc_region` 虽已补充字段注释，但其业务规则与连接口径仍需继续补证据，接口字段清单暂不能直接冻结全部衍生语义。

## 8. 对接口设计的直接输入

说明：本节只提炼“可直接用于接口设计的输入/输出字段候选与必须的治理点”。对未采集到字段清单的表，只能给出接口层依赖方向，不下字段级结论。

### 8.1 标准地址（基于 `tmp_addr_segm_nj_20260317`）

- 列表/详情输出字段候选（已采集字段）：
  - 标识：`id`、`segm_id`
  - 展示：`segm_name`、`segm_no`、`stand_name`、`stand_no`、`alias`
  - 归属：`region_id`、`station_id`、`installstation_id`、`busstation_id`、`service_region_id`
  - 层级：`parent_segm_id`
  - 类型/状态：`segm_type`、`addr_type`、`status`、`delete_state`
  - 坐标：`x`、`y`
- 查询条件候选（已采集字段）：
  - 精确：`segm_id`、`region_id`、`station_id`、`segm_type`、`addr_type`、`status`
  - 模糊：`segm_name`、`stand_name`、`segm_no`、`stand_no`
  - 树查询：`parent_segm_id`（需要先补齐“断链/根节点”规则）
- 接口必须考虑的容错/治理点（基于证据的直接输入）：
  - `station_id` 存在缺失样本（1）：详情接口需明确返回策略（返回空、返回默认、或附带异常标识）。
  - `parent_segm_id` 大规模为空：树接口需定义根节点判定规则（仅基于证据暂不能下结论）。

### 8.2 安装地址到标准地址挂接（基于 `tmp_addr_set_segm_nj_20260317`）

- 边界说明：本节仅作为“标准地址主链路闭环”所需的过渡性接口输入，供当前批次引用；其更完整的安装地址对象定义、资源关系口径与独立接口输入，需在后续“安装地址与资源关系”领域包中单独补盘并归档。

- 列表/详情输出字段候选（已采集字段）：
  - 安装地址标识：`id`、`set_addr_id`
  - 安装地址展示：`set_addr_name`、`set_addr_no`、`alias`
  - 挂接字段：`segm_id`、`region_id`、`segm_type`
  - 状态/同步：`status`、`synchronous_date`、`delete_state`
  - 组织/区域：`area_id`、`org_id`
- 查询条件候选（已采集字段）：
  - 精确：`set_addr_id`、`segm_id`、`region_id`、`status`
  - 模糊：`set_addr_name`
- 接口必须考虑的容错/治理点（基于证据的直接输入）：
  - 挂接未命中规模大（1290542）：需要在接口层明确“未挂接”状态的可查询与可治理能力，而不是简单过滤。
  - `set_type` 高比例为空：在其语义未澄清前，不建议将其作为强筛选条件或强枚举展示字段。

### 8.3 主数据/字典依赖（`spc_station`、`spc_region`、`segm_addr_type`、`pub_restriction`、`spc_regional_company`、`staff`）

- `spc_station`、`spc_region`：本轮已补充字段注释，可作为主数据结构索引与字段语义参考；但具体连接键约束、层级规则与映射口径仍待继续补证据。
- `segm_addr_type`：用于解释类型/层级（已知存在 `level_id` 分布证据，但映射字段与含义待补）。
- `pub_restriction`：用于解释限制/字典项（已知 `code` 值域复杂且大量空值，接口侧需谨慎使用）。
- `spc_regional_company`：用于组织/权限口径辅助（已知 `segm_type_priv=180013` 的分布证据）。
- `staff`：当前仅确认已纳入本批次盘点，且主/关联事实表存在人员相关字段；是否以及如何映射到 `staff`（匹配规则、连接键、缺失率）仍待补证据。

## 9. 暂不下结论事项

以下事项在本次证据材料中缺少足够信息或规则定义，属于“必须保留但暂不下结论”的内容：

- `tmp_addr_segm_nj_20260317.parent_segm_id` 的空值（74661）中，哪些是“合理根节点”，哪些是“缺失父节点”；以及是否存在“parent 非空但父节点不存在”的断链规模。
- `tmp_addr_set_segm_nj_20260317` -> `tmp_addr_segm_nj_20260317` 挂接未命中（1290542）的原因拆分与可治理策略；当前仅能确认未命中规模，不能推断原因。
- `segm_type`（如 `180007`、`180013` 等）与 `addr_type` 的业务语义与可读枚举说明。
- `segm_addr_type.level_id` 的业务含义，以及其与标准地址字段（`segm_type`/`addr_type`）的映射规则与主键口径。
- `pub_restriction.code` 的业务含义，及 `NULL`、`UNKNOW`、`O`、数字/长数字等混合值的统一解释与取值规范。
- `spc_station`、`spc_region`、`staff` 与主事实表的连接规则、有效性规则及业务编码口径；其中 `spc_station`、`spc_region` 已补充字段级注释，但规则级闭环仍未完成。
