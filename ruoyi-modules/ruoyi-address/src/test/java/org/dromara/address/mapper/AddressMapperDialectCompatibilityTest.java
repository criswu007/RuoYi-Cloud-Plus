package org.dromara.address.mapper;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class AddressMapperDialectCompatibilityTest {

    @Test
    void shouldAvoidDatabaseSpecificFunctionsInCoreAddressMappers() throws IOException {
        assertMapperDoesNotContainForbiddenSql("mapper/address/AddrSegmMapper.xml");
        assertMapperDoesNotContainForbiddenSql("mapper/address/SpcRegionMapper.xml");
        assertMapperDoesNotContainForbiddenSql("mapper/address/SegmAddrTypeMapper.xml");
        assertMapperDoesNotContainForbiddenSql("mapper/address/AddrSetSegmMapper.xml");
        assertMapperDoesNotContainForbiddenSql("mapper/address/SpcStationMapper.xml");
        assertMapperDoesNotContainForbiddenSql("mapper/address/PubRestrictionMapper.xml");
        assertMapperDoesNotContainForbiddenSql("mapper/address/StandardAddressImportDetailMapper.xml");
    }

    @Test
    void shouldNotReferenceMissingHistoricalColumnsInAddrSegmMapper() throws IOException {
        String content = readResource("mapper/address/AddrSegmMapper.xml").toLowerCase();
        assertFalse(content.contains("modify_date"),
            () -> "mapper/address/AddrSegmMapper.xml 不应引用线上 ADDR_SEGM 缺失的 modify_date 字段");
    }

    @Test
    void shouldUseGradeIdRulesForReadonlyRegionLevelProjection() throws IOException {
        String content = readResource("mapper/address/SpcRegionMapper.xml").toLowerCase();
        assertTrue(content.contains("grade_id"),
            () -> "mapper/address/SpcRegionMapper.xml 应按 spc_region.grade_id 判定一二级标准地址");
        assertTrue(content.contains("2000002"),
            () -> "mapper/address/SpcRegionMapper.xml 应包含一级地址 grade_id=2000002 规则");
        assertTrue(content.contains("2000004"),
            () -> "mapper/address/SpcRegionMapper.xml 应包含二级地址 grade_id=2000004 规则");
        assertFalse(content.contains("usegradeidlevelrules"),
            () -> "mapper/address/SpcRegionMapper.xml 不应继续保留旧结构兼容分支参数");
        assertFalse(content.contains("super_region_id is null or r.super_region_id = ''"),
            () -> "mapper/address/SpcRegionMapper.xml 一二级判定应直接基于 grade_id，不能再回退到 super_region_id");
    }

    @Test
    void shouldNotDeclareExplicitOrderingForReadonlyRegionQueries() throws IOException {
        String content = readResource("mapper/address/SpcRegionMapper.xml").toLowerCase();
        assertFalse(content.contains("order by r.region_no"),
            () -> "mapper/address/SpcRegionMapper.xml 一二级地址查询不应按 region_no 排序");
        assertFalse(content.contains("order by r.region_id"),
            () -> "mapper/address/SpcRegionMapper.xml 一二级地址查询不应显式指定 region_id 排序");
    }

    @Test
    void shouldAvoidSelectStarInBusinessMappers() throws IOException {
        assertMapperDoesNotContainSelectStar("mapper/address/AddrSegmMapper.xml");
        assertMapperDoesNotContainSelectStar("mapper/address/SpcRegionMapper.xml");
        assertMapperDoesNotContainSelectStar("mapper/address/AddrSetSegmMapper.xml");
        assertMapperDoesNotContainSelectStar("mapper/address/SpcStationMapper.xml");
        assertMapperDoesNotContainSelectStar("mapper/address/PubRestrictionMapper.xml");
        assertMapperDoesNotContainSelectStar("mapper/address/StandardAddressImportDetailMapper.xml");
    }

    @Test
    void shouldAvoidCaseWhenInBusinessMappers() throws IOException {
        assertMapperDoesNotContainCaseWhen("mapper/address/AddrSegmMapper.xml");
        assertMapperDoesNotContainCaseWhen("mapper/address/SpcRegionMapper.xml");
        assertMapperDoesNotContainCaseWhen("mapper/address/AddrSetSegmMapper.xml");
        assertMapperDoesNotContainCaseWhen("mapper/address/SpcStationMapper.xml");
        assertMapperDoesNotContainCaseWhen("mapper/address/PubRestrictionMapper.xml");
        assertMapperDoesNotContainCaseWhen("mapper/address/StandardAddressImportDetailMapper.xml");
    }

    @Test
    void shouldNotUseStandaloneSqlLineCommentsInBusinessMappers() throws IOException {
        assertMapperDoesNotContainStandaloneSqlLineComment("mapper/address/AddrSegmMapper.xml");
        assertMapperDoesNotContainStandaloneSqlLineComment("mapper/address/SpcRegionMapper.xml");
        assertMapperDoesNotContainStandaloneSqlLineComment("mapper/address/AddrSetSegmMapper.xml");
        assertMapperDoesNotContainStandaloneSqlLineComment("mapper/address/SpcStationMapper.xml");
        assertMapperDoesNotContainStandaloneSqlLineComment("mapper/address/PubRestrictionMapper.xml");
        assertMapperDoesNotContainStandaloneSqlLineComment("mapper/address/StandardAddressImportDetailMapper.xml");
    }

    private void assertMapperDoesNotContainForbiddenSql(String resourcePath) throws IOException {
        String content = readResource(resourcePath).toLowerCase();
        List<String> forbiddenTokens = List.of(
            "ifnull(",
            " limit ",
            "limit\n",
            "limit\t",
            "concat('%',",
            "nvl(",
            "decode(",
            "sysdate",
            "systimestamp",
            "to_char(",
            "to_date(",
            "date_format(",
            "group_concat(",
            "find_in_set(",
            "regexp_like(",
            "json_extract(",
            "json_value("
        );
        for (String forbiddenToken : forbiddenTokens) {
            assertFalse(content.contains(forbiddenToken),
                () -> resourcePath + " 不应包含跨库不兼容 SQL 片段: " + forbiddenToken);
        }
    }

    private void assertMapperDoesNotContainSelectStar(String resourcePath) throws IOException {
        String content = readResource(resourcePath).toLowerCase();
        assertFalse(content.contains("select *"),
            () -> resourcePath + " 不应使用 select *，必须显式声明列清单以降低多库与表结构差异风险");
    }

    private void assertMapperDoesNotContainCaseWhen(String resourcePath) throws IOException {
        String content = readResource(resourcePath);
        Pattern caseWhenPattern = Pattern.compile("(?i)\\bcase\\b\\s+\\bwhen\\b", Pattern.MULTILINE);
        assertFalse(caseWhenPattern.matcher(content).find(),
            () -> resourcePath + " 不应使用 case when，层级判定与特殊排序需上移到程序层适配");
    }

    private void assertMapperDoesNotContainStandaloneSqlLineComment(String resourcePath) throws IOException {
        String content = readResource(resourcePath);
        Pattern sqlLineCommentPattern = Pattern.compile("(?m)^\\s*--");
        assertFalse(sqlLineCommentPattern.matcher(content).find(),
            () -> resourcePath + " 不应包含 -- 行注释，避免分页插件或 SQL 改写在同一行追加片段时被注释吞掉");
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("未找到资源: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
