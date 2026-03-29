# 线上最新非备份表结构盘点报告

## 1. 盘点范围

- 数据库 catalog：`ftth_cloud_address`。
- 盘点范围：仅统计 `table_name NOT LIKE 'bak%'` 的线上非备份表。
- 当前纳入表数：`10`。
- 大表口径：`ADDR_SEGM`、`ADDR_SET_SEGM` 的结构按全量盘点，数据分布按 1% hash sample (`crc32(主键) % 100 = 0`) 观测。

## 2. 非备份表摘要

| 表名 | 表注释 | 行数 | 字段注释覆盖度 | 可用级别 | 设计使用口径 |
|---|---|---:|---:|---|---|
| `ADDR_SEGM` | 标准分段地址 | 2318000 | 82/82 (100.00%) | `可直接入模` | 最新地址主事实表；字段注释完整，数据观测采用 1% hash sample。 |
| `ADDR_SET_SEGM` | 安装地址表 | 100000 | 20/20 (100.00%) | `可直接入模` | 安装地址主事实表；字段注释完整，数据观测采用 1% hash sample。 |
| `getpageVol` | 选址信息表 | 0 | 7/7 (100.00%) | `仅作旁证` | 当前为空表，仅能作为选址信息结构旁证。 |
| `pub_restriction` | 约束关系 | 6344 | 14/14 (100.00%) | `仅作旁证` | 字典/限制解释表；需按 keyword + serial_no 使用，不能把全表直接视为单一业务字典。 |
| `segm_addr_type` | 地址类型表 | 19 | 10/10 (100.00%) | `可直接入模` | 地址层级/类型解释表，可直接支撑 `segm_type` 候选值说明。 |
| `spc_region` | 分公司 | 103 | 28/28 (100.00%) | `仅作旁证` | 区域主数据结构索引；编码语义仍需结合规则确认。 |
| `spc_regional_company` | 组织映射表 | 80 | 7/7 (100.00%) | `可直接入模` | 组织映射结构表，可直接用于区域组织映射旁证。 |
| `spc_station` | 管理站表 | 1230 | 85/85 (100.00%) | `仅作旁证` | 管理站结构索引；站点层级、组织归属仍不能仅凭单表冻结。 |
| `staff` | 员工 | 892 | 5/48 (10.42%) | `禁止单独定口径` | 人员表注释仍稀疏，不能单表冻结人员/权限规则。 |
| `sync_set_addr_info` | 地址变更同步表 | 0 | 8/8 (100.00%) | `仅作旁证` | 当前为空表，仅能作为地址变更同步结构旁证。 |

## 3. 结构结论

- `ADDR_SEGM` 当前为线上标准地址主表，主键为 `segm_id(varchar(24))`，共有 `82` 个字段，字段注释覆盖度 `100%`。
- `ADDR_SET_SEGM` 当前为线上安装地址主表，主键为 `set_addr_id(varchar(24))`，共有 `20` 个字段，字段注释覆盖度 `100%`。
- 当前线上非备份表新增了 `getpageVol`（选址信息表）与 `sync_set_addr_info`（地址变更同步表）两个旁路结构表，但当前行数均为 `0`，只能先做结构旁证。
- `pub_restriction`、`segm_addr_type`、`spc_region`、`spc_regional_company`、`spc_station` 均已具备较完整结构注释，其中业务字典字段应优先按 `serial_no` 而非 `code` 与大表编码对齐。
- `staff` 当前仍只有 `5/48` 个字段有注释，只能作为人员结构旁证，不能直接冻结人员、岗位或权限模型。

## 4. 大表抽样结论

- `ADDR_SEGM` 实际行数约 `2318000`，样本量 `23289`，抽样比例约 `1.00%`。
- `ADDR_SET_SEGM` 实际行数约 `100000`，样本量 `1023`，抽样比例约 `1.02%`。
- `ADDR_SEGM.parent_segm_id` 在样本中的空值占比约 `0.11%`，说明当前主地址树关系整体较完整。
- `ADDR_SEGM.station_id` 在样本中的空值占比约 `7.43%`；`installstation_id` 与 `busstation_id` 在样本中均为 `100%` 空值，属于必须单独跟进的结构/数据风险。
- `ADDR_SEGM.segm_type` 抽样分布以 `180007`、`180006` 为主，`180095`（伪地址）占比也不低，后续建模时需要显式区分“房间/楼层/伪地址”等层级语义。
- `ADDR_SEGM.status` 在样本中全部为 `2140900`（有效），但 `delete_state=1` 仍约占 `5%`，说明“状态有效”与“逻辑删除”不是同一维度。
- `ADDR_SEGM.addr_type` 当前抽样主要落在 `2140500` 与 `2140001`，且仍有少量空值；该字段依赖 `addr_type / ADDR_ADDR_TYPE` 两组字典混合解释，规则仍需澄清。
- `ADDR_SEGM.place_type`、`area_type`、`addr_unit_type` 在样本中仍有较高空值占比，不能默认作为强筛选字段。
- `ADDR_SET_SEGM.set_type` 与 `status` 在样本中仅 `48` 条非空，空值占比都约 `95.31%`，说明安装地址表当前大量记录并未完整回填类型/状态。
- `ADDR_SET_SEGM.area_id`、`org_id` 在样本中仅 `48` 条非空，空值占比约 `95.31%` / `95.31%`，组织归属相关能力不能直接依赖现状数据。
- `ADDR_SET_SEGM.segm_id` 与 `region_id` 在样本中均为非空，说明安装地址到标准地址、区域的基础挂接字段现状较稳定。

## 5. 取值来源与字典结论

- 当前线上 `pub_restriction` 中，业务字段与主表编码的对齐列应优先使用 `serial_no`；`code` 当前已变为较短编码，不再直接对应大表中的 `2140xxx` 旧值。
- `ADDR_SEGM.status`、`ADDR_SET_SEGM.status` 对应 `pub_restriction.serial_no(keyword='ADDR_SEGM_STATUS')`。
- `ADDR_SEGM.place_type` / `opr_state` / `area_type` / `addr_unit_type` / `addr_in_type_ftth` / `ftth_pon_type` / `addr_in_type_lan` 均可落到 `pub_restriction.serial_no + keyword` 的具体字典口径。
- `ADDR_SEGM.segm_type`、`ADDR_SET_SEGM.segm_type` 可落到 `segm_addr_type.addr_type_id`。
- `ADDR_SEGM.addr_type` 仍是混合字典字段：当前观测值同时涉及 `pub_restriction.serial_no(keyword='addr_type')` 与 `pub_restriction.serial_no(keyword='ADDR_ADDR_TYPE')`，必须保留“来源待确认”备注。

## 6. 风险与下一步

- `ADDR_SEGM` 的安装站/营业站字段在样本中全部为空，需要确认是结构预留、历史遗留，还是迁移链路未补齐。
- `ADDR_SET_SEGM` 的 `set_type`、`status`、`area_id`、`org_id` 大面积为空，后续如果做安装地址治理、组织归属、同步下游，必须先明确“空值是否允许长期存在”。
- `staff` 仍需单独补盘，否则涉及操作人、创建人、岗位、权限的设计都不宜冻结。
- 当前 README 中引用的旧 8 表脚本已不适合作为最新线上盘点基线，后续若要反复重跑，建议单独补一个“非 bak 表 + 大表抽样”的新版采集脚本。
