# 历史结构与数据盘点索引

> 本目录只保留当前仍在使用的盘点材料，服务于“线上最新结构核实、字段溯源、风险追查、后续模块补盘入口”。旧批次、旧 `bak_` / `tmp_*` 专题已移入 [archive/README.md](../archive/README.md)，不再作为当前开发主依据。

## 当前活跃材料

- [online-schema-comment-inventory-report.md](./online-schema-comment-inventory-report.md)
  - 线上最新非备份表的表级盘点结论。
  - 当前判断“是否可直接入模”的主入口。
- [ftth-cloud-address-schema-handbook.md](./ftth-cloud-address-schema-handbook.md)
  - 线上最新非备份表的字段速查手册。
  - 当前查字段注释、取值来源、候选值的主入口。
- [online-schema-comment-inventory-evidence.md](./evidence/online-schema-comment-inventory-evidence.md)
  - 线上最新非备份表的证据摘录。
  - 只在需要追证据或核对统计口径时使用。
- [grid-module-structure-baseline.md](./grid-module-structure-baseline.md)
  - 网格模块的后续入口。
  - 当前在线库还没有正式网格表时，先以历史样例和装载 SQL 作为结构起点。

## 当前标准地址使用口径

- 标准地址程序设计默认只以线上最新非备份表为主基线。
- 当前主表为 `ADDR_SEGM`，安装地址主表为 `ADDR_SET_SEGM`。
- `ADDR_SEGM`、`ADDR_SET_SEGM` 等超大表按全量结构盘点、按稳定 hash 抽样观察数据分布。
- 有明确外部来源的字段必须写具体表或字典；字符串 ID 只写来源，不展开具体值。
- 如果后续需要追查旧批次迁移问题，再进入归档目录，不回到当前主线文档混用。

## 后续模块入口

| 模块 | 当前入口 | 说明 |
| --- | --- | --- |
| 安装地址与资源关系 | `ADDR_SET_SEGM` 相关章节 | 当前先依托在线盘点结论推进，独立专题后续补。 |
| 网格模块 | [grid-module-structure-baseline.md](./grid-module-structure-baseline.md) | 作为后续网格主数据、关系表、导入策略的正式入口。 |
| 客户与地址关系 | 待补 | 后续补盘后仍收敛在本目录，不再新起散落文档。 |
| 设备与安装地址关系 | 待补 | 后续补盘后仍收敛在本目录。 |

## 归档说明

- 旧 `bak_` / `tmp_*` 批次专题、南京历史批次专题、旧结构过程稿已移入 [archive/README.md](../archive/README.md)。
- 归档资料只用于迁移追溯、差异排查和历史事实比对，不作为当前结构建模和程序设计的默认入口。
