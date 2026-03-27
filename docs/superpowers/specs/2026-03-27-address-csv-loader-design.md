# 历史地址 CSV 代码导入工具设计

## 1. 背景与目标

当前 `ftth_cloud_address` 线上库需要按甲方提供的标准地址模块资料完成重建，并将数据样例中的历史 CSV 数据重新灌入 TiDB。

已确认前提如下：
- 线上数据库无法登录服务器执行文件级导入。
- 可从本地电脑直接通过 JDBC 连接远端 TiDB。
- 样例 CSV 文件体积较大，其中 `ADDR_SEGM` 对应文件约 1.8G，`ADDR_SET_SEGM` 对应文件约 726M。
- 当前样例 CSV 编码为 `gb18030`，首行带表头，字段以逗号分隔，双引号包裹，换行符为 `LF`。
- 目标表与表结构口径以已整理产物为准，尤其标准地址表与安装地址表分别使用 `ADDR_SEGM`、`ADDR_SET_SEGM`。

本设计目标是提供一个可在本地直接运行的独立 CLI 工具，按“逐行读取、分批提交”的方式将大 CSV 稳定导入远端 TiDB，并支持断点续跑和坏数据隔离。

本设计不覆盖以下内容：
- 目标库 DDL 本身的生成规则
- 业务模块内正式导入接口设计
- 服务器侧文件上传、`LOAD DATA LOCAL INFILE` 或 `IMPORT INTO` 运维方案
- 导入后业务校验与数据修复规则

## 2. 已确认约束

- 工具应与 `ruoyi-address` 业务模块解耦，避免引入 Nacos、微服务启动链路或现有业务导入逻辑。
- 工具使用 Java 17 运行，与当前仓库基础环境保持一致。
- 工具需直接复用当前仓库中已经沉淀的资料与产物，尤其是：
  - `/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/ftth_cloud_address_rebuild_full_ddl.sql`
  - `/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/ftth_cloud_address_csv_load_templates.sql`
  - `/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/ftth_cloud_address_csv_import_guide.md`
- 工具的导入策略已确认采用“逐行读取 + 分批提交”，而不是单行单事务或整文件单事务。
- 工具要优先保障可恢复性、可观测性和失败隔离，而不是追求极限吞吐。

## 3. 方案对比与结论

### 3.1 方案 A：独立 Maven CLI + 纯 JDBC

特点：
- 独立于业务服务运行。
- 不依赖 Spring Boot 上下文。
- 直接使用 `PreparedStatement` 批量写入远端 TiDB。

优点：
- 耦合最低，最适合一次性历史重建任务。
- 启动快，运行边界简单，排错路径短。
- 可在本地直接通过 `mvn exec` 或打包后的 `jar` 执行。

缺点：
- 需要自行实现 CSV 解析、类型转换、checkpoint、坏数据输出等能力。

### 3.2 方案 B：独立 Spring Boot CLI

特点：
- 在单独模块中启动 Spring Boot。
- 复用配置体系和日志体系。

优点：
- 代码风格接近现有仓库。
- 可以复用部分 DataSource 或组件能力。

缺点：
- 启动更重，容易被现有 profile、配置中心、starter 链路牵连。
- 对一次性导入任务而言复杂度偏高。

### 3.3 方案 C：离线生成 INSERT SQL

特点：
- 先将 CSV 转成 SQL 文件，再手工执行 SQL。

优点：
- 实现路径直接。

缺点：
- 超大 CSV 会生成超大 SQL 文件，执行和排错都不友好。
- 转义、坏数据定位、失败续跑都较麻烦。

### 3.4 最终结论

采用方案 A：在仓库内新增一个独立 Maven CLI 工具，以纯 JDBC 方式导入 CSV。

## 4. 工具边界与目录规划

工具建议放在：

`/Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/address-csv-loader`

目录结构建议如下：

```text
address-csv-loader/
  pom.xml
  README.md
  src/main/java/.../
  src/test/java/.../
```

该工具只承担以下职责：
- 解析命令行参数
- 读取指定 CSV
- 按目标表定义执行参数绑定和批量写入
- 记录 checkpoint
- 输出坏数据与运行日志

该工具不承担以下职责：
- 自动建表
- 自动比对线上结构
- 自动修复历史脏数据
- 自动推断目标表字段顺序

## 5. 核心组件设计

### 5.1 `AddressCsvLoaderApplication`

目的：程序主入口，负责解析命令行参数并组织整个导入流程。

核心职责：
- 校验输入参数完整性
- 根据目标表加载表定义
- 初始化 JDBC 连接、checkpoint、坏数据输出器
- 控制批量执行与进度打印

### 5.2 `LoaderOptions`

目的：承载命令行参数与运行期配置。

建议参数包括：
- `jdbcUrl`
- `username`
- `password`
- `table`
- `csv`
- `charset`
- `batchSize`
- `checkpointDir`
- `errorDir`
- `startLine`
- `maxRetryTimes`
- `logEveryRows`

关键约束：
- `table` 必须命中注册表中的已支持表。
- `batchSize` 必须大于 0。
- `startLine` 不能小于 2，因为首行是表头。

### 5.3 `TableSpecRegistry`

目的：统一维护目标表定义，避免运行时依赖动态探测或手工拼列。

每张表至少定义：
- 目标表名
- 列顺序
- 列对应 JDBC 类型
- 主键列信息
- 默认导入文件名

首批支持表范围：
- `pub_restriction`
- `segm_addr_type`
- `spc_region`
- `spc_regional_company`
- `spc_station`
- `staff`
- `ADDR_SEGM`
- `ADDR_SET_SEGM`

字段顺序以现有重建 DDL 产物为准，不在运行时自行猜测。

### 5.4 `CsvStreamingReader`

目的：流式读取超大 CSV 文件，避免整体载入内存。

要求：
- 支持 `gb18030`
- 支持带表头 CSV
- 正确处理双引号包裹、逗号、空字段
- 提供当前物理行号

关键约束：
- 不将整文件读入内存
- 不因单行异常中断整个读取过程

### 5.5 `JdbcBatchWriter`

目的：将解析后的单行数据绑定到 `PreparedStatement`，并按批次提交。

职责：
- 生成固定 `INSERT INTO ... VALUES ...` SQL
- 执行参数绑定
- 按批量提交事务
- 在批失败时降级为单行重试

关键约束：
- 每次成功 `commit` 后才允许更新 checkpoint。
- 批失败时不得直接丢弃整批数据。

### 5.6 `CheckpointStore`

目的：记录导入进度，支持断点续跑。

每个 checkpoint 至少记录：
- 目标表名
- CSV 文件路径
- 文件大小
- 文件最后修改时间
- 最后成功提交的行号
- 成功条数
- 失败条数
- 更新时间

关键约束：
- 只有当文件路径、大小、修改时间匹配时，历史 checkpoint 才能复用。
- 手工指定 `startLine` 时，以人工参数优先。

### 5.7 `BadRowWriter`

目的：输出坏数据和错误日志，保证失败可追踪。

建议输出：
- `*.bad.csv`：保留原始问题行
- `*.bad.log`：记录行号、表名、异常类型、异常信息

## 6. 执行流程设计

单次执行仅处理一个“目标表 + CSV 文件”组合，整体流程如下：

1. 解析命令行参数。
2. 加载目标表定义。
3. 读取并校验 checkpoint。
4. 打开 CSV，跳过表头，定位到起始行。
5. 逐行读取并做基础清洗。
6. 对字段数、字段值和类型进行最小必要校验。
7. 合格数据进入当前 batch。
8. 到达 `batchSize` 后执行一次批量写入和事务提交。
9. 提交成功后刷新 checkpoint。
10. 如果批量写入失败，则降级为该批逐行重试。
11. 文件结束后提交剩余不足一批的数据。
12. 输出最终统计结果。

这里的关键语义是：
- 逐行读取
- 分批提交
- checkpoint 只记录已经成功提交的数据

## 7. 数据转换与校验规则

本工具只做最小必要的数据层转换，不做业务规则清洗。

### 7.1 空值规则

以下情况统一转换为数据库 `NULL`：
- 空字符串
- 全空白字符串
- 字面量 `null`
- 字面量 `NULL`

### 7.2 类型规则

按目标列 JDBC 类型绑定参数：
- 字符型：保留原始文本
- 整型/数值型：做基础数值解析，非法值记为坏数据
- 日期时间型：按预设格式解析，解析失败记为坏数据
- 其他类型：默认按字符串兜底

### 7.3 列数规则

若某行字段数与注册表定义不一致，则该行直接记入坏数据文件，不进入 batch。

### 7.4 主键与重复规则

如果数据库返回主键冲突、唯一约束冲突等不可恢复错误，则该行记为坏数据，不自动重试。

## 8. 错误处理与重试策略

### 8.1 可恢复错误

以下错误可对当前 batch 做有限次重试：
- 瞬时网络抖动
- 连接超时
- 可重连的 JDBC 异常

重试策略：
- 默认限制最大重试次数
- 超过次数后终止当前任务并保留 checkpoint

### 8.2 不可恢复错误

以下错误不做批次自动重试：
- 字段数不匹配
- 类型转换失败
- 字段超长
- 主键或唯一约束冲突
- 明确的 SQL 语义错误

处理策略：
- 单行隔离
- 写入坏数据文件和坏数据日志
- 不中断整个导入任务

### 8.3 批失败降级策略

若某一批执行失败，则自动进入“批内逐行重试”模式：
- 逐行执行单条插入
- 成功行继续保留
- 失败行写入坏数据输出

这样可以避免因一条坏数据导致整批有效数据丢失。

## 9. 运行与操作设计

### 9.1 推荐导入顺序

建议默认按以下顺序执行：

1. `pub_restriction`
2. `segm_addr_type`
3. `spc_region`
4. `spc_regional_company`
5. `spc_station`
6. `staff`
7. `ADDR_SEGM`
8. `ADDR_SET_SEGM`

这样可以先用小表验证工具链路与连接稳定性，再处理大表。

### 9.2 运行方式

建议支持以下两种方式：

1. 开发期：`mvn exec:java`
2. 正式执行：`mvn package` 后使用 `java -jar`

建议命令行形态：

```bash
java -jar address-csv-loader.jar \
  --jdbc-url="jdbc:mysql://82.156.6.111:4000/ftth_cloud_address?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true" \
  --username="address" \
  --password="***" \
  --table="ADDR_SEGM" \
  --csv="/Users/criswu/Desktop/广电/历史数据/标准地址模块/数据样例/标准地址/tmp_addr_segm_nj_20260317.CSV" \
  --charset="GB18030" \
  --batch-size=1000 \
  --checkpoint-dir="./runtime/checkpoints" \
  --error-dir="./runtime/errors"
```

### 9.3 可观测性要求

运行期间至少输出以下信息：
- 当前目标表
- 当前文件路径
- 当前处理行号
- 已成功条数
- 已失败条数
- 最近一次提交耗时

## 10. 测试策略

该工具实现时建议覆盖以下测试：

### 10.1 CSV 解析测试

验证场景包括：
- 普通字段
- 含逗号字段
- 含双引号字段
- 空字段
- `gb18030` 编码内容

### 10.2 Checkpoint 测试

验证场景包括：
- 首次执行创建 checkpoint
- 断点续跑从上次成功行继续
- 文件变更后拒绝复用旧 checkpoint

### 10.3 Batch 降级测试

验证场景包括：
- 正常批量提交
- 批量失败后逐行重试
- 坏数据行正确写入输出文件

### 10.4 类型转换测试

验证场景包括：
- 数值转换成功
- 日期转换成功
- 非法值隔离为坏数据

## 11. 风险与边界

### 11.1 已知风险

- TiDB 远程连接稳定性可能影响长时间导入任务。
- 某些历史字段真实脏数据分布未知，可能导致坏数据数量高于预期。
- 若线上表约束与当前重建 DDL 有差异，可能在导入时暴露额外 SQL 异常。

### 11.2 边界声明

- 本工具仅以当前已整理的 8 张表为首批导入目标。
- 本工具不会自动修复数据库结构问题。
- 本工具不会自动做数据去重、编码标准化或业务规则修正。

## 12. 实施前置结论

本设计的最终结论如下：

- 采用独立 Maven CLI + 纯 JDBC 的实现方案。
- 工具放置于 `tmp/history-inventory` 下，避免侵入 `ruoyi-address` 业务模块。
- 导入模式采用“逐行读取 + 分批提交 + checkpoint 续跑 + 坏数据隔离”。
- 首批目标范围固定为 8 张表，其中标准地址与安装地址分别使用 `ADDR_SEGM`、`ADDR_SET_SEGM`。
- 后续实现重点优先放在稳定性、可恢复性、坏数据定位和可操作性，而不是追求服务化或通用平台化。
