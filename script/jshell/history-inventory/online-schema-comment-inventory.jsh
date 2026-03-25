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
        this.comment = (comment == null || comment.isBlank()) ? null : comment;
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

String jsonEscape(String s) {
    if (s == null) {
        return null;
    }
    StringBuilder out = new StringBuilder(s.length() + 16);
    for (int i = 0; i < s.length(); i++) {
        char ch = s.charAt(i);
        switch (ch) {
            case '\\' -> out.append("\\\\");
            case '"' -> out.append("\\\"");
            case '\n' -> out.append("\\n");
            case '\r' -> out.append("\\r");
            case '\t' -> out.append("\\t");
            default -> {
                if (ch < 0x20) {
                    out.append(String.format("\\u%04x", (int) ch));
                } else {
                    out.append(ch);
                }
            }
        }
    }
    return out.toString();
}

String toJson(Object v) {
    if (v == null) {
        return "null";
    }
    if (v instanceof String s) {
        return "\"" + jsonEscape(s) + "\"";
    }
    if (v instanceof Number || v instanceof Boolean) {
        return String.valueOf(v);
    }
    if (v instanceof Map<?, ?> m) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(toJson(String.valueOf(e.getKey())));
            sb.append(":");
            sb.append(toJson(e.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }
    if (v instanceof List<?> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(toJson(item));
        }
        sb.append("]");
        return sb.toString();
    }
    return toJson(String.valueOf(v));
}

LinkedHashMap<String, Object> obj(Object... kv) {
    LinkedHashMap<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i + 1 < kv.length; i += 2) {
        m.put(String.valueOf(kv[i]), kv[i + 1]);
    }
    return m;
}

void printObj(Map<String, Object> record) {
    System.out.println(toJson(record));
}

void printError(String errorType, String message, Object... kv) {
    LinkedHashMap<String, Object> m = new LinkedHashMap<>();
    m.put("error", true);
    m.put("error_type", errorType);
    m.put("message", message);
    for (int i = 0; i + 1 < kv.length; i += 2) {
        m.put(String.valueOf(kv[i]), kv[i + 1]);
    }
    printObj(m);
}

String q(String ident) {
    if (ident == null) {
        return "``";
    }
    return "`" + ident.replace("`", "``") + "`";
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

List<String> loadAllTables(Connection conn) throws SQLException {
    List<String> tables = new ArrayList<>();
    String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() ORDER BY table_name";
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            tables.add(rs.getString(1));
        }
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

Map<String, String> buildSourceHints(Map<String, List<ColumnMeta>> columnsByTable) {
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

Map<String, List<ColumnMeta>> loadAllColumns(Connection conn) throws SQLException {
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
    }
    return byTable;
}

long queryRowCount(Connection conn, String table) throws SQLException {
    String sql = "SELECT COUNT(*) AS row_count FROM " + q(table);
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        if (rs.next()) {
            return rs.getLong("row_count");
        }
    }
    return -1L;
}

Map<String, Integer> queryDistinctNonNullCountsBatch(Connection conn, String table, List<String> columns) throws SQLException {
    Map<String, Integer> out = new LinkedHashMap<>();
    if (columns == null || columns.isEmpty()) {
        return out;
    }
    StringBuilder sb = new StringBuilder();
    Map<String, String> aliasToColumn = new LinkedHashMap<>();
    sb.append("SELECT ");
    for (int i = 0; i < columns.size(); i++) {
        String col = columns.get(i);
        String alias = "d" + i;
        if (i > 0) {
            sb.append(", ");
        }
        sb.append("COUNT(DISTINCT ").append(q(col)).append(") AS ").append(alias);
        aliasToColumn.put(alias, col);
    }
    sb.append(" FROM ").append(q(table));
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sb.toString())) {
        if (rs.next()) {
            for (Map.Entry<String, String> e : aliasToColumn.entrySet()) {
                out.put(e.getValue(), rs.getInt(e.getKey()));
            }
        }
    }
    return out;
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

Map<String, String> loadTableComments(Connection conn) throws SQLException {
    Map<String, String> out = new HashMap<>();
    String sql = "SELECT table_name, table_comment FROM information_schema.tables WHERE table_schema = DATABASE()";
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            String table = rs.getString("table_name");
            String comment = rs.getString("table_comment");
            out.put(table, (comment == null || comment.isBlank()) ? null : comment);
        }
    }
    return out;
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
    printObj(obj(
            "event", "CONFIG_ERROR",
            "missing", missingConfigs
    ));
} else {
    Properties props = new Properties();
    props.setProperty("user", dbUser);
    props.setProperty("password", dbPassword);
    try (Connection conn = DriverManager.getConnection(dbUrl, props)) {
        System.out.println("## CONNECTED");
        String actualCatalog = conn.getCatalog();
        System.out.println("Current catalog: " + actualCatalog);

        // TABLE_INVENTORY
        printLabel("TABLE_INVENTORY");
        List<String> allTables;
        boolean tableInventoryOk = true;
        try {
            allTables = loadAllTables(conn);
            printObj(obj("table_names", allTables));
        } catch (SQLException e) {
            tableInventoryOk = false;
            printError("TABLE_INVENTORY_QUERY_ERROR", e.getMessage());
            allTables = new ArrayList<>(CORE_TABLES);
            allTables.sort(String::compareTo);
            printObj(obj("fallback_used", true, "fallback_table_names", allTables));
        }

        // TABLE_COUNT_ASSERT
        printLabel("TABLE_COUNT_ASSERT");
        boolean catalogMatch = EXPECTED_CATALOG.equals(actualCatalog);
        printObj(obj(
                "expected_catalog", EXPECTED_CATALOG,
                "actual_catalog", actualCatalog,
                "assert_catalog_match", catalogMatch
        ));
        if (!catalogMatch) {
            printError("CATALOG_MISMATCH", "actual catalog does not match expected",
                    "expected_catalog", EXPECTED_CATALOG,
                    "actual_catalog", actualCatalog);
        }
        try {
            int tableCount = loadTableCount(conn);
            printObj(obj(
                    "expected_table_count", EXPECTED_TABLE_COUNT,
                    "actual_table_count", tableCount,
                    "assert_eight_tables", tableCount == EXPECTED_TABLE_COUNT
            ));
        } catch (SQLException e) {
            printError("TABLE_COUNT_QUERY_ERROR", e.getMessage());
        }

        // TABLE_COMMENTS
        printLabel("TABLE_COMMENTS");
        Map<String, String> tableComments = Collections.emptyMap();
        String tableCommentsError = null;
        try {
            tableComments = loadTableComments(conn);
        } catch (SQLException e) {
            tableCommentsError = e.getMessage();
            printError("TABLE_COMMENT_QUERY_ERROR", tableCommentsError);
        }
        if (allTables.isEmpty()) {
            printObj(obj("empty", true));
        }
        for (String t : allTables) {
            Long rowCount = null;
            String rowCountError = null;
            try {
                rowCount = queryRowCount(conn, t);
            } catch (SQLException e) {
                rowCountError = e.getMessage();
            }
            LinkedHashMap<String, Object> rec = obj(
                    "table_name", t,
                    "table_comment", tableComments.get(t),
                    "table_comment_collected", tableCommentsError == null,
                    "row_count", rowCount,
                    "row_count_collected", rowCountError == null
            );
            if (tableCommentsError != null) {
                rec.put("table_comment_error", tableCommentsError);
            }
            if (rowCountError != null) {
                rec.put("row_count_error", rowCountError);
            }
            printObj(rec);
        }

        // Load column metadata once (used by multiple labels)
        Map<String, List<ColumnMeta>> columnsByTable = Collections.emptyMap();
        String columnsError = null;
        try {
            columnsByTable = loadAllColumns(conn);
        } catch (SQLException e) {
            columnsError = e.getMessage();
        }

        // COLUMN_COMMENTS
        printLabel("COLUMN_COMMENTS");
        if (columnsError != null) {
            printError("COLUMN_METADATA_QUERY_ERROR", columnsError);
        } else {
            List<String> tableNames = new ArrayList<>(columnsByTable.keySet());
            tableNames.sort(String::compareTo);
            for (String table : tableNames) {
                List<Map<String, Object>> cols = new ArrayList<>();
                for (ColumnMeta c : columnsByTable.getOrDefault(table, Collections.emptyList())) {
                    cols.add(obj(
                            "ordinal", c.ordinal,
                            "column_name", c.columnName,
                            "column_type", c.columnType,
                            "nullable", c.isNullable,
                            "key", (c.columnKey == null || c.columnKey.isBlank()) ? null : c.columnKey,
                            "comment", c.comment
                    ));
                }
                printObj(obj(
                        "table_name", table,
                        "columns", cols
                ));
            }
        }

        // COMMENT_COVERAGE
        printLabel("COMMENT_COVERAGE");
        if (columnsError != null) {
            printError("COMMENT_COVERAGE_DEPENDS_ON_COLUMN_METADATA", columnsError);
        } else {
            List<String> tableNames = new ArrayList<>(columnsByTable.keySet());
            tableNames.sort(String::compareTo);
            for (String table : tableNames) {
                List<ColumnMeta> cols = columnsByTable.getOrDefault(table, Collections.emptyList());
                int total = cols.size();
                int withComment = 0;
                for (ColumnMeta c : cols) {
                    if (c.comment != null) {
                        withComment++;
                    }
                }
                printObj(obj(
                        "table_name", table,
                        "column_total", total,
                        "column_with_comment", withComment
                ));
            }
        }

        // SOURCE_HINTS
        printLabel("SOURCE_HINTS");
        Map<String, String> sourceHints = Collections.emptyMap();
        if (columnsError != null) {
            printError("SOURCE_HINTS_DEPENDS_ON_COLUMN_METADATA", columnsError);
        } else {
            sourceHints = buildSourceHints(columnsByTable);
            if (sourceHints.isEmpty()) {
                printObj(obj("empty", true));
            } else {
                for (Map.Entry<String, String> e : sourceHints.entrySet()) {
                    String key = e.getKey(); // table.column
                    int dot = key.indexOf('.');
                    String table = dot > 0 ? key.substring(0, dot) : null;
                    String column = dot > 0 ? key.substring(dot + 1) : key;
                    printObj(obj(
                            "table_name", table,
                            "column_name", column,
                            "source", e.getValue()
                    ));
                }
            }
        }

        // ENUM_CANDIDATES
        printLabel("ENUM_CANDIDATES");
        List<EnumCandidate> enumCandidates = new ArrayList<>();
        Map<String, String> lowCardinalityErrorsByTable = new HashMap<>();
        Map<String, Map<String, Integer>> lowCardinalityDistinctByTable = new HashMap<>();
        if (columnsError != null) {
            printError("ENUM_CANDIDATES_DEPENDS_ON_COLUMN_METADATA", columnsError);
        } else {
            List<String> tableNames = new ArrayList<>(columnsByTable.keySet());
            tableNames.sort(String::compareTo);

            // 1) 批量计算“低值域候选判定”所需 distinct 计数：每表 1 次查询（避免逐列 COUNT(DISTINCT) 放大）
            for (String table : tableNames) {
                List<String> needDistinctCols = new ArrayList<>();
                for (ColumnMeta c : columnsByTable.getOrDefault(table, Collections.emptyList())) {
                    String colKey = c.tableName + "." + c.columnName;
                    boolean commentHint = hasExplicitEnumHintInComment(c.comment);
                    boolean sourceHint = sourceHints.containsKey(colKey);
                    if (!commentHint && !sourceHint && !isIdLikeName(c.columnName)) {
                        needDistinctCols.add(c.columnName);
                    }
                }
                try {
                    lowCardinalityDistinctByTable.put(table, queryDistinctNonNullCountsBatch(conn, table, needDistinctCols));
                } catch (SQLException e) {
                    lowCardinalityErrorsByTable.put(table, e.getMessage());
                }
            }

            // 2) 生成候选（严格按计划规则，不额外收窄）
            for (String table : tableNames) {
                List<Map<String, Object>> tableCandidates = new ArrayList<>();
                Map<String, Integer> distinctMap = lowCardinalityDistinctByTable.getOrDefault(table, Collections.emptyMap());
                for (ColumnMeta c : columnsByTable.getOrDefault(table, Collections.emptyList())) {
                    String colKey = c.tableName + "." + c.columnName;
                    String hint = sourceHints.get(colKey);
                    boolean commentHint = hasExplicitEnumHintInComment(c.comment);
                    boolean sourceHint = hint != null;

                    if (commentHint) {
                        EnumCandidate cand = new EnumCandidate(c, "COMMENT_HINT", "字段注释", hint, null);
                        enumCandidates.add(cand);
                        tableCandidates.add(obj(
                                "ordinal", c.ordinal,
                                "column_name", c.columnName,
                                "column_type", c.columnType,
                                "nullable", c.isNullable,
                                "key", (c.columnKey == null || c.columnKey.isBlank()) ? null : c.columnKey,
                                "comment", c.comment,
                                "selection_rule", cand.selectionRule,
                                "value_source", cand.valueSource,
                                "source_hint", cand.sourceHint,
                                "distinct_nonnull", cand.distinctNonNull
                        ));
                        continue;
                    }
                    if (sourceHint) {
                        EnumCandidate cand = new EnumCandidate(c, "SOURCE_HINT", "SOURCE_HINTS", hint, null);
                        enumCandidates.add(cand);
                        tableCandidates.add(obj(
                                "ordinal", c.ordinal,
                                "column_name", c.columnName,
                                "column_type", c.columnType,
                                "nullable", c.isNullable,
                                "key", (c.columnKey == null || c.columnKey.isBlank()) ? null : c.columnKey,
                                "comment", c.comment,
                                "selection_rule", cand.selectionRule,
                                "value_source", cand.valueSource,
                                "source_hint", cand.sourceHint,
                                "distinct_nonnull", cand.distinctNonNull
                        ));
                        continue;
                    }

                    if (!isIdLikeName(c.columnName)) {
                        Integer distinct = distinctMap.get(c.columnName);
                        if (distinct == null) {
                            // 若批量 distinct 失败，这里不应静默吞掉；在本 label 下交由错误记录体现。
                            continue;
                        }
                        if (distinct <= 20) {
                            EnumCandidate cand = new EnumCandidate(c, "LOW_CARDINALITY", "当前表实际值域", null, distinct);
                            enumCandidates.add(cand);
                            tableCandidates.add(obj(
                                    "ordinal", c.ordinal,
                                    "column_name", c.columnName,
                                    "column_type", c.columnType,
                                    "nullable", c.isNullable,
                                    "key", (c.columnKey == null || c.columnKey.isBlank()) ? null : c.columnKey,
                                    "comment", c.comment,
                                    "selection_rule", cand.selectionRule,
                                    "value_source", cand.valueSource,
                                    "source_hint", cand.sourceHint,
                                    "distinct_nonnull", cand.distinctNonNull
                            ));
                        }
                    }
                }

                printObj(obj(
                        "table_name", table,
                        "candidate_count", tableCandidates.size(),
                        "candidates", tableCandidates
                ));
                if (lowCardinalityErrorsByTable.containsKey(table)) {
                    printError("LOW_CARDINALITY_DISTINCT_BATCH_ERROR", lowCardinalityErrorsByTable.get(table),
                            "table_name", table);
                }
            }
        }

        // ENUM_VALUE_DISTRIBUTION
        printLabel("ENUM_VALUE_DISTRIBUTION");
        if (columnsError != null) {
            printError("ENUM_VALUE_DISTRIBUTION_DEPENDS_ON_COLUMN_METADATA", columnsError);
        } else {
            enumCandidates.sort(Comparator
                    .comparing((EnumCandidate x) -> x.col.tableName)
                    .thenComparingInt(x -> x.col.ordinal));

            for (EnumCandidate cand : enumCandidates) {
                String table = cand.col.tableName;
                String col = cand.col.columnName;

                // 合并/复用：低值域候选的 distinct 来自批量查询；distinct==0 可直接输出空分布，避免额外 GROUP BY 扫描
                if ("LOW_CARDINALITY".equals(cand.selectionRule) && cand.distinctNonNull != null && cand.distinctNonNull == 0) {
                    printObj(obj(
                            "table_name", table,
                            "column_name", col,
                            "distinct_nonnull", 0,
                            "values", Collections.emptyList()
                    ));
                    continue;
                }

                String sql = "SELECT " + q(col) + " AS value, COUNT(*) AS total FROM " + q(table)
                        + " WHERE " + q(col) + " IS NOT NULL GROUP BY " + q(col)
                        + " ORDER BY total DESC, value ASC";
                List<Map<String, Object>> values = new ArrayList<>();
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        Object v = rs.getObject("value");
                        long total = rs.getLong("total");
                        values.add(obj(
                                "value", v == null ? null : String.valueOf(v),
                                "total", total
                        ));
                    }
                } catch (SQLException e) {
                    printError("ENUM_VALUE_DISTRIBUTION_QUERY_ERROR", e.getMessage(),
                            "table_name", table,
                            "column_name", col,
                            "sql", sql);
                    continue;
                }

                printObj(obj(
                        "table_name", table,
                        "column_name", col,
                        "distinct_nonnull", values.size(),
                        "values", values
                ));
            }
        }
    } catch (SQLException e) {
        printObj(obj("event", "CONNECT_ERROR", "message", e.getMessage()));
    }
}
