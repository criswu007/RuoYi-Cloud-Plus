# 南京标准地址核心历史盘点 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于南京历史库已识别的 8 张标准地址主链路相关表，形成可复核、可持续追加的正式盘点报告与接口设计输入清单，为后续接口设计、兼容接口收敛和目标模型讨论提供事实基线。

**Architecture:** 采用“三层产物”推进：先固化可重复执行的历史库盘点脚本，再沉淀带标签的原始证据，最后输出结构化报告与接口设计输入清单。整个过程只收敛历史事实、字段语义、数据质量和风险，不直接推导目标数据库模型或实现方案。

**Tech Stack:** Markdown、JShell、MySQL JDBC、SQL、Git

---

## Planned File Structure

### 计划新增文件

- `ruoyi-modules/ruoyi-address/docs/historical-inventory/README.md`
  - 历史盘点总索引，记录领域包、批次、状态和文档链接。
- `ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-report.md`
  - 南京标准地址核心正式盘点报告。
- `ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-interface-input.md`
  - 南京标准地址核心接口设计输入清单。
- `ruoyi-modules/ruoyi-address/docs/historical-inventory/evidence/nanjing-standard-address-core-evidence.md`
  - 带标签的原始查询证据摘录，供后续复核。
- `script/jshell/history-inventory/nanjing-standard-address-core.jsh`
  - 历史库盘点执行脚本，统一打印表结构、数据量、空值、关联一致性等结果。

### 可能修改文件

- `ruoyi-modules/ruoyi-address/docs/project-baseline.md`
  - 当前主入口已收敛到项目总控基线；历史盘点入口说明如需调整，应优先改这里。

### 不在本计划内的文件

- `script/sql/ruoyi-address.sql`
- `ruoyi-modules/ruoyi-address/docs/archive/standard-address-db-design.md`
- `ruoyi-modules/ruoyi-address/docs/interface-checklist.md`
- `ruoyi-modules/ruoyi-address/docs/reference/vibe-coding-api-manual.md`

这些文件在历史事实未进一步收敛前不作为本轮执行目标，避免再次把“方案稿”误当基线。

## Execution Notes

- 数据源固定为 `ftth_cloud_address` 历史库，范围先聚焦南京。
- 当前已知主链路表共 8 张：
  - `tmp_addr_segm_nj_20260317`
  - `tmp_addr_set_segm_nj_20260317`
  - `spc_station`
  - `spc_region`
  - `segm_addr_type`
  - `pub_restriction`
  - `spc_regional_company`
  - `staff`
- 查询执行方式统一使用 `jshell + mysql-connector-j`，避免依赖本机缺失的 `mysql` CLI。
- 盘点输出必须覆盖四类内容：
  - 表清单
  - 事实结论
  - 接口设计输入
  - 风险缺口

### Task 1: 建立历史盘点目录与索引骨架

**Files:**
- Create: `ruoyi-modules/ruoyi-address/docs/historical-inventory/README.md`
- Create: `ruoyi-modules/ruoyi-address/docs/historical-inventory/evidence/.gitkeep`

- [ ] **Step 1: 确认目标目录当前不存在或为空**

Run:
```bash
ls -la ruoyi-modules/ruoyi-address/docs
ls -la ruoyi-modules/ruoyi-address/docs/historical-inventory
```

Expected:
- 第一条命令能看到现有 `docs` 文件列表
- 第二条命令若目录不存在则报错，若已存在则内容为空或仅有历史产物

- [ ] **Step 2: 创建目录与索引文件**

Create `ruoyi-modules/ruoyi-address/docs/historical-inventory/README.md` with:
```md
# 历史库盘点索引

## 领域包状态

| 领域包 | 当前批次 | 状态 | 文档 |
|---|---|---|---|
| 标准地址核心 | 南京标准地址主链路基线批次 | 进行中 | 待补链接 |
| 安装地址与资源关系 | - | 待展开 | - |
| 客户与地址关系 | - | 缺口待确认 | - |
| 网格主数据 | - | 缺口待确认 | - |
| 网格关系与归属 | - | 缺口待确认 | - |
```

- [ ] **Step 3: 校验目录与索引文件已生成**

Run:
```bash
ls -la ruoyi-modules/ruoyi-address/docs/historical-inventory
sed -n '1,80p' ruoyi-modules/ruoyi-address/docs/historical-inventory/README.md
```

Expected:
- 目录存在
- `README.md` 包含 5 个领域包与状态表

- [ ] **Step 4: 提交目录骨架**

Run:
```bash
git add ruoyi-modules/ruoyi-address/docs/historical-inventory/README.md ruoyi-modules/ruoyi-address/docs/historical-inventory/evidence/.gitkeep
git commit -m "docs: initialize historical inventory workspace"
```

Expected:
- 提交只包含目录骨架与索引文件

### Task 2: 固化可复跑的南京标准地址核心盘点脚本

**Files:**
- Create: `script/jshell/history-inventory/nanjing-standard-address-core.jsh`

- [ ] **Step 1: 创建 JShell 盘点脚本骨架**

Create `script/jshell/history-inventory/nanjing-standard-address-core.jsh` with:
```java
import java.sql.*;
import java.util.*;

String url = "jdbc:mysql://82.156.6.111:4000/ftth_cloud_address?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true";
String username = "address";
String password = "Wetry2328!";

void printQuery(Connection conn, String label, String sql) throws Exception {
    try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
        ResultSetMetaData meta = rs.getMetaData();
        int columns = meta.getColumnCount();
        System.out.println("## " + label);
        while (rs.next()) {
            List<String> row = new ArrayList<>();
            for (int i = 1; i <= columns; i++) {
                row.add(String.valueOf(rs.getObject(i)));
            }
            System.out.println(String.join("|", row));
        }
        System.out.println();
    }
}

try (Connection conn = DriverManager.getConnection(url, username, password)) {
    System.out.println("## CONNECTED");
    System.out.println(conn.getCatalog());
}
```

- [ ] **Step 2: 补充固定查询集合**

在同一脚本中追加以下类别查询，每类至少 1 条，且输出 label 必须稳定：
- `TABLE_ROW_COUNTS`
- `ADDR_MAIN_COLUMNS`
- `INSTALL_ADDR_MAIN_COLUMNS`
- `NULL_CHECKS`
- `DUPLICATE_CHECKS`
- `RELATION_CHECKS`
- `DICTIONARY_CHECKS`

Query examples:
```sql
select 'tmp_addr_segm_nj_20260317', count(*) from tmp_addr_segm_nj_20260317;
select count(*) as missing_parent from tmp_addr_segm_nj_20260317 t
left join tmp_addr_segm_nj_20260317 p on t.parent_segm_id = p.segm_id
where t.parent_segm_id is not null and p.segm_id is null;
select segm_type, count(*) from tmp_addr_segm_nj_20260317 group by segm_type order by count(*) desc;
```

- [ ] **Step 3: 运行脚本验证可连库**

Run:
```bash
jshell --class-path ~/.m2/repository/com/mysql/mysql-connector-j/9.5.0/mysql-connector-j-9.5.0.jar script/jshell/history-inventory/nanjing-standard-address-core.jsh
```

Expected:
- 输出 `## CONNECTED`
- 输出库名 `ftth_cloud_address`
- 不出现 JDBC 驱动缺失或认证失败

- [ ] **Step 4: 补齐缺失查询并再次执行**

Run:
```bash
jshell --class-path ~/.m2/repository/com/mysql/mysql-connector-j/9.5.0/mysql-connector-j-9.5.0.jar script/jshell/history-inventory/nanjing-standard-address-core.jsh | sed -n '1,200p'
```

Expected:
- 能看到所有固定 label
- 输出覆盖 8 张表的结构/数量/关联结果

- [ ] **Step 5: 提交脚本**

Run:
```bash
git add script/jshell/history-inventory/nanjing-standard-address-core.jsh
git commit -m "docs: add nanjing standard address inventory runner"
```

Expected:
- 提交只包含盘点脚本

### Task 3: 生成并沉淀原始证据

**Files:**
- Create: `ruoyi-modules/ruoyi-address/docs/historical-inventory/evidence/nanjing-standard-address-core-evidence.md`

- [ ] **Step 1: 执行脚本并抓取完整输出**

Run:
```bash
jshell --class-path ~/.m2/repository/com/mysql/mysql-connector-j/9.5.0/mysql-connector-j-9.5.0.jar script/jshell/history-inventory/nanjing-standard-address-core.jsh > /tmp/nanjing-standard-address-core.txt
wc -l /tmp/nanjing-standard-address-core.txt
```

Expected:
- 生成 `/tmp/nanjing-standard-address-core.txt`
- 输出行数大于 0

- [ ] **Step 2: 将关键证据摘录到 evidence 文档**

Create `ruoyi-modules/ruoyi-address/docs/historical-inventory/evidence/nanjing-standard-address-core-evidence.md` with sections:
```md
# 南京标准地址核心盘点证据

## 1. 基础连接信息
## 2. 表数量与数据量
## 3. 标准地址主事实表关键分布
## 4. 安装地址关联事实关键分布
## 5. 区域/站点/字典辅助表分布
## 6. 关键关联一致性检查
## 7. 原始异常与待确认项
```

摘录时保留原始 label 与结果，避免二次转述失真。

- [ ] **Step 3: 校验证据文档覆盖 8 张表**

Run:
```bash
rg -n "tmp_addr_segm_nj_20260317|tmp_addr_set_segm_nj_20260317|spc_station|spc_region|segm_addr_type|pub_restriction|spc_regional_company|staff" ruoyi-modules/ruoyi-address/docs/historical-inventory/evidence/nanjing-standard-address-core-evidence.md
```

Expected:
- 8 张表名都能在证据文档中找到

- [ ] **Step 4: 提交证据文档**

Run:
```bash
git add ruoyi-modules/ruoyi-address/docs/historical-inventory/evidence/nanjing-standard-address-core-evidence.md
git commit -m "docs: capture nanjing standard address inventory evidence"
```

Expected:
- 提交只包含证据文档

### Task 4: 编写南京标准地址核心正式盘点报告

**Files:**
- Create: `ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-report.md`
- Modify: `ruoyi-modules/ruoyi-address/docs/historical-inventory/README.md`

- [ ] **Step 1: 创建报告骨架**

Create `ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-report.md` with:
```md
# 南京标准地址核心正式盘点报告

## 1. 盘点范围
## 2. 表清单与角色定位
## 3. 标准地址主链路事实结论
## 4. 各表结构与字段口径摘要
## 5. 数据质量结论
## 6. 跨表一致性结论
## 7. 风险清单
## 8. 对接口设计的直接输入
## 9. 暂不下结论事项
```

- [ ] **Step 2: 填充“表清单与角色定位”章节**

要求：
- 8 张表逐表落位
- 标注主事实表、关联事实表、主数据表、辅助表
- 对 `tmp_addr_set_segm_nj_20260317` 写明“当前归入标准地址核心仅为主链路盘点需要”

- [ ] **Step 3: 填充“事实结论”和“数据质量结论”章节**

要求：
- 至少覆盖主键/业务键、核心字段、数据量、空值/异常值、父子关系和挂接关系
- 至少明确以下链路：
  - `tmp_addr_segm_nj_20260317` 自关联父子关系
  - `tmp_addr_set_segm_nj_20260317 -> tmp_addr_segm_nj_20260317`
  - `tmp_addr_segm_nj_20260317 -> spc_region`
  - `tmp_addr_segm_nj_20260317 -> spc_station`

- [ ] **Step 4: 填充“风险清单”和“暂不下结论事项”章节**

风险类型必须固定为：
```md
- 结构风险
- 质量风险
- 映射风险
- 能力缺口
```

- [ ] **Step 5: 回写索引文件中的文档链接与状态**

Update `ruoyi-modules/ruoyi-address/docs/historical-inventory/README.md`:
```md
| 标准地址核心 | 南京标准地址主链路基线批次 | 已完成首版 | `nanjing-standard-address-core-report.md` / `nanjing-standard-address-core-interface-input.md` |
```

- [ ] **Step 6: 校验报告完整性**

Run:
```bash
sed -n '1,260p' ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-report.md
rg -n "结构风险|质量风险|映射风险|能力缺口|tmp_addr_segm_nj_20260317|tmp_addr_set_segm_nj_20260317" ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-report.md
```

Expected:
- 报告包含所有章节
- 风险分类齐全
- 两张事实表都被清晰描述

- [ ] **Step 7: 提交报告与索引更新**

Run:
```bash
git add ruoyi-modules/ruoyi-address/docs/historical-inventory/README.md ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-report.md
git commit -m "docs: add nanjing standard address core inventory report"
```

Expected:
- 提交仅包含报告与索引更新

### Task 5: 编写接口设计输入清单

**Files:**
- Create: `ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-interface-input.md`

- [ ] **Step 1: 创建接口输入清单骨架**

Create `ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-interface-input.md` with:
```md
# 南京标准地址核心接口设计输入清单

## 1. 使用原则
## 2. 可直接进入接口的字段
## 3. 需转换后进入接口的字段
## 4. 当前无法直接支撑的接口能力
## 5. 待需求澄清字段
```

- [ ] **Step 2: 填充字段映射表**

每条字段映射至少包含以下列：
```md
| 来源表 | 来源字段 | 历史语义 | 建议接口位置 | 是否可直接使用 | 备注 |
|---|---|---|---|---|---|
```

至少覆盖以下字段族：
- 标准地址标识与层级：`id`、`segm_id`、`segm_type`、`segm_name`、`parent_segm_id`
- 展示名称与编码：`stand_name`、`stand_no`
- 区域与公司归属：`region_id`、`service_region_id`
- 管理站归属：`station_id`、`installstation_id`、`busstation_id`
- 接入与覆盖能力：`addr_in_type`、`addr_in_type_ftth`、`ftth_pon_type`、`addr_in_type_lan`、`cover_num`
- 安装地址挂接：`set_addr_id`、`set_addr_name`、`segm_id`

- [ ] **Step 3: 填充“当前无法直接支撑的接口能力”章节**

必须明确以下内容是否存在历史缺口：
- 标签体系
- 合并/拆分显式操作痕迹
- 规则引擎与监控任务
- 异常地址与工单链路
- 网格归属字段

- [ ] **Step 4: 校验输入清单与报告一致**

Run:
```bash
rg -n "segm_id|stand_name|station_id|cover_num|set_addr_id" ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-interface-input.md
```

Expected:
- 关键字段族均被覆盖
- 未引入证据文档和报告之外的臆断字段

- [ ] **Step 5: 提交接口输入清单**

Run:
```bash
git add ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-interface-input.md
git commit -m "docs: add nanjing standard address interface inputs"
```

Expected:
- 提交只包含接口输入清单

### Task 6: 交叉复核并形成下一步入口

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/docs/historical-inventory/README.md`
- Modify: `ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-report.md`
- Modify: `ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-interface-input.md`

- [ ] **Step 1: 做一次交叉一致性检查**

Checklist:
- 报告、证据、接口输入清单三者中的 8 张表口径一致
- 行数、主外键、关键字段名称一致
- 风险项与“无法直接支撑的接口能力”不互相矛盾

- [ ] **Step 2: 将下一步入口补到索引文件**

Update `ruoyi-modules/ruoyi-address/docs/historical-inventory/README.md` with:
```md
## 下一步建议

1. 展开“安装地址与资源关系”包，重点找安装地址与设备/房间/资源关系表
2. 展开“客户与地址关系”包，识别客户主表和客户地址关系表
3. 对网格相关关键词做一次全库存在性检索，确认是否为跨库缺口
```

- [ ] **Step 3: 运行最终校验命令**

Run:
```bash
sed -n '1,200p' ruoyi-modules/ruoyi-address/docs/historical-inventory/README.md
sed -n '1,260p' ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-interface-input.md
```

Expected:
- 索引文件、报告、接口输入清单都可独立阅读
- 下一步入口明确

- [ ] **Step 4: 提交最终整理**

Run:
```bash
git add ruoyi-modules/ruoyi-address/docs/historical-inventory/README.md \
        ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-report.md \
        ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-interface-input.md
git commit -m "docs: finalize nanjing standard address historical inventory baseline"
```

Expected:
- 提交只包含最终整理和交叉复核修改

## Definition of Done

- `标准地址核心` 包已有固定入口和索引。
- 南京首批 8 张表有可复跑的盘点脚本。
- 有带标签的原始证据文档，可支持二次复核。
- 有正式盘点报告，覆盖表清单、事实结论、接口设计输入、风险缺口。
- 有独立的接口设计输入清单，能直接服务后续接口和模型讨论。
- 索引文件已明确下一个要展开的领域包。

## Out of Scope

- 不设计新系统数据库表结构
- 不修改 Controller/Service/Mapper 实现
- 不补写 Swagger/OpenAPI
- 不展开旧系统兼容接口逐条实现
- 不在本计划内处理 ES 建模和增量同步实现方案
