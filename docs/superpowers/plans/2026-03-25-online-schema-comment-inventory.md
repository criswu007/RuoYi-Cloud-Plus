# 线上库字段注释与结构补盘 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于线上 `ftth_cloud_address` 当前 8 张表的最新字段注释，补齐字段级历史盘点证据，并输出一份可用于后续设计的全库结构手册。

**Architecture:** 先固化一份可复跑的 JShell 盘点脚本，统一采集表注释、字段注释、可疑枚举字段和值域分布；再沉淀原始证据；最后分别输出“补盘报告”和“结构手册”。结构手册强调字段事实、注释、取值来源和枚举可能值，不把注释直接等价为业务真相；对于标准地址表、安装地址表之外的依赖数据缺失或无法在当前 8 表内闭合的场景，显式标注缺口。

**Tech Stack:** Markdown、JShell、MySQL JDBC、SQL、Git

---

## Planned File Structure

### 计划新增文件

- `script/jshell/history-inventory/online-schema-comment-inventory.jsh`
  - 线上库 8 张表的注释/枚举/值域盘点脚本。
- `ruoyi-modules/ruoyi-address/docs/historical-inventory/evidence/online-schema-comment-inventory-evidence.md`
  - 线上库字段注释与枚举值盘点的原始证据摘录。
- `ruoyi-modules/ruoyi-address/docs/historical-inventory/online-schema-comment-inventory-report.md`
  - 本轮补盘的正式结论，说明注释覆盖度、可直接利用的信息、依赖缺口与使用边界。
- `ruoyi-modules/ruoyi-address/docs/historical-inventory/ftth-cloud-address-schema-handbook.md`
  - 全库 8 张表结构手册，逐表整理注释、取值来源、枚举可能值和缺口说明。

### 计划修改文件

- `ruoyi-modules/ruoyi-address/docs/historical-inventory/README.md`
  - 回写本轮补盘批次的入口链接。

### 不在本计划内的文件

- `ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-report.md`
- `ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-interface-input.md`
- `script/sql/ruoyi-address.sql`

本轮以“全库字段级补盘”为目标，不直接改写上一轮接口基线，避免把新注释直接混同为既有结论。

## Execution Notes

- 数据源固定为 `ftth_cloud_address` 线上库。
- 当前线上库仅 8 张表：
  - `pub_restriction`
  - `segm_addr_type`
  - `spc_region`
  - `spc_regional_company`
  - `spc_station`
  - `staff`
  - `tmp_addr_segm_nj_20260317`
  - `tmp_addr_set_segm_nj_20260317`
- 本轮输出必须覆盖四类信息：
  - 表注释与字段注释
  - 注释覆盖度与缺口
  - 枚举字段候选与实际值域
  - 取值来源与依赖缺失说明
- “取值来源”只允许基于当前 8 表与实际分布做归类，不能把未证实的外部系统、业务规则或历史程序逻辑写成确定事实。
- 必须先通过脚本原样列出当前 catalog 的完整表清单与总数，再据此声明“全库”范围；禁止只靠人工硬编码 8 张表直接下结论。
- 若 `TABLE_COUNT_ASSERT` 显示当前 catalog 实际表数不为 8，则必须停止后续报告/手册编写，先将脚本输出和差异表名反馈给用户确认范围；不得自行继续沿用“全库=8 张表”的结论。
- `取值来源` 判定优先级固定为：
  1. 字段注释直接给出来源或枚举
  2. `SOURCE_HINTS` 明确识别到当前 8 表内依赖表
  3. 当前表实际值域满足“非空 distinct 值 <= 20，且字段名不匹配 `_id` / `_name` / `_no` / `_code`，除非字段注释已明确其为枚举”时，才允许落为 `当前表实际值域`
  4. 以上均不满足时，统一落为 `当前 8 表内未识别`
- `ENUM_CANDIDATES` 判定规则固定为：
  - 字段注释包含显式枚举/布尔/状态/类型/来源/属性/能力提示时，必须纳入
  - 命中 `SOURCE_HINTS` 且来源为字典/依赖表的字段，必须纳入
  - 满足“非空 distinct 值 <= 20”的低值域字段，且不属于明显主数据标识字段时，可以纳入

### Task 1: 固化线上库字段注释盘点脚本

**Files:**
- Create: `script/jshell/history-inventory/online-schema-comment-inventory.jsh`

- [ ] **Step 1: 创建脚本骨架**

Create `script/jshell/history-inventory/online-schema-comment-inventory.jsh` with:
```java
import java.sql.*;
import java.util.*;

// 通过 ADDRESS_DB_URL / ADDRESS_DB_USERNAME / ADDRESS_DB_PASSWORD 读取连接
// 输出固定 label，便于后续摘录到证据文档
```

- [ ] **Step 2: 固定输出表与字段注释信息**

脚本至少输出以下固定 label：
- `TABLE_INVENTORY`
- `TABLE_COUNT_ASSERT`
- `TABLE_COMMENTS`
- `COLUMN_COMMENTS`
- `COMMENT_COVERAGE`
- `ENUM_CANDIDATES`
- `ENUM_VALUE_DISTRIBUTION`
- `SOURCE_HINTS`

要求：
- `TABLE_INVENTORY` 输出当前 catalog 的全部表名，按表名排序
- `TABLE_COUNT_ASSERT` 输出当前 catalog 的表总数，并显式标识本轮“全库”范围是否为 8 张表
- `TABLE_COMMENTS` 输出 `table_name / table_comment / row_count`
- `COLUMN_COMMENTS` 输出 `table_name / ordinal / column_name / column_type / nullable / key / comment`
- `COMMENT_COVERAGE` 输出每表字段总数与有注释字段数
- `ENUM_CANDIDATES` 必须严格按本计划已定义的规则判定，不能由实现者自由裁量
- `ENUM_VALUE_DISTRIBUTION` 输出候选字段的非空 distinct 值及数量
- `SOURCE_HINTS` 输出当前 8 表内可直接识别的来源提示，例如：
  - `region_id -> spc_region.region_id`
  - `station_id/installstation_id/busstation_id -> spc_station.station_id`
  - `job_id -> pub_restriction.code`（仅当注释已明确）

- [ ] **Step 3: 运行脚本验证可连库且标签稳定**

Run:
```bash
ADDRESS_DB_URL=... ADDRESS_DB_USERNAME=... ADDRESS_DB_PASSWORD=... \
jshell --class-path ~/.m2/repository/com/mysql/mysql-connector-j/9.5.0/mysql-connector-j-9.5.0.jar \
  script/jshell/history-inventory/online-schema-comment-inventory.jsh | sed -n '1,240p'
```

Expected:
- 输出 `## CONNECTED`
- 输出 `Current catalog: ftth_cloud_address`
- 能看到 8 类固定 label，且必须包含 `TABLE_INVENTORY` 与 `TABLE_COUNT_ASSERT`
- 不出现 `[QUERY ERROR]` / `[TABLE MISSING]`
- 如果 `TABLE_COUNT_ASSERT` 显示表总数不为 8，则本任务在此停止并转为向用户确认，不继续后续 Task 2-4

- [ ] **Step 4: 提交脚本**

Run:
```bash
git add script/jshell/history-inventory/online-schema-comment-inventory.jsh
git commit -m "docs: add online schema comment inventory runner"
```

Expected:
- 提交仅包含脚本

### Task 2: 生成字段注释补盘证据

**Files:**
- Create: `ruoyi-modules/ruoyi-address/docs/historical-inventory/evidence/online-schema-comment-inventory-evidence.md`

- [ ] **Step 1: 执行脚本并抓取完整输出**

Run:
```bash
ADDRESS_DB_URL=... ADDRESS_DB_USERNAME=... ADDRESS_DB_PASSWORD=... \
jshell --class-path ~/.m2/repository/com/mysql/mysql-connector-j/9.5.0/mysql-connector-j-9.5.0.jar \
  script/jshell/history-inventory/online-schema-comment-inventory.jsh > /tmp/online-schema-comment-inventory.txt
wc -l /tmp/online-schema-comment-inventory.txt
```

Expected:
- 生成 `/tmp/online-schema-comment-inventory.txt`
- 行数大于 0

- [ ] **Step 2: 摘录证据文档**

Create `ruoyi-modules/ruoyi-address/docs/historical-inventory/evidence/online-schema-comment-inventory-evidence.md` with sections:
```md
# 线上库字段注释补盘证据

## 1. 基础连接信息
## 2. 当前 catalog 全部表清单与总数
## 3. 表注释与行数
## 4. 注释覆盖度
## 5. 字段注释清单
## 6. 枚举候选字段与实际值域
## 7. 当前 8 表内可识别的取值来源提示
## 8. 缺口与异常保留项
```

要求：
- 摘录时保留原始 label
- 不对原始结果做二次加工推断
- 标准地址表、安装地址表需完整摘录字段注释
- `TABLE_INVENTORY` 与 `TABLE_COUNT_ASSERT` 必须原样摘录

- [ ] **Step 3: 校验证据覆盖 8 张表与关键字段**

Run:
```bash
rg -n "tmp_addr_segm_nj_20260317|tmp_addr_set_segm_nj_20260317|spc_station|spc_region|segm_addr_type|pub_restriction|spc_regional_company|staff" \
  ruoyi-modules/ruoyi-address/docs/historical-inventory/evidence/online-schema-comment-inventory-evidence.md
```

Expected:
- 8 张表均被覆盖

- [ ] **Step 4: 提交证据**

Run:
```bash
git add ruoyi-modules/ruoyi-address/docs/historical-inventory/evidence/online-schema-comment-inventory-evidence.md
git commit -m "docs: capture online schema comment inventory evidence"
```

Expected:
- 提交仅包含证据文档

### Task 3: 编写字段注释补盘报告

**Files:**
- Create: `ruoyi-modules/ruoyi-address/docs/historical-inventory/online-schema-comment-inventory-report.md`

- [ ] **Step 1: 创建报告骨架**

Create `ruoyi-modules/ruoyi-address/docs/historical-inventory/online-schema-comment-inventory-report.md` with:
```md
# 线上库字段注释补盘报告

## 1. 盘点范围
## 2. 注释覆盖度结论
## 3. 表级可用性结论
## 4. 枚举字段与值域结论
## 5. 取值来源与依赖缺口
## 6. 对后续设计使用的约束
```

- [ ] **Step 2: 填充注释覆盖度与表级可用性**

要求：
- 逐表说明：
  - 表注释是否完整
  - 字段注释覆盖度
  - 当前能否直接支撑字段级结构文档
- 必须显式写明“本轮全库范围=当前 catalog 下实际存在的 8 张表”，并引用证据中的表清单与表总数
- 至少明确以下结论：
  - `tmp_addr_segm_nj_20260317` 与 `tmp_addr_set_segm_nj_20260317` 字段注释已明显补强
  - `spc_station` 注释仍较稀疏
  - 依赖表的“取值来源”只能做到部分识别

- [ ] **Step 3: 填充枚举值与来源边界**

要求：
- 区分三类来源：
  - 字段注释直接给出的枚举
  - 当前表实际值域统计给出的枚举
  - 依赖字典/依赖表可识别但未完全闭合的来源
- 必须说明“当前表实际值域统计”采用的判定阈值为 `非空 distinct <= 20`
- 明确注明：
  - 标准地址表、安装地址表仅为部分数据
  - 部分依赖数据或字典映射在当前 8 表内找不到闭环

- [ ] **Step 4: 校验报告与证据一致**

Run:
```bash
sed -n '1,260p' ruoyi-modules/ruoyi-address/docs/historical-inventory/online-schema-comment-inventory-report.md
```

Expected:
- 不出现证据之外的确定性推断
- 风险和边界表述完整

- [ ] **Step 5: 提交报告**

Run:
```bash
git add ruoyi-modules/ruoyi-address/docs/historical-inventory/online-schema-comment-inventory-report.md
git commit -m "docs: add online schema comment inventory report"
```

Expected:
- 提交仅包含报告

### Task 4: 输出全库结构手册

**Files:**
- Create: `ruoyi-modules/ruoyi-address/docs/historical-inventory/ftth-cloud-address-schema-handbook.md`

- [ ] **Step 1: 创建结构手册骨架**

Create `ruoyi-modules/ruoyi-address/docs/historical-inventory/ftth-cloud-address-schema-handbook.md` with:
```md
# FTTH Address 线上库结构手册

## 1. 使用说明
## 2. 表级总览
## 3. 标准地址表
## 4. 安装地址表
## 5. 管理站与区域表
## 6. 字典与组织辅助表
## 7. 人员辅助表
## 8. 当前缺口说明
```

- [ ] **Step 2: 填充表级总览**

每表至少包含：
```md
| 表名 | 表注释 | 行数 | 角色定位 | 注释覆盖度 | 备注 |
|---|---|---:|---|---|---|
```

要求：
- `使用说明` 必须写明：当前“全库”是由证据中的 `TABLE_INVENTORY` / `TABLE_COUNT_ASSERT` 证明，而非人工预设
- `表级总览` 必须覆盖当前 catalog 下全部 8 张表，不允许只写业务相关子集

- [ ] **Step 3: 逐表填充字段清单**

每张表的字段表至少包含：
```md
| 字段序号 | 字段名 | 类型 | 可空 | 键 | 字段注释 | 取值来源 | 可能取值/样例 | 备注 |
|---|---|---|---|---|---|---|---|---|
```

要求：
- `取值来源` 只能写：
  - `字段注释`
  - `当前表实际值域`
  - `依赖表 <table>.<column>`
  - `当前 8 表内未识别`
- `取值来源` 必须严格按本计划已定义的优先级判定，且能在证据文档中找到直接依据
- 涉及枚举值的字段，`可能取值/样例` 必须列出当前可识别的所有值
- `当前表实际值域` 仅允许用于“非空 distinct <= 20”的字段，且必须在备注中标明“来自当前表值域统计，不等价于完整业务字典”
- 对 `tmp_addr_segm_nj_20260317`、`tmp_addr_set_segm_nj_20260317`，明确说明“当前仅为部分数据”
- 对依赖找不到闭环的字段，备注里明确写“当前依赖数据可能缺失/无法在现有 8 表确认”

- [ ] **Step 4: 更新索引入口**

Update `ruoyi-modules/ruoyi-address/docs/historical-inventory/README.md`：
- 不新增新的“领域包”条目
- 在现有 README 末尾新增 `## 补充资料` 章节
- 在该章节中至少链接：
  - `online-schema-comment-inventory-report.md`
  - `ftth-cloud-address-schema-handbook.md`
  - `evidence/online-schema-comment-inventory-evidence.md`

- [ ] **Step 5: 校验手册完整性**

Run:
```bash
rg -n "tmp_addr_segm_nj_20260317|tmp_addr_set_segm_nj_20260317|spc_station|spc_region|segm_addr_type|pub_restriction|spc_regional_company|staff" \
  ruoyi-modules/ruoyi-address/docs/historical-inventory/ftth-cloud-address-schema-handbook.md
```

Expected:
- 8 张表均有独立章节或表格记录

- [ ] **Step 6: 提交手册与索引**

Run:
```bash
git add ruoyi-modules/ruoyi-address/docs/historical-inventory/ftth-cloud-address-schema-handbook.md \
        ruoyi-modules/ruoyi-address/docs/historical-inventory/README.md
git commit -m "docs: add ftth address schema handbook"
```

Expected:
- 提交仅包含手册与索引更新

## Definition of Done

- 线上库 8 张表均完成字段注释与注释覆盖度补盘。
- 有可复跑的盘点脚本，可再次抓取最新字段注释与枚举值分布。
- 有原始证据文档，能支撑后续复核。
- 有补盘报告，明确注释覆盖度、可利用信息和依赖缺口。
- 有全库结构手册，逐表整理注释、取值来源和枚举可能值。
- README 已提供本轮补盘入口。

## Out of Scope

- 不修改接口基线文档内容
- 不直接设计新的数据库模型
- 不补写 Controller/Service/Mapper 实现
- 不对业务规则作超出当前 8 表与注释范围的推断
