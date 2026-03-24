/*
 * 南京标准地址核心盘点脚本（JShell 形式）
 * 目的：连接南京历史地址库，按固定 label 分类输出实际存在表的结构、行数、空值、重复、关联与字典分布；
 *       去除明文凭据，通过环境变量/系统属性补充连接配置，并确保标签后缀保持 <subject>_<metric> 风格。
 */

import java.sql.*;
import java.util.*;

final String TMP_ADDR_SEGM_TABLE = "tmp_addr_segm_nj_20260317";
final String TMP_ADDR_SET_SEGM_TABLE = "tmp_addr_set_segm_nj_20260317";
final String SPC_STATION_TABLE = "spc_station";
final String SPC_REGION_TABLE = "spc_region";
final String SEGM_ADDR_TYPE_TABLE = "segm_addr_type";
final String PUB_RESTRICTION_TABLE = "pub_restriction";
final String SPC_REGIONAL_COMPANY_TABLE = "spc_regional_company";
final String STAFF_TABLE = "staff";

final List<String> CORE_TABLES = Collections.unmodifiableList(Arrays.asList(
        TMP_ADDR_SEGM_TABLE,
        TMP_ADDR_SET_SEGM_TABLE,
        SPC_STATION_TABLE,
        SPC_REGION_TABLE,
        SEGM_ADDR_TYPE_TABLE,
        PUB_RESTRICTION_TABLE,
        SPC_REGIONAL_COMPANY_TABLE,
        STAFF_TABLE
));

class QueryDef {
    final String label;
    final String sql;
    final List<String> requiredTables;

    QueryDef(String label, String sql, String... requiredTables) {
        this.label = label;
        this.sql = sql;
        this.requiredTables = requiredTables.length == 0 ? Collections.emptyList() : Arrays.asList(requiredTables);
    }
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

boolean tableExists(Connection conn, String table) {
    try {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(conn.getCatalog(), null, table, new String[]{"TABLE"})) {
            return rs.next();
        }
    } catch (SQLException e) {
        System.out.println("[METADATA ERROR:" + table + "] " + e.getMessage());
        return false;
    }
}

void executeQuery(Connection conn, QueryDef query) {
    System.out.println("## " + query.label);
    for (String table : query.requiredTables) {
        if (!tableExists(conn, table)) {
            System.out.println("  [TABLE MISSING: " + table + "]");
            return;
        }
    }
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(query.sql)) {
        dumpResultSet(rs);
    } catch (SQLException e) {
        System.out.println("  [QUERY ERROR] " + e.getMessage());
    }
}

void runTableRowCounts(Connection conn) {
    for (String table : CORE_TABLES) {
        executeQuery(conn, new QueryDef("TABLE_ROW_COUNTS:" + table + "_row_count",
                "SELECT COUNT(*) AS row_count FROM " + table, table));
    }
}

QueryDef schemaColumnsQuery(String prefix, String labelSuffix, String table) {
    return new QueryDef(prefix + ":" + labelSuffix,
            "SELECT column_name, column_type, is_nullable, column_default FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = '%s' ORDER BY ordinal_position".formatted(table),
            table);
}

QueryDef groupDistributionQuery(String prefix, String labelSuffix, String table, String groupColumn, int limit) {
    String limitClause = limit > 0 ? " LIMIT " + limit : "";
    return new QueryDef(prefix + ":" + labelSuffix,
            "SELECT %s, COUNT(*) AS total FROM %s GROUP BY %s ORDER BY total DESC".formatted(groupColumn, table, groupColumn) + limitClause,
            table);
}

QueryDef duplicateTopQuery(String prefix, String labelSuffix, String table, String keyColumn, int limit) {
    String limitClause = limit > 0 ? " LIMIT " + limit : "";
    return new QueryDef(prefix + ":" + labelSuffix,
            "SELECT %s, COUNT(*) AS total FROM %s GROUP BY %s HAVING total > 1 ORDER BY total DESC".formatted(keyColumn, table, keyColumn) + limitClause,
            table);
}

QueryDef missingRelationCountQuery(String prefix, String labelSuffix,
                                   String leftTable, String rightTable,
                                   String leftAlias, String rightAlias,
                                   String joinCondition, String whereCondition) {
    String sql = "SELECT COUNT(*) AS missing FROM %s %s LEFT JOIN %s %s ON %s WHERE %s".formatted(
            leftTable, leftAlias, rightTable, rightAlias, joinCondition, whereCondition);
    return new QueryDef(prefix + ":" + labelSuffix, sql, leftTable, rightTable);
}

QueryDef missingRelationGroupQuery(String prefix, String labelSuffix,
                                   String leftTable, String rightTable,
                                   String leftAlias, String rightAlias,
                                   String joinCondition, String whereCondition,
                                   String groupColumn, int limit) {
    String limitClause = limit > 0 ? " LIMIT " + limit : "";
    String columnRef = leftAlias + "." + groupColumn;
    String sql = "SELECT %s, COUNT(*) AS total FROM %s %s LEFT JOIN %s %s ON %s WHERE %s GROUP BY %s ORDER BY total DESC".formatted(
            columnRef, leftTable, leftAlias, rightTable, rightAlias, joinCondition, whereCondition, columnRef) + limitClause;
    return new QueryDef(prefix + ":" + labelSuffix, sql, leftTable, rightTable);
}

List<QueryDef> buildQueries() {
    return Arrays.asList(
            schemaColumnsQuery("ADDR_MAIN_COLUMNS", "tmp_addr_segm_columns", TMP_ADDR_SEGM_TABLE),
            groupDistributionQuery("ADDR_MAIN_COLUMNS", "tmp_addr_segm_segm_type_distribution", TMP_ADDR_SEGM_TABLE, "segm_type", 0),
            schemaColumnsQuery("INSTALL_ADDR_MAIN_COLUMNS", "tmp_addr_set_segm_columns", TMP_ADDR_SET_SEGM_TABLE),
            groupDistributionQuery("INSTALL_ADDR_MAIN_COLUMNS", "tmp_addr_set_segm_set_type_distribution", TMP_ADDR_SET_SEGM_TABLE, "set_type", 0),
            missingRelationCountQuery("NULL_CHECKS", "tmp_addr_segm_missing_parent_count",
                    TMP_ADDR_SEGM_TABLE, TMP_ADDR_SEGM_TABLE, "t", "p",
                    "t.parent_segm_id = p.segm_id", "t.parent_segm_id IS NOT NULL AND p.segm_id IS NULL"),
            duplicateTopQuery("DUPLICATE_CHECKS", "tmp_addr_segm_duplicate_segm_id_top20", TMP_ADDR_SEGM_TABLE, "segm_id", 20),
            duplicateTopQuery("DUPLICATE_CHECKS", "tmp_addr_set_segm_duplicate_set_addr_id_top20", TMP_ADDR_SET_SEGM_TABLE, "set_addr_id", 20),
            missingRelationCountQuery("RELATION_CHECKS", "tmp_addr_set_segm_missing_standard_count",
                    TMP_ADDR_SET_SEGM_TABLE, TMP_ADDR_SEGM_TABLE, "s", "t",
                    "s.segm_id = t.segm_id", "s.segm_id IS NOT NULL AND t.segm_id IS NULL"),
            missingRelationGroupQuery("RELATION_CHECKS", "tmp_addr_segm_missing_region_link_count",
                    TMP_ADDR_SEGM_TABLE, SPC_REGION_TABLE, "t", "r",
                    "t.region_id = r.region_id", "t.region_id IS NOT NULL AND r.region_id IS NULL",
                    "region_id", 20),
            missingRelationCountQuery("RELATION_CHECKS", "tmp_addr_segm_missing_station_id_count",
                    TMP_ADDR_SEGM_TABLE, SPC_STATION_TABLE, "t", "s",
                    "t.station_id = s.station_id", "t.station_id IS NOT NULL AND s.station_id IS NULL"),
            missingRelationCountQuery("RELATION_CHECKS", "tmp_addr_segm_missing_installstation_id_count",
                    TMP_ADDR_SEGM_TABLE, SPC_STATION_TABLE, "t", "s",
                    "t.installstation_id = s.station_id", "t.installstation_id IS NOT NULL AND s.station_id IS NULL"),
            missingRelationCountQuery("RELATION_CHECKS", "tmp_addr_segm_missing_busstation_id_count",
                    TMP_ADDR_SEGM_TABLE, SPC_STATION_TABLE, "t", "s",
                    "t.busstation_id = s.station_id", "t.busstation_id IS NOT NULL AND s.station_id IS NULL"),
            groupDistributionQuery("DICTIONARY_CHECKS", "segm_addr_type_level_distribution", SEGM_ADDR_TYPE_TABLE, "level_id", 20),
            groupDistributionQuery("DICTIONARY_CHECKS", "pub_restriction_code_distribution", PUB_RESTRICTION_TABLE, "code", 20),
            groupDistributionQuery("DICTIONARY_CHECKS", "spc_regional_company_seg_type_priv_distribution", SPC_REGIONAL_COMPANY_TABLE, "segm_type_priv", 20)
    );
}

String resolveConfig(String key) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
        value = System.getProperty(key);
    }
    return (value == null || value.isBlank()) ? null : value;
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
        System.out.println("Current catalog: " + conn.getCatalog());
        runTableRowCounts(conn);
        for (QueryDef query : buildQueries()) {
            executeQuery(conn, query);
        }
    } catch (SQLException e) {
        System.out.println("[CONNECT ERROR] " + e.getMessage());
    }
}
