/*
 * 线上库字段注释盘点脚本（JShell 形式）
 *
 * 约束：
 * - 仅做“事实采集”，不输出结论性中文报告。
 * - 通过 ADDRESS_DB_URL / ADDRESS_DB_USERNAME / ADDRESS_DB_PASSWORD 读取连接（也支持同名 System properties）。
 * - 输出固定 label，便于后续摘录到证据文档。
 */

import java.sql.*;
import java.util.*;
import java.util.regex.*;

final String EXPECTED_CATALOG = "ftth_cloud_address";
final int EXPECTED_TABLE_COUNT = 8;

final String PUB_RESTRICTION_TABLE = "pub_restriction";
final String SEGM_ADDR_TYPE_TABLE = "segm_addr_type";
final String SPC_REGION_TABLE = "spc_region";
final String SPC_REGIONAL_COMPANY_TABLE = "spc_regional_company";
final String SPC_STATION_TABLE = "spc_station";
final String STAFF_TABLE = "staff";
final String TMP_ADDR_SEGM_TABLE = "tmp_addr_segm_nj_20260317";
final String TMP_ADDR_SET_SEGM_TABLE = "tmp_addr_set_segm_nj_20260317";

final List<String> CORE_TABLES = Collections.unmodifiableList(Arrays.asList(
        PUB_RESTRICTION_TABLE,
        SEGM_ADDR_TYPE_TABLE,
        SPC_REGION_TABLE,
        SPC_REGIONAL_COMPANY_TABLE,
        SPC_STATION_TABLE,
        STAFF_TABLE,
        TMP_ADDR_SEGM_TABLE,
        TMP_ADDR_SET_SEGM_TABLE
));

class ColumnMeta {
    final String tableName;
    final int ordinal;
    final String columnName;
    final String columnType;
    final String isNullable;
    final String columnKey;
    final String comment;

    ColumnMeta(String tableName, int ordinal, String columnName, String columnType, String isNullable, String columnKey, String comment) {
        this.tableName = tableName;
        this.ordinal = ordinal;
        this.columnName = columnName;
        this.columnType = columnType;
        this.isNullable = isNullable;
        this.columnKey = columnKey;
        this.comment = comment == null ? "" : comment;
    }
}

String resolveConfig(String key) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
        value = System.getProperty(key);
    }
    return (value == null || value.isBlank()) ? null : value;
}

void printLabel(String label) {
    System.out.println("## " + label);
}

void dumpResultSet(ResultSet rs) throws SQLException {
    ResultSetMetaData meta = rs.getMetaData();
    int columnCount = meta.getColumnCount();
    boolean hasRows = false;
    while (rs.next()) {
        hasRows = true;
        StringBuilder builder = new StringBuilder("  ");
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                builder.append(", ");
            }
            Object value = rs.getObject(i);
            builder.append(meta.getColumnLabel(i)).append("=").append(value == null ? "NULL" : value);
        }
        System.out.println(builder);
    }
    if (!hasRows) {
        System.out.println("  [EMPTY RESULT]");
    }
}

void executeQuery(Connection conn, String label, String sql) {
    printLabel(label);
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        dumpResultSet(rs);
    } catch (SQLException e) {
        // 任务期望正常情况下不出现该行；保留用于现场排障。
        System.out.println("  [QUERY ERROR] " + e.getMessage());
    }
}

List<String> loadAllTables(Connection conn) {
    List<String> tables = new ArrayList<>();
    String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() ORDER BY table_name";
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            tables.add(rs.getString(1));
        }
    } catch (SQLException e) {
        // 这里若失败，会连带影响后续所有盘点；仍打印但不抛出。
        printLabel("TABLE_INVENTORY");
        System.out.println("  [QUERY ERROR] " + e.getMessage());
    }
    return tables;
}

int loadTableCount(Connection conn) throws SQLException {
    String sql = "SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = DATABASE()";
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        if (rs.next()) {
            return rs.getInt("table_count");
        }
    }
    return -1;
}

Map<String, String> buildSourceHints(Connection conn, Map<String, List<ColumnMeta>> columnsByTable) {
    // SOURCE_HINTS 仅输出“当前 8 表内可直接识别的来源提示”
    Map<String, String> hints = new LinkedHashMap<>();

    // 1) region_id -> spc_region.region_id
    // 2) station_id/installstation_id/busstation_id -> spc_station.station_id
    // 3) job_id -> pub_restriction.code（仅当注释已明确）
    for (String table : CORE_TABLES) {
        List<ColumnMeta> cols = columnsByTable.getOrDefault(table, Collections.emptyList());
        for (ColumnMeta c : cols) {
            String colLower = c.columnName == null ? "" : c.columnName.toLowerCase(Locale.ROOT);
            if ("region_id".equals(colLower)) {
                // 排除 spc_region 自身的 region_id（这不是“来源提示”，而是来源表自身字段）
                if (!SPC_REGION_TABLE.equalsIgnoreCase(table)) {
                    hints.put(table + "." + c.columnName, "spc_region.region_id");
                }
            } else if ("station_id".equals(colLower) || "installstation_id".equals(colLower) || "busstation_id".equals(colLower)) {
                // 排除 spc_station 自身的 station_id
                if (!SPC_STATION_TABLE.equalsIgnoreCase(table)) {
                    hints.put(table + "." + c.columnName, "spc_station.station_id");
                }
            } else if ("job_id".equals(colLower)) {
                String commentLower = (c.comment == null ? "" : c.comment).toLowerCase(Locale.ROOT);
                // “仅当注释已明确”按最小可执行判断：出现 pub_restriction 或 “字典”且包含 pub_restriction
                boolean explicit = commentLower.contains("pub_restriction") || commentLower.contains("取pub_restriction");
                if (explicit) {
                    hints.put(table + "." + c.columnName, "pub_restriction.code");
                }
            }
        }
    }
    return hints;
}

boolean isIdLikeName(String columnName) {
    if (columnName == null) {
        return false;
    }
    String n = columnName.toLowerCase(Locale.ROOT);
    return n.endsWith("_id")
            || n.endsWith("_name")
            || n.endsWith("_no")
            || n.endsWith("_code")
            || "id".equals(n)
            || "name".equals(n)
            || "no".equals(n)
            || "code".equals(n);
}

boolean hasExplicitEnumHintInComment(String comment) {
    if (comment == null) {
        return false;
    }
    String c = comment.trim();
    if (c.isEmpty()) {
        return false;
    }
    String lowerComment = c.toLowerCase(Locale.ROOT);

    // A. 显式映射提示：例如 "0:xxx" / "0：xxx" / "2017101维修,2017102安装"
    if (Pattern.compile("\\b\\d+\\s*[:：]").matcher(c).find()) {
        return true;
    }
    Matcher m = Pattern.compile("\\b\\d{2,}\\s*[^0-9\\s,，;；]+").matcher(c);
    int hits = 0;
    while (m.find()) {
        hits++;
        if (hits >= 2) {
            return true;
        }
    }

    // B. 字典/来源显式提示（固定规则）
    // - 注释直接给出来源/字典提示即纳入（任务要求：显式“来源/枚举”需纳入）
    if (lowerComment.contains("字典")) {
        return true;
    }
    if (lowerComment.contains("取") && (lowerComment.contains("表") || lowerComment.contains("对应") || lowerComment.contains("来源") || lowerComment.contains("字典"))) {
        return true;
    }

    // C. 枚举/布尔显式提示
    if (lowerComment.contains("枚举") || lowerComment.contains("布尔") || lowerComment.contains("boolean") || lowerComment.contains("bool")) {
        return true;
    }
    if (lowerComment.contains("是否")) {
        return true;
    }

    // D. 状态/类型/来源/属性/能力提示：按任务要求，不再额外要求字段名匹配
    if (lowerComment.contains("状态")
            || lowerComment.contains("类型")
            || lowerComment.contains("来源")
            || lowerComment.contains("属性")
            || lowerComment.contains("能力")) {
        return true;
    }

    return false;
}

Map<String, List<ColumnMeta>> loadAllColumns(Connection conn) {
    Map<String, List<ColumnMeta>> byTable = new HashMap<>();
    String sql = """
            SELECT table_name,
                   ordinal_position,
                   column_name,
                   column_type,
                   is_nullable,
                   column_key,
                   column_comment
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
            ORDER BY table_name, ordinal_position
            """;
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            String table = rs.getString("table_name");
            int ordinal = rs.getInt("ordinal_position");
            String name = rs.getString("column_name");
            String type = rs.getString("column_type");
            String nullable = rs.getString("is_nullable");
            String key = rs.getString("column_key");
            String comment = rs.getString("column_comment");
            byTable.computeIfAbsent(table, t -> new ArrayList<>()).add(new ColumnMeta(table, ordinal, name, type, nullable, key, comment));
        }
    } catch (SQLException e) {
        // 若这里失败，COLUMN_COMMENTS 等会受影响；但仍让脚本继续输出其它标签。
        System.out.println("[COLUMNS LOAD ERROR] " + e.getMessage());
    }
    return byTable;
}

long queryRowCount(Connection conn, String table) throws SQLException {
    String sql = "SELECT COUNT(*) AS row_count FROM " + table;
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        if (rs.next()) {
            return rs.getLong("row_count");
        }
    }
    return -1L;
}

int queryDistinctNonNullCount(Connection conn, String table, String column) throws SQLException {
    String sql = "SELECT COUNT(DISTINCT " + column + ") AS distinct_nonnull FROM " + table + " WHERE " + column + " IS NOT NULL";
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        if (rs.next()) {
            return rs.getInt("distinct_nonnull");
        }
    }
    return -1;
}

class EnumCandidate {
    final ColumnMeta col;
    final String selectionRule; // COMMENT_HINT | SOURCE_HINT | LOW_CARDINALITY
    final String valueSource;   // 字段注释 | SOURCE_HINTS | 当前表实际值域 | 当前 8 表内未识别
    final String sourceHint;    // e.g. spc_region.region_id
    final Integer distinctNonNull; // may be null

    EnumCandidate(ColumnMeta col, String selectionRule, String valueSource, String sourceHint, Integer distinctNonNull) {
        this.col = col;
        this.selectionRule = selectionRule;
        this.valueSource = valueSource;
        this.sourceHint = sourceHint;
        this.distinctNonNull = distinctNonNull;
    }
}

List<EnumCandidate> buildEnumCandidates(Connection conn,
                                        Map<String, List<ColumnMeta>> columnsByTable,
                                        Map<String, String> sourceHints) {
    // 固定规则：
    // - 字段注释包含显式枚举/布尔/状态/类型/来源/属性/能力提示时，必须纳入
    // - 命中 SOURCE_HINTS 且来源为字典/依赖表的字段，必须纳入
    // - 满足“非空 distinct 值 <= 20”的低值域字段，且不属于明显主数据标识字段时，可以纳入
    List<EnumCandidate> candidates = new ArrayList<>();

    for (Map.Entry<String, List<ColumnMeta>> e : columnsByTable.entrySet()) {
        String table = e.getKey();
        List<ColumnMeta> cols = e.getValue();
        for (ColumnMeta c : cols) {
            String key = c.tableName + "." + c.columnName;
            String hint = sourceHints.get(key);

            boolean commentHint = hasExplicitEnumHintInComment(c.comment);
            boolean sourceHint = hint != null;

            // 取值来源判定优先级固定为：
            // 1) 字段注释直接给出来源或枚举
            // 2) SOURCE_HINTS 明确识别到当前 8 表内依赖表
            // 3) 当前表实际值域满足“非空 distinct 值 <= 20，且字段名不匹配 _id/_name/_no/_code（除非注释已明确为枚举）” -> 当前表实际值域
            // 4) 以上均不满足 -> 当前 8 表内未识别（此处输出为 UNRECOGNIZED）
            String valueSource;
            if (commentHint) {
                valueSource = "字段注释";
            } else if (sourceHint) {
                valueSource = "SOURCE_HINTS";
            } else {
                valueSource = "当前 8 表内未识别";
            }

            if (commentHint) {
                candidates.add(new EnumCandidate(c, "COMMENT_HINT", valueSource, hint, null));
                continue;
            }
            if (sourceHint) {
                candidates.add(new EnumCandidate(c, "SOURCE_HINT", valueSource, hint, null));
                continue;
            }

            // 低值域候选：严格按规则判定
            // - 非空 distinct 值 <= 20
            // - 且不属于明显主数据标识字段（_id/_name/_no/_code 等）
            if (!isIdLikeName(c.columnName)) {
                Integer distinct = null;
                try {
                    distinct = queryDistinctNonNullCount(conn, c.tableName, c.columnName);
                } catch (SQLException ex) {
                    // 低值域判断失败不应阻断脚本；保持候选不纳入即可。
                    distinct = null;
                }
                if (distinct != null && distinct >= 0 && distinct <= 20) {
                    candidates.add(new EnumCandidate(c, "LOW_CARDINALITY", "当前表实际值域", null, distinct));
                }
            }
        }
    }

    candidates.sort(Comparator
            .comparing((EnumCandidate x) -> x.col.tableName)
            .thenComparingInt(x -> x.col.ordinal));
    return candidates;
}

void printTableInventory(List<String> tables) {
    printLabel("TABLE_INVENTORY");
    if (tables.isEmpty()) {
        System.out.println("  [EMPTY RESULT]");
        return;
    }
    for (String t : tables) {
        System.out.println("  table_name=" + t);
    }
}

void printTableCountAssert(int tableCount) {
    printLabel("TABLE_COUNT_ASSERT");
    System.out.println("  table_count=" + tableCount + ", assert_eight_tables=" + (tableCount == EXPECTED_TABLE_COUNT));
}

void printTableComments(Connection conn, List<String> tables) {
    printLabel("TABLE_COMMENTS");
    if (tables.isEmpty()) {
        System.out.println("  [EMPTY RESULT]");
        return;
    }
    for (String t : tables) {
        String comment = "";
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT table_comment FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?")) {
            ps.setString(1, t);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    comment = rs.getString(1);
                    if (comment == null) {
                        comment = "";
                    }
                }
            }
        } catch (SQLException e) {
            comment = "";
        }

        long rowCount = -1L;
        try {
            rowCount = queryRowCount(conn, t);
        } catch (SQLException e) {
            rowCount = -1L;
        }
        System.out.println("  table_name=" + t + ", table_comment=" + (comment.isBlank() ? "NULL" : comment) + ", row_count=" + rowCount);
    }
}

void printColumnComments(Map<String, List<ColumnMeta>> columnsByTable) {
    printLabel("COLUMN_COMMENTS");
    List<String> tables = new ArrayList<>(columnsByTable.keySet());
    tables.sort(String::compareTo);
    boolean any = false;
    for (String t : tables) {
        List<ColumnMeta> cols = columnsByTable.getOrDefault(t, Collections.emptyList());
        for (ColumnMeta c : cols) {
            any = true;
            String comment = c.comment == null || c.comment.isBlank() ? "NULL" : c.comment;
            System.out.println("  table_name=" + c.tableName
                    + ", ordinal=" + c.ordinal
                    + ", column_name=" + c.columnName
                    + ", column_type=" + c.columnType
                    + ", nullable=" + c.isNullable
                    + ", key=" + (c.columnKey == null || c.columnKey.isBlank() ? "NULL" : c.columnKey)
                    + ", comment=" + comment);
        }
    }
    if (!any) {
        System.out.println("  [EMPTY RESULT]");
    }
}

void printCommentCoverage(Map<String, List<ColumnMeta>> columnsByTable) {
    printLabel("COMMENT_COVERAGE");
    List<String> tables = new ArrayList<>(columnsByTable.keySet());
    tables.sort(String::compareTo);
    if (tables.isEmpty()) {
        System.out.println("  [EMPTY RESULT]");
        return;
    }
    for (String t : tables) {
        List<ColumnMeta> cols = columnsByTable.getOrDefault(t, Collections.emptyList());
        int total = cols.size();
        int withComment = 0;
        for (ColumnMeta c : cols) {
            if (c.comment != null && !c.comment.isBlank()) {
                withComment++;
            }
        }
        System.out.println("  table_name=" + t + ", column_total=" + total + ", column_with_comment=" + withComment);
    }
}

void printSourceHints(Map<String, String> sourceHints) {
    printLabel("SOURCE_HINTS");
    if (sourceHints.isEmpty()) {
        System.out.println("  [EMPTY RESULT]");
        return;
    }
    for (Map.Entry<String, String> e : sourceHints.entrySet()) {
        // 输出示例：region_id -> spc_region.region_id（按表名可追溯）
        System.out.println("  hint=" + e.getKey() + " -> " + e.getValue());
    }
}

void printEnumCandidates(List<EnumCandidate> candidates) {
    printLabel("ENUM_CANDIDATES");
    if (candidates.isEmpty()) {
        System.out.println("  [EMPTY RESULT]");
        return;
    }
    Map<String, List<EnumCandidate>> byTable = new LinkedHashMap<>();
    for (EnumCandidate c : candidates) {
        byTable.computeIfAbsent(c.col.tableName, t -> new ArrayList<>()).add(c);
    }
    for (Map.Entry<String, List<EnumCandidate>> e : byTable.entrySet()) {
        String table = e.getKey();
        List<String> items = new ArrayList<>();
        for (EnumCandidate c : e.getValue()) {
            String comment = c.col.comment == null || c.col.comment.isBlank() ? "NULL" : c.col.comment;
            items.add("ordinal=" + c.col.ordinal
                    + ", column_name=" + c.col.columnName
                    + ", column_type=" + c.col.columnType
                    + ", nullable=" + c.col.isNullable
                    + ", key=" + (c.col.columnKey == null || c.col.columnKey.isBlank() ? "NULL" : c.col.columnKey)
                    + ", comment=" + comment
                    + ", selection_rule=" + c.selectionRule
                    + ", value_source=" + c.valueSource
                    + ", source_hint=" + (c.sourceHint == null ? "NULL" : c.sourceHint)
                    + ", distinct_nonnull=" + (c.distinctNonNull == null ? "NULL" : c.distinctNonNull));
        }
        System.out.println("  table_name=" + table + ", candidates=" + String.join(" ; ", items));
    }
}

void printEnumValueDistribution(Connection conn, List<EnumCandidate> candidates) {
    printLabel("ENUM_VALUE_DISTRIBUTION");
    if (candidates.isEmpty()) {
        System.out.println("  [EMPTY RESULT]");
        return;
    }

    // 为保证标签在 sed 截断范围内可见，分布以“每候选字段一行”输出；
    // 但不对结果做截断，保留完整 distinct 值与数量（任务要求）。
    Map<String, List<EnumCandidate>> byTable = new LinkedHashMap<>();
    for (EnumCandidate c : candidates) {
        byTable.computeIfAbsent(c.col.tableName, t -> new ArrayList<>()).add(c);
    }
    for (Map.Entry<String, List<EnumCandidate>> e : byTable.entrySet()) {
        String table = e.getKey();
        List<String> cols = new ArrayList<>();
        for (EnumCandidate c : e.getValue()) {
            String col = c.col.columnName;

            int distinctNonNull = -1;
            try {
                distinctNonNull = queryDistinctNonNullCount(conn, table, col);
            } catch (SQLException ex) {
                cols.add("column_name=" + col + ", [QUERY ERROR] " + ex.getMessage());
                continue;
            }

            String sql = "SELECT " + col + " AS value, COUNT(*) AS total FROM " + table
                    + " WHERE " + col + " IS NOT NULL GROUP BY " + col
                    + " ORDER BY total DESC, value ASC";
            List<String> pairs = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Object v = rs.getObject("value");
                    long total = rs.getLong("total");
                    String vs = v == null ? "NULL" : String.valueOf(v);
                    pairs.add(vs + ":" + total);
                }
            } catch (SQLException ex) {
                cols.add("column_name=" + col + ", [QUERY ERROR] " + ex.getMessage());
                continue;
            }

            cols.add("column_name=" + col
                    + ", distinct_nonnull=" + distinctNonNull
                    + ", values=" + String.join(" | ", pairs));
        }
        System.out.println("  table_name=" + table + ", distributions=" + String.join(" ; ", cols));
    }
}

void printCommentCoverageSummary(Connection conn, List<String> tables, Map<String, List<ColumnMeta>> columnsByTable) {
    // 该 label 要求的是“每表字段总数与有注释字段数”，已在 printCommentCoverage 实现。
    // 这里保留方法签名以便未来扩展，不输出额外 label。
}

List<String> missingConfigs = new ArrayList<>();
String dbUrl = resolveConfig("ADDRESS_DB_URL");
String dbUser = resolveConfig("ADDRESS_DB_USERNAME");
String dbPassword = resolveConfig("ADDRESS_DB_PASSWORD");
if (dbUrl == null) {
    missingConfigs.add("ADDRESS_DB_URL");
}
if (dbUser == null) {
    missingConfigs.add("ADDRESS_DB_USERNAME");
}
if (dbPassword == null) {
    missingConfigs.add("ADDRESS_DB_PASSWORD");
}

if (!missingConfigs.isEmpty()) {
    System.out.println("[CONFIG ERROR] 缺失数据库连接配置: " + String.join(", ", missingConfigs));
} else {
    Properties props = new Properties();
    props.setProperty("user", dbUser);
    props.setProperty("password", dbPassword);
    try (Connection conn = DriverManager.getConnection(dbUrl, props)) {
        System.out.println("## CONNECTED");
        String catalog = conn.getCatalog();
        System.out.println("Current catalog: " + catalog);

        List<String> allTables = loadAllTables(conn);
        printTableInventory(allTables);

        int tableCount;
        try {
            tableCount = loadTableCount(conn);
        } catch (SQLException e) {
            tableCount = -1;
        }
        printTableCountAssert(tableCount);

        printTableComments(conn, allTables);

        Map<String, List<ColumnMeta>> columnsByTable = loadAllColumns(conn);
        printCommentCoverage(columnsByTable);

        Map<String, String> sourceHints = buildSourceHints(conn, columnsByTable);
        printSourceHints(sourceHints);

        List<EnumCandidate> enumCandidates = buildEnumCandidates(conn, columnsByTable, sourceHints);
        printEnumCandidates(enumCandidates);
        printEnumValueDistribution(conn, enumCandidates);

        // 放在最后，避免 COLUMN_COMMENTS 的大量输出把其它固定 label 推出 sed 截取范围。
        printColumnComments(columnsByTable);
    } catch (SQLException e) {
        System.out.println("[CONNECT ERROR] " + e.getMessage());
    }
}
