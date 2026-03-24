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

List<QueryDef> buildQueries() {
    return Arrays.asList(
            new QueryDef("ADDR_MAIN_COLUMNS:tmp_addr_segm_columns",
                    "SELECT column_name, column_type, is_nullable, column_default FROM information_schema.columns WHERE table_schema='ftth_cloud_address' AND table_name='%s' ORDER BY ordinal_position".formatted(TMP_ADDR_SEGM_TABLE),
                    TMP_ADDR_SEGM_TABLE),
            new QueryDef("ADDR_MAIN_COLUMNS:tmp_addr_segm_segm_type_distribution",
                    "SELECT segm_type, COUNT(*) AS total FROM %s GROUP BY segm_type ORDER BY total DESC".formatted(TMP_ADDR_SEGM_TABLE),
                    TMP_ADDR_SEGM_TABLE),
            new QueryDef("INSTALL_ADDR_MAIN_COLUMNS:tmp_addr_set_segm_columns",
                    "SELECT column_name, column_type, is_nullable, column_default FROM information_schema.columns WHERE table_schema='ftth_cloud_address' AND table_name='%s' ORDER BY ordinal_position".formatted(TMP_ADDR_SET_SEGM_TABLE),
                    TMP_ADDR_SET_SEGM_TABLE),
            new QueryDef("INSTALL_ADDR_MAIN_COLUMNS:tmp_addr_set_segm_set_type_distribution",
                    "SELECT set_type, COUNT(*) AS total FROM %s GROUP BY set_type ORDER BY total DESC".formatted(TMP_ADDR_SET_SEGM_TABLE),
                    TMP_ADDR_SET_SEGM_TABLE),
            new QueryDef("NULL_CHECKS:tmp_addr_segm_missing_parent_count",
                    "SELECT COUNT(*) AS missing_parent FROM %s t LEFT JOIN %s p ON t.parent_segm_id = p.segm_id WHERE t.parent_segm_id IS NOT NULL AND p.segm_id IS NULL".formatted(TMP_ADDR_SEGM_TABLE, TMP_ADDR_SEGM_TABLE),
                    TMP_ADDR_SEGM_TABLE),
            new QueryDef("DUPLICATE_CHECKS:tmp_addr_segm_duplicate_segm_id_top20",
                    "SELECT segm_id, COUNT(*) AS total FROM %s GROUP BY segm_id HAVING total > 1 ORDER BY total DESC LIMIT 20".formatted(TMP_ADDR_SEGM_TABLE),
                    TMP_ADDR_SEGM_TABLE),
            new QueryDef("DUPLICATE_CHECKS:tmp_addr_set_segm_duplicate_set_addr_id_top20",
                    "SELECT set_addr_id, COUNT(*) AS total FROM %s GROUP BY set_addr_id HAVING total > 1 ORDER BY total DESC LIMIT 20".formatted(TMP_ADDR_SET_SEGM_TABLE),
                    TMP_ADDR_SET_SEGM_TABLE),
            new QueryDef("RELATION_CHECKS:tmp_addr_set_segm_missing_standard_count",
                    "SELECT COUNT(*) AS missing_standard FROM %s s LEFT JOIN %s t ON s.segm_id = t.segm_id WHERE s.segm_id IS NOT NULL AND t.segm_id IS NULL".formatted(TMP_ADDR_SET_SEGM_TABLE, TMP_ADDR_SEGM_TABLE),
                    TMP_ADDR_SET_SEGM_TABLE, TMP_ADDR_SEGM_TABLE),
            new QueryDef("RELATION_CHECKS:tmp_addr_segm_missing_region_link_count",
                    "SELECT t.region_id, COUNT(*) AS total FROM %s t LEFT JOIN %s r ON t.region_id = r.region_id WHERE t.region_id IS NOT NULL AND r.region_id IS NULL GROUP BY t.region_id ORDER BY total DESC LIMIT 20".formatted(TMP_ADDR_SEGM_TABLE, SPC_REGION_TABLE),
                    TMP_ADDR_SEGM_TABLE, SPC_REGION_TABLE),
            new QueryDef("RELATION_CHECKS:tmp_addr_segm_missing_station_id_count",
                    "SELECT COUNT(*) AS missing_station FROM %s t LEFT JOIN %s s ON t.station_id = s.station_id WHERE t.station_id IS NOT NULL AND s.station_id IS NULL".formatted(TMP_ADDR_SEGM_TABLE, SPC_STATION_TABLE),
                    TMP_ADDR_SEGM_TABLE, SPC_STATION_TABLE),
            new QueryDef("RELATION_CHECKS:tmp_addr_segm_missing_installstation_id_count",
                    "SELECT COUNT(*) AS missing_installstation FROM %s t LEFT JOIN %s s ON t.installstation_id = s.station_id WHERE t.installstation_id IS NOT NULL AND s.station_id IS NULL".formatted(TMP_ADDR_SEGM_TABLE, SPC_STATION_TABLE),
                    TMP_ADDR_SEGM_TABLE, SPC_STATION_TABLE),
            new QueryDef("RELATION_CHECKS:tmp_addr_segm_missing_busstation_id_count",
                    "SELECT COUNT(*) AS missing_busstation FROM %s t LEFT JOIN %s s ON t.busstation_id = s.station_id WHERE t.busstation_id IS NOT NULL AND s.station_id IS NULL".formatted(TMP_ADDR_SEGM_TABLE, SPC_STATION_TABLE),
                    TMP_ADDR_SEGM_TABLE, SPC_STATION_TABLE),
            new QueryDef("DICTIONARY_CHECKS:segm_addr_type_level_distribution",
                    "SELECT level_id, COUNT(*) AS total FROM %s GROUP BY level_id ORDER BY total DESC LIMIT 20".formatted(SEGM_ADDR_TYPE_TABLE),
                    SEGM_ADDR_TYPE_TABLE),
            new QueryDef("DICTIONARY_CHECKS:pub_restriction_code_distribution",
                    "SELECT code, COUNT(*) AS total FROM %s GROUP BY code ORDER BY total DESC LIMIT 20".formatted(PUB_RESTRICTION_TABLE),
                    PUB_RESTRICTION_TABLE),
            new QueryDef("DICTIONARY_CHECKS:spc_regional_company_seg_type_priv_distribution",
                    "SELECT segm_type_priv, COUNT(*) AS total FROM %s GROUP BY segm_type_priv ORDER BY total DESC LIMIT 20".formatted(SPC_REGIONAL_COMPANY_TABLE),
                    SPC_REGIONAL_COMPANY_TABLE)
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
