/*
 * 南京标准地址核心盘点脚本（JShell 形式）
 * 目的：连接南京历史地址库，按类别输出稳定的盘点标签，覆盖行数、字段、空值、重复、关联与字典验证
 */

import java.sql.*;
import java.util.*;

class QueryDef {
    final String label;
    final String sql;

    QueryDef(String label, String sql) {
        this.label = label;
        this.sql = sql;
    }
}

void printResult(Connection conn, String label, String sql) {
    System.out.println("## " + label);
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
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
                builder.append(meta.getColumnLabel(i))
                        .append("=")
                        .append(value == null ? "NULL" : value);
            }
            System.out.println(builder);
        }
        if (!hasRows) {
            System.out.println("  [EMPTY RESULT]");
        }
    } catch (SQLException e) {
        System.out.println("  [QUERY ERROR] " + e.getMessage());
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

void runTableRowCounts(Connection conn) {
    List<String> tables = Arrays.asList(
            "tmp_addr_segm_nj_20260317",
            "tmp_addr_set_segm_nj_20260317",
            "addr_main",
            "install_addr_main",
            "addr_dictionary",
            "addr_type",
            "region",
            "station"
    );
    for (String table : tables) {
        String label = "TABLE_ROW_COUNTS:" + table;
        if (!tableExists(conn, table)) {
            System.out.println("## " + label);
            System.out.println("  [TABLE MISSING]");
            continue;
        }
        printResult(conn, label, "SELECT COUNT(*) AS row_count FROM " + table);
    }
}

List<QueryDef> buildQueries() {
    return Arrays.asList(
            new QueryDef("ADDR_MAIN_COLUMNS:structure",
                    "SELECT column_name, column_type, is_nullable, column_default FROM information_schema.columns WHERE table_schema='ftth_cloud_address' AND table_name='addr_main' ORDER BY ordinal_position"),
            new QueryDef("INSTALL_ADDR_MAIN_COLUMNS:structure",
                    "SELECT column_name, column_type, is_nullable, column_default FROM information_schema.columns WHERE table_schema='ftth_cloud_address' AND table_name='install_addr_main' ORDER BY ordinal_position"),
            new QueryDef("ADDR_MAIN_COLUMNS:tmp_addr_segm_nj_seg_type",
                    "SELECT segm_type, COUNT(*) AS total FROM tmp_addr_segm_nj_20260317 GROUP BY segm_type ORDER BY total DESC"),
            new QueryDef("ADDR_MAIN_COLUMNS:tmp_addr_segm_nj_key_fields",
                    "SELECT parent_segm_id IS NULL AS missing_parent, COUNT(*) AS total FROM tmp_addr_segm_nj_20260317 GROUP BY missing_parent"),
            new QueryDef("ADDR_MAIN_COLUMNS:tmp_addr_set_segm_nj_set_type",
                    "SELECT set_type, COUNT(*) AS total FROM tmp_addr_set_segm_nj_20260317 GROUP BY set_type ORDER BY total DESC"),
            new QueryDef("NULL_CHECKS:tmp_addr_segm_missing_parent",
                    "SELECT COUNT(*) AS missing_parent FROM tmp_addr_segm_nj_20260317 t LEFT JOIN tmp_addr_segm_nj_20260317 p ON t.parent_segm_id = p.segm_id WHERE t.parent_segm_id IS NOT NULL AND p.segm_id IS NULL"),
            new QueryDef("DUPLICATE_CHECKS:tmp_addr_segm_segm_id",
                    "SELECT segm_id, COUNT(*) AS total FROM tmp_addr_segm_nj_20260317 GROUP BY segm_id HAVING total > 1 ORDER BY total DESC LIMIT 20"),
            new QueryDef("DUPLICATE_CHECKS:tmp_addr_set_segm_set_id",
                    "SELECT set_id, COUNT(*) AS total FROM tmp_addr_set_segm_nj_20260317 GROUP BY set_id HAVING total > 1 ORDER BY total DESC LIMIT 20"),
            new QueryDef("RELATION_CHECKS:install_missing_addr_main",
                    "SELECT COUNT(*) AS missing_standard FROM install_addr_main i LEFT JOIN addr_main a ON i.segm_id = a.segm_id WHERE i.segm_id IS NOT NULL AND a.segm_id IS NULL"),
            new QueryDef("RELATION_CHECKS:region_link",
                    "SELECT a.region_id, COUNT(*) AS total FROM addr_main a LEFT JOIN region r ON a.region_id = r.region_id WHERE a.region_id IS NOT NULL GROUP BY a.region_id ORDER BY total DESC LIMIT 20"),
            new QueryDef("RELATION_CHECKS:station_link",
                    "SELECT i.station_id, COUNT(*) AS total FROM install_addr_main i LEFT JOIN station s ON i.station_id = s.station_id WHERE i.station_id IS NOT NULL GROUP BY i.station_id ORDER BY total DESC LIMIT 20"),
            new QueryDef("DICTIONARY_CHECKS:addr_dictionary_type",
                    "SELECT dict_type, COUNT(*) AS total FROM addr_dictionary GROUP BY dict_type ORDER BY total DESC LIMIT 20"),
            new QueryDef("DICTIONARY_CHECKS:addr_type_code",
                    "SELECT type_code, COUNT(*) AS total FROM addr_type GROUP BY type_code ORDER BY total DESC LIMIT 20")
    );
}

final String url = "jdbc:mysql://82.156.6.111:4000/ftth_cloud_address?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true";
final Properties props = new Properties();
props.setProperty("user", "address");
props.setProperty("password", "Wetry2328!");

try (Connection conn = DriverManager.getConnection(url, props)) {
    System.out.println("## CONNECTED");
    System.out.println("Current catalog: " + conn.getCatalog());
    runTableRowCounts(conn);
    for (QueryDef query : buildQueries()) {
        printResult(conn, query.label, query.sql);
    }
} catch (SQLException e) {
    System.out.println("[CONNECT ERROR] " + e.getMessage());
}
