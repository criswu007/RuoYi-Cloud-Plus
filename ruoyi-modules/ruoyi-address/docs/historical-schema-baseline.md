# 标准地址与网格历史库表结构开发基线

> 自本版起，标准地址与网格模块开发默认遵循“先当前线上结构、后扩展设计”的原则。旧版目标模型设计稿和旧批次专题已归档，不再作为主开发依据。

## 1. 使用顺序

1. 先看当前文档，确认当前开发必须依赖的主表、关系和禁止事项。
2. 再看 [online-schema-comment-inventory-report.md](./historical-inventory/online-schema-comment-inventory-report.md) 和 [ftth-cloud-address-schema-handbook.md](./historical-inventory/ftth-cloud-address-schema-handbook.md)，确认当前线上非备份表结构。
3. 若涉及迁移差异、安装地址挂接、历史值域或旧系统现象，再补看归档的南京批次专题，不把归档当成唯一结构基线，但允许作为补充证据参与判断。
4. 若涉及接口或对外协议，再结合 [interface-checklist.md](./interface-checklist.md) 确认能力边界。
5. 若结构与需求书冲突，以需求书定业务目标，但实现时不得脱离当前线上结构凭空另起一套核心模型。

## 2. 结构基线优先级

### 2.1 标准地址

- 当前主开发结构基线：`ftth_cloud_address` 当前线上非备份表结构
  - 主事实表：`ADDR_SEGM`
  - 关联主表：`ADDR_SET_SEGM`
  - 结构/字典表：`segm_addr_type`、`pub_restriction`
  - 旁证表：`spc_region`、`spc_station`、`spc_regional_company`、`getpageVol`、`sync_set_addr_info`
- 归档的南京批次专题可补充以下信息：历史值域、空值率、安装地址挂接现象、迁移兼容链路、旧系统字段口径。见 [archive/README.md](./archive/README.md)。
- 需求书明确要求 `1/2` 级行政区划按 `spec_region` 查询，其余层级按 `segm_addr` 查询；当前在线结构文档写作 `spc_region`，实现前必须核对联调库实际表名。

### 2.2 网格

- 当前在线库 `ftth_cloud_address` 中未发现正式网格业务表。
- 网格开发基线改为依赖历史样例、装载 SQL 和字段注释整理结果，正式摘要见 [historical-inventory/grid-module-structure-baseline.md](./historical-inventory/grid-module-structure-baseline.md)。
- 在形成新的线上正式库表前，网格核心实体、字段命名和关系方向不得脱离这些历史表样例单独重造。

## 3. 标准地址开发必须依赖的历史结构

| 范围 | 历史表 | 开发使用原则 |
| --- | --- | --- |
| 标准地址主事实 | `ADDR_SEGM` | 标识、层级、名称、状态、区域、站点、坐标等主字段优先沿用当前线上口径。 |
| 安装地址主事实 | `ADDR_SET_SEGM` | 安装地址仍视为独立实体，但默认关联标准地址；当前阶段先保留结构入口，不单独扩展安装地址专题。 |
| 层级/类型解释 | `segm_addr_type` | 地址层级、类型解释、legacy `segm_type` 相关逻辑需围绕历史字典展开；尤其要区分普通 `房间` 与 `尾级地址(选址生成)`，不允许在实现中把选址生成地址直接硬编码成普通房间类型。 |
| 字典与限制 | `pub_restriction` | 编码解释优先按 `keyword + serial_no` 口径，不直接假设 `code` 可唯一代表历史值。 |
| 区域主数据 | `spc_region` | `1/2` 级行政区划与区域主数据口径需结合需求书与联调库实际命名核实。 |
| 管理站主数据 | `spc_station` | 管理站字段、区域归属、legacy 兼容优先参考历史结构，业务角色在关系层补齐。 |
| 组织映射 | `spc_regional_company` | 区域公司映射与组织旁证优先复用历史字段。 |
| 历史标准地址样本 | `tmp_addr_segm_nj_20260317` | 用于补充历史值域、迁移差异、地址拼装和旧系统兼容现象，不替代当前线上主事实表。 |
| 历史安装地址样本 | `tmp_addr_set_segm_nj_20260317` | 用于补充安装地址挂接现象、设备关联字段和历史治理场景。 |

## 4. 网格开发必须依赖的历史结构

| 范围 | 历史表 | 开发使用原则 |
| --- | --- | --- |
| 网格主数据 | `grid_TOJF` | 网格主键、组织归属、经理挂接等字段优先跟随历史样例。 |
| 网格经理 | `grid_manager_tojf` | 经理主键、手机号、组织归属、状态口径优先复用历史字段。 |
| 网格组织 | `ORGANIZATION_TOJF` | 组织树、路径、层级、类型先跟随历史组织表。 |
| 网格地址关系 | `GRID_ADDR_REL_TOJF` | 标准地址与网格的关系先按历史关联表理解。 |
| 客户网格关系 | `CUST_GRID_REL_TOJF_TD` | 客户归属、经理/团队归属和来源分析需优先参考历史关系表。 |
| 客户基础信息 | `cust_TOJF` / `cust_TOJF_TD` | 客户基础字段与网格挂接能力先跟随历史字段。 |

## 5. 禁止事项

- 不再以 [archive/standard-address-db-design.md](./archive/standard-address-db-design.md) 或任何旧批次专题作为主落库依据。
- 不得为了“模型更漂亮”而跳过 `ADDR_SEGM`、`ADDR_SET_SEGM`、`grid_TOJF` 等历史主表，直接另建一套脱离历史迁移链路的核心表。
- 不得在未核实历史字段语义前，把 `is_city`、`area_type`、`status`、`delete_state`、`addr_type`、`segm_type` 等字段按字面随意重解释。
- 不得把 `pub_restriction`、`staff` 之类旁证表直接当作已冻结、无歧义的业务主模型。
- 不得把归档南京批次专题完全排除出实现判断；凡涉及迁移、双向同步、安装地址治理、legacy 兼容时，必须补看对应历史现象证据。
- 文档、BO/VO、Mapper 和 SQL 的字段命名不得长期停留在 `id/name/fullName/code/remark` 这类泛化表述；标准地址、安装地址主模型默认应优先向 `segm_*`、`stand_*`、`set_addr_*`、`region_id`、`station_id` 等线上主字段口径收敛。

## 6. 当前建议读法

- 标准地址结构边界先看：[historical-inventory/online-schema-comment-inventory-report.md](./historical-inventory/online-schema-comment-inventory-report.md)
- 标准地址字段速查再看：[historical-inventory/ftth-cloud-address-schema-handbook.md](./historical-inventory/ftth-cloud-address-schema-handbook.md)
- 标准地址迁移现象、历史样本和值域补充再看：[archive/legacy-standard-address-batch/nanjing-standard-address-core-report.md](./archive/legacy-standard-address-batch/nanjing-standard-address-core-report.md)
- 网格历史结构摘要看：[historical-inventory/grid-module-structure-baseline.md](./historical-inventory/grid-module-structure-baseline.md)
- 需要旧批次链路、历史迁移事实或过程稿时，再去看：
  - [archive/README.md](./archive/README.md)
  - `/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/archive/address-working-notes`
  - `/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/archive/address-docs`
