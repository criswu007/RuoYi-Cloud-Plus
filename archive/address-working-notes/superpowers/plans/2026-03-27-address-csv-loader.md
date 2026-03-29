# Address CSV Loader Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个可在本地运行的独立 Maven CLI 工具，将标准地址模块的历史 CSV 以“逐行读取、分批提交、断点续跑、坏数据隔离”的方式导入远端 TiDB。

**Architecture:** 工具放在 `tmp/history-inventory/address-csv-loader` 下，使用 Java 17、纯 JDBC 和流式 CSV 解析，不依赖 `ruoyi-address` 服务启动。导入链路按 `LoaderOptions -> TableSpecRegistry -> CsvStreamingReader -> JdbcBatchWriter -> CheckpointStore/BadRowWriter` 组织，通过小而专一的类把命令行解析、表结构映射、CSV 解码、JDBC 写入、进度恢复分开。

**Tech Stack:** Java 17, Maven, JUnit 5, Apache Commons CSV, MySQL Connector/J, H2(test), SLF4J Simple

---

## 文件结构

### 计划创建的文件

- `tmp/history-inventory/address-csv-loader/pom.xml`
  目的：定义独立 CLI 工具的构建、依赖和可执行主类。
- `tmp/history-inventory/address-csv-loader/README.md`
  目的：说明编译、运行、断点续跑和坏数据输出目录。
- `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/AddressCsvLoaderApplication.java`
  目的：程序入口，串联参数解析、导入流程和进度输出。
- `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/LoaderOptions.java`
  目的：承载并校验命令行参数。
- `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/spec/TableSpec.java`
  目的：描述单张目标表的列定义和插入 SQL。
- `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/spec/TableSpecRegistry.java`
  目的：注册 8 张目标表的列顺序、JDBC 类型和默认文件名。
- `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/csv/CsvRow.java`
  目的：承载单行 CSV 的物理行号和字段值。
- `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/csv/CsvStreamingReader.java`
  目的：按 `gb18030` 流式读取 CSV，并产出 `CsvRow`。
- `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/io/CheckpointRecord.java`
  目的：承载 checkpoint 文件内容。
- `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/io/CheckpointStore.java`
  目的：读写 checkpoint，并校验文件元数据。
- `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/io/BadRowWriter.java`
  目的：输出 `*.bad.csv` 与 `*.bad.log`。
- `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/jdbc/JdbcValueConverter.java`
  目的：按 JDBC 类型做空值、数值、日期和字符串转换。
- `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/jdbc/BatchWriteResult.java`
  目的：承载单批写入结果。
- `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/jdbc/JdbcBatchWriter.java`
  目的：执行批量插入、事务提交、批失败逐行降级和有限重试。
- `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/LoaderOptionsTest.java`
  目的：验证命令行参数解析和缺省校验。
- `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/spec/TableSpecRegistryTest.java`
  目的：验证表定义完整性和 SQL 生成。
- `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/io/CheckpointStoreTest.java`
  目的：验证 checkpoint 保存、恢复和失效判定。
- `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/io/BadRowWriterTest.java`
  目的：验证坏数据输出格式。
- `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/csv/CsvStreamingReaderTest.java`
  目的：验证 `gb18030`、引号、逗号和空值读取。
- `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/jdbc/JdbcValueConverterTest.java`
  目的：验证类型转换和空值归一化。
- `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/jdbc/JdbcBatchWriterTest.java`
  目的：验证批量提交、主键冲突降级和坏数据隔离。
- `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/AddressCsvLoaderApplicationTest.java`
  目的：验证应用级串联逻辑和断点续跑。

### 计划不修改的文件

- `ruoyi-modules/ruoyi-address/**`
  原因：本次工具为独立 CLI，不侵入业务模块。
- `tmp/history-inventory/ftth_cloud_address_rebuild_full_ddl.sql`
  原因：作为表定义事实来源，只读不改。
- `tmp/history-inventory/ftth_cloud_address_csv_load_templates.sql`
  原因：作为目标表与列顺序参考，只读不改。

## 实施任务

### Task 1: 搭建独立 Maven CLI 骨架

**Files:**
- Create: `tmp/history-inventory/address-csv-loader/pom.xml`
- Create: `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/AddressCsvLoaderApplication.java`
- Create: `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/LoaderOptions.java`
- Test: `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/LoaderOptionsTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void parsesRequiredOptions() {
    LoaderOptions options = LoaderOptions.parse(new String[] {
        "--jdbc-url=jdbc:mysql://example/db",
        "--username=user",
        "--password=pwd",
        "--table=ADDR_SEGM",
        "--csv=/tmp/input.csv"
    });

    assertEquals("ADDR_SEGM", options.table());
    assertEquals(Path.of("/tmp/input.csv"), options.csvPath());
    assertEquals("GB18030", options.charset().name());
    assertEquals(1000, options.batchSize());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -f /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/address-csv-loader/pom.xml -Dtest=LoaderOptionsTest test`
Expected: FAIL，提示 `LoaderOptions` 或 `pom.xml` 尚不存在。

- [ ] **Step 3: Write minimal implementation**

```java
public record LoaderOptions(
    String jdbcUrl,
    String username,
    String password,
    String table,
    Path csvPath,
    Charset charset,
    int batchSize,
    Path checkpointDir,
    Path errorDir,
    long startLine,
    int maxRetryTimes,
    long logEveryRows
) {
    public static LoaderOptions parse(String[] args) {
        Map<String, String> values = new HashMap<>();
        for (String arg : args) {
            String[] parts = arg.split("=", 2);
            values.put(parts[0], parts.length > 1 ? parts[1] : "");
        }
        return new LoaderOptions(
            require(values, "--jdbc-url"),
            require(values, "--username"),
            require(values, "--password"),
            require(values, "--table"),
            Path.of(require(values, "--csv")),
            Charset.forName(values.getOrDefault("--charset", "GB18030")),
            Integer.parseInt(values.getOrDefault("--batch-size", "1000")),
            Path.of(values.getOrDefault("--checkpoint-dir", "runtime/checkpoints")),
            Path.of(values.getOrDefault("--error-dir", "runtime/errors")),
            Long.parseLong(values.getOrDefault("--start-line", "2")),
            Integer.parseInt(values.getOrDefault("--max-retry-times", "3")),
            Long.parseLong(values.getOrDefault("--log-every-rows", "10000"))
        );
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -f /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/address-csv-loader/pom.xml -Dtest=LoaderOptionsTest test`
Expected: PASS，`LoaderOptionsTest` 通过。

- [ ] **Step 5: Commit**

```bash
git -C /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus add \
  tmp/history-inventory/address-csv-loader/pom.xml \
  tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/AddressCsvLoaderApplication.java \
  tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/LoaderOptions.java \
  tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/LoaderOptionsTest.java
git -C /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus commit -m "feat: bootstrap address csv loader cli"
```

### Task 2: 固化目标表定义与插入 SQL 生成

**Files:**
- Create: `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/spec/TableSpec.java`
- Create: `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/spec/TableSpecRegistry.java`
- Test: `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/spec/TableSpecRegistryTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void returnsAddrSegmSpecWithExpectedColumnCount() {
    TableSpec spec = TableSpecRegistry.get("ADDR_SEGM");

    assertEquals(82, spec.columns().size());
    assertEquals("segm_id", spec.columns().get(0).name());
    assertTrue(spec.insertSql().startsWith("INSERT INTO ADDR_SEGM"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -f /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/address-csv-loader/pom.xml -Dtest=TableSpecRegistryTest test`
Expected: FAIL，提示 `TableSpec` 或 `TableSpecRegistry` 尚不存在。

- [ ] **Step 3: Write minimal implementation**

```java
public record TableSpec(String tableName, List<ColumnSpec> columns, String defaultCsvFileName) {
    public String insertSql() {
        String columnSql = columns.stream().map(ColumnSpec::name).collect(joining(", "));
        String valueSql = columns.stream().map(it -> "?").collect(joining(", "));
        return "INSERT INTO " + tableName + " (" + columnSql + ") VALUES (" + valueSql + ")";
    }
}

public final class TableSpecRegistry {
    private static final Map<String, TableSpec> TABLES = Map.of(
        "ADDR_SEGM", new TableSpec("ADDR_SEGM", List.of(/* 82 columns */), "tmp_addr_segm_nj_20260317.CSV"),
        "ADDR_SET_SEGM", new TableSpec("ADDR_SET_SEGM", List.of(/* 20 columns */), "tmp_addr_set_segm_nj_20260317.CSV")
        // 其余 6 张表继续补齐
    );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -f /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/address-csv-loader/pom.xml -Dtest=TableSpecRegistryTest test`
Expected: PASS，至少覆盖 8 张表定义和 SQL 占位符数量。

- [ ] **Step 5: Commit**

```bash
git -C /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus add \
  tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/spec/TableSpec.java \
  tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/spec/TableSpecRegistry.java \
  tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/spec/TableSpecRegistryTest.java
git -C /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus commit -m "feat: add address loader table specs"
```

### Task 3: 实现 checkpoint 与坏数据输出

**Files:**
- Create: `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/io/CheckpointRecord.java`
- Create: `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/io/CheckpointStore.java`
- Create: `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/io/BadRowWriter.java`
- Test: `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/io/CheckpointStoreTest.java`
- Test: `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/io/BadRowWriterTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void reloadsCheckpointWhenFileMetadataMatches() throws Exception {
    CheckpointRecord record = new CheckpointRecord("ADDR_SEGM", csvPath, size, lastModified, 1002L, 1000L, 2L);
    store.save(record);

    Optional<CheckpointRecord> loaded = store.load("ADDR_SEGM", csvPath);

    assertTrue(loaded.isPresent());
    assertEquals(1002L, loaded.get().lastCommittedLine());
}

@Test
void writesBadRowCsvAndLog() throws Exception {
    badRowWriter.write(88L, "ADDR_SEGM", List.of("1", "broken"), "字段数不匹配");

    assertTrue(Files.exists(errorDir.resolve("ADDR_SEGM.bad.csv")));
    assertTrue(Files.readString(errorDir.resolve("ADDR_SEGM.bad.log")).contains("字段数不匹配"));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -f /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/address-csv-loader/pom.xml -Dtest=CheckpointStoreTest,BadRowWriterTest test`
Expected: FAIL，提示相关类不存在。

- [ ] **Step 3: Write minimal implementation**

```java
public record CheckpointRecord(
    String tableName,
    Path csvPath,
    long fileSize,
    Instant lastModifiedTime,
    long lastCommittedLine,
    long successCount,
    long failureCount
) {}

public final class CheckpointStore {
    public Optional<CheckpointRecord> load(String tableName, Path csvPath) { /* 读取 properties 并校验 size/mtime */ }
    public void save(CheckpointRecord record) { /* 写入 properties */ }
}

public final class BadRowWriter implements Closeable {
    public void write(long lineNumber, String tableName, List<String> rawValues, String errorMessage) { /* 输出 csv/log */ }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -f /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/address-csv-loader/pom.xml -Dtest=CheckpointStoreTest,BadRowWriterTest test`
Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git -C /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus add \
  tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/io/CheckpointRecord.java \
  tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/io/CheckpointStore.java \
  tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/io/BadRowWriter.java \
  tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/io/CheckpointStoreTest.java \
  tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/io/BadRowWriterTest.java
git -C /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus commit -m "feat: add checkpoint and bad row output"
```

### Task 4: 实现 CSV 流式读取与字段转换

**Files:**
- Create: `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/csv/CsvRow.java`
- Create: `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/csv/CsvStreamingReader.java`
- Create: `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/jdbc/JdbcValueConverter.java`
- Test: `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/csv/CsvStreamingReaderTest.java`
- Test: `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/jdbc/JdbcValueConverterTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void readsGb18030CsvWithQuotedComma() throws Exception {
    List<CsvRow> rows = reader.readAll();

    assertEquals(2, rows.size());
    assertEquals(2L, rows.get(0).lineNumber());
    assertEquals("南京,鼓楼", rows.get(0).values().get(1));
}

@Test
void convertsBlankAndNullLiteralToNull() {
    assertNull(converter.convert("   ", JDBCType.VARCHAR));
    assertNull(converter.convert("NULL", JDBCType.VARCHAR));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -f /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/address-csv-loader/pom.xml -Dtest=CsvStreamingReaderTest,JdbcValueConverterTest test`
Expected: FAIL，提示 `CsvStreamingReader` 或 `JdbcValueConverter` 尚不存在。

- [ ] **Step 3: Write minimal implementation**

```java
public record CsvRow(long lineNumber, List<String> values) {}

public final class CsvStreamingReader implements Closeable {
    public Optional<CsvRow> next() { /* 基于 Commons CSV 流式读取并返回物理行号 */ }
}

public final class JdbcValueConverter {
    public Object convert(String rawValue, JDBCType jdbcType) {
        String normalized = normalize(rawValue);
        if (normalized == null) return null;
        return switch (jdbcType) {
            case INTEGER, BIGINT, SMALLINT -> Long.valueOf(normalized);
            case DECIMAL, NUMERIC -> new BigDecimal(normalized);
            case TIMESTAMP, DATE -> Timestamp.valueOf(normalized);
            default -> normalized;
        };
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -f /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/address-csv-loader/pom.xml -Dtest=CsvStreamingReaderTest,JdbcValueConverterTest test`
Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git -C /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus add \
  tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/csv/CsvRow.java \
  tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/csv/CsvStreamingReader.java \
  tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/jdbc/JdbcValueConverter.java \
  tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/csv/CsvStreamingReaderTest.java \
  tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/jdbc/JdbcValueConverterTest.java
git -C /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus commit -m "feat: add csv streaming and jdbc value conversion"
```

### Task 5: 实现 JDBC 批量写入、批失败降级与应用串联

**Files:**
- Create: `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/jdbc/BatchWriteResult.java`
- Create: `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/jdbc/JdbcBatchWriter.java`
- Modify: `tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/AddressCsvLoaderApplication.java`
- Test: `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/jdbc/JdbcBatchWriterTest.java`
- Test: `tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/AddressCsvLoaderApplicationTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void fallsBackToSingleRowInsertWhenBatchFails() throws Exception {
    BatchWriteResult result = writer.writeRows(spec, List.of(validRow, duplicatePkRow, anotherValidRow));

    assertEquals(2, result.successCount());
    assertEquals(1, result.failureCount());
    assertTrue(Files.readString(errorDir.resolve("ADDR_SEGM.bad.log")).contains("PRIMARY KEY"));
}

@Test
void resumesFromCheckpointLine() throws Exception {
    checkpointStore.save(new CheckpointRecord("ADDR_SEGM", csvPath, size, lastModified, 3L, 2L, 0L));

    int exitCode = AddressCsvLoaderApplication.run(options, dataSourceFactory);

    assertEquals(0, exitCode);
    assertEquals(remainingRowCount, queryInsertedCount());
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -f /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/address-csv-loader/pom.xml -Dtest=JdbcBatchWriterTest,AddressCsvLoaderApplicationTest test`
Expected: FAIL，提示 `JdbcBatchWriter` 尚不存在或应用入口未接线。

- [ ] **Step 3: Write minimal implementation**

```java
public record BatchWriteResult(long successCount, long failureCount, long lastCommittedLine) {}

public final class JdbcBatchWriter {
    public BatchWriteResult writeRows(TableSpec spec, List<CsvRow> rows) throws SQLException {
        try {
            return executeBatch(spec, rows);
        } catch (SQLException batchFailure) {
            return executeOneByOne(spec, rows, batchFailure);
        }
    }
}

public final class AddressCsvLoaderApplication {
    public static int run(LoaderOptions options) {
        TableSpec spec = TableSpecRegistry.get(options.table());
        // 读取 checkpoint -> 流式读 CSV -> 到批次阈值写入 -> 成功后刷新 checkpoint
        return 0;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -f /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/address-csv-loader/pom.xml -Dtest=JdbcBatchWriterTest,AddressCsvLoaderApplicationTest test`
Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git -C /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus add \
  tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/jdbc/BatchWriteResult.java \
  tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/jdbc/JdbcBatchWriter.java \
  tmp/history-inventory/address-csv-loader/src/main/java/org/dromara/historyloader/AddressCsvLoaderApplication.java \
  tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/jdbc/JdbcBatchWriterTest.java \
  tmp/history-inventory/address-csv-loader/src/test/java/org/dromara/historyloader/AddressCsvLoaderApplicationTest.java
git -C /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus commit -m "feat: complete address csv loader execution flow"
```

### Task 6: 补齐 README 与整体验证

**Files:**
- Create: `tmp/history-inventory/address-csv-loader/README.md`
- Modify: `tmp/history-inventory/address-csv-loader/pom.xml`

- [ ] **Step 1: Write the failing test**

```java
@Test
void packageBuildCreatesRunnableJar() {
    // 该步骤以 Maven 打包验证代替单元测试
}
```

- [ ] **Step 2: Run verification to confirm current build is incomplete**

Run: `mvn -f /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/address-csv-loader/pom.xml clean test package`
Expected: FAIL 或缺少可执行 `jar` 配置、README 尚未补齐。

- [ ] **Step 3: Write minimal implementation**

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-jar-plugin</artifactId>
  <configuration>
    <archive>
      <manifest>
        <mainClass>org.dromara.historyloader.AddressCsvLoaderApplication</mainClass>
      </manifest>
    </archive>
  </configuration>
</plugin>
```

README 需至少写明：
- 编译命令
- `java -jar` 示例
- 8 张表推荐导入顺序
- checkpoint 与坏数据目录说明
- 常见失败场景处理建议

- [ ] **Step 4: Run verification to confirm final build and tests pass**

Run: `mvn -f /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/address-csv-loader/pom.xml clean test package`
Expected: PASS，测试通过并产出可执行包。

- [ ] **Step 5: Commit**

```bash
git -C /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus add \
  tmp/history-inventory/address-csv-loader/pom.xml \
  tmp/history-inventory/address-csv-loader/README.md
git -C /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus commit -m "docs: add address csv loader usage guide"
```

## 最终验证清单

- [ ] 运行 `mvn -f /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/tmp/history-inventory/address-csv-loader/pom.xml clean test package`
- [ ] 确认 `LoaderOptionsTest`、`TableSpecRegistryTest`、`CheckpointStoreTest`、`BadRowWriterTest`、`CsvStreamingReaderTest`、`JdbcValueConverterTest`、`JdbcBatchWriterTest`、`AddressCsvLoaderApplicationTest` 全部通过
- [ ] 确认可执行包生成成功
- [ ] 确认 README 覆盖编译、运行、续跑和坏数据处理说明
- [ ] 如可行，使用小样例 CSV 做一次本地 H2 或小批量 dry-run 验证

## 执行备注

- 本次实现必须遵守 @superpowers:test-driven-development：每个生产类先写失败测试，再写最小实现。
- 进入实现前优先使用 @superpowers:using-git-worktrees；若当前仓库没有既定 worktree 目录，需要先确定工作区位置。
- 完成后必须遵守 @superpowers:verification-before-completion，基于新鲜命令输出汇报结果。
