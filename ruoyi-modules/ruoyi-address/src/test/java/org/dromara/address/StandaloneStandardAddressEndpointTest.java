package org.dromara.address;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * standalone 标准地址接口联通测试。
 * 目的：验证 standalone 模式下标准地址核心列表接口可被真实 HTTP 请求访问，而不只是应用上下文能够启动。
 * 入参/出参：无显式入参；测试通过表示 `/address/standard/list` 在本地联调模式下可返回成功响应。
 * 关键约束：必须走真实 HTTP 链路，以覆盖 standalone mock 登录过滤器、鉴权与 Controller 参数绑定行为。
 * 异常与副作用：若 standalone 缺失鉴权配置、JWT 配置或最小化基础设施兜底，该测试会直接失败。
 */
@Tag("dev")
@ActiveProfiles("standalone")
@SpringBootTest(
    classes = RuoYiAddressApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class StandaloneStandardAddressEndpointTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 目的：验证标准地址列表接口在 standalone 模式下可返回 HTTP 200。
     * 入参：无。
     * 出参：无。
     * 关键约束：请求体按当前后端约定使用 `application/x-www-form-urlencoded` 提交分页参数。
     * 异常与副作用：请求失败会直接导致测试失败，不写入业务数据。
     */
    @Test
    void shouldAccessStandardAddressListEndpointUnderStandaloneProfile() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("pageNum", "1");
        body.add("pageSize", "10");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        var response = testRestTemplate.postForEntity("/address/standard/list", requestEntity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"code\":200"), () -> "标准地址列表接口返回异常响应: " + response.getBody());
    }

    /**
     * 目的：验证标准地址详情接口会返回线上语义的扩展属性字段。
     * 入参：standalone 样例标准地址 `000000000000000000000305`。
     * 出参：无。
     * 关键约束：详情返回必须覆盖 `singleProjectCode/isCity/addrInTypeFtth/areaType` 等 canonical 字段，供详情页与编辑弹窗直接回显。
     * 异常与副作用：请求失败或字段缺失会直接导致测试失败，不写入业务数据。
     */
    @Test
    void shouldExposeCanonicalExtensionFieldsOnStandardAddressDetailUnderStandaloneProfile() {
        var response = testRestTemplate.postForEntity("/address/standard/000000000000000000000305", null, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"singleProjectCode\":\"GC-2026-001\""), () -> "详情未返回 singleProjectCode: " + response.getBody());
        assertTrue(response.getBody().contains("\"isCity\":\"Y\""), () -> "详情未返回 isCity: " + response.getBody());
        assertTrue(response.getBody().contains("\"addrLevel\":15"), () -> "详情未返回业务级别 addrLevel: " + response.getBody());
        assertTrue(response.getBody().contains("\"levelId\":150"), () -> "详情未返回数据库层级 levelId: " + response.getBody());
        assertTrue(response.getBody().contains("\"addrInTypeFtth\":2140760"), () -> "详情未返回 addrInTypeFtth: " + response.getBody());
        assertTrue(response.getBody().contains("\"areaType\":2140511"), () -> "详情未返回 areaType: " + response.getBody());
    }

    /**
     * 目的：验证标准地址级别选项接口会返回 `segm_addr_type` 的完整 19 级数据。
     * 入参：无。
     * 出参：无。
     * 关键约束：返回字段至少包含 `addrTypeId/name/addrLevel/levelId`，并覆盖房间号、尾级地址等关键类型。
     * 异常与副作用：请求失败、返回条数不足或字段缺失会直接导致测试失败，不写入业务数据。
     */
    @Test
    void shouldExposeAllStandardAddressLevelOptionsUnderStandaloneProfile() throws Exception {
        var response = testRestTemplate.postForEntity("/address/standard/levelOptions", null, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode data = root.path("data");

        assertEquals(200, root.path("code").asInt(), () -> "级别选项接口返回异常响应: " + response.getBody());
        assertEquals(19, data.size(), () -> "级别选项数量不正确: " + response.getBody());
        assertTrue(containsLevelOption(data, "180007", "房间号", 16, 160), () -> "缺少房间号级别选项: " + response.getBody());
        assertTrue(containsLevelOption(data, "180100", "尾级地址（选址生成）", 19, 190), () -> "缺少尾级地址级别选项: " + response.getBody());
    }

    /**
     * 目的：验证标准地址编辑页聚合字典接口会返回状态、接入方式、接入能力、城乡属性与房屋属性等分组。
     * 入参：无。
     * 出参：无。
     * 关键约束：返回结构必须覆盖前端编辑页依赖的固定 key，且字典项名称与编码直接来自 `pub_restriction`。
     * 异常与副作用：请求失败、分组缺失或字典为空会直接导致测试失败，不写入业务数据。
     */
    @Test
    void shouldExposeStandardAddressFormOptionsUnderStandaloneProfile() throws Exception {
        var response = testRestTemplate.postForEntity("/address/standard/formOptions", null, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode data = root.path("data");

        assertEquals(200, root.path("code").asInt(), () -> "编辑页聚合字典接口返回异常响应: " + response.getBody());
        assertTrue(containsRestrictionOption(data.path("statusOptions"), "2140900", "有效"), () -> "状态字典缺少有效: " + response.getBody());
        assertTrue(containsRestrictionOption(data.path("addrInTypeFtthOptions"), "2140760", "FTTH_双纤"), () -> "FTTH 接入方式字典缺失: " + response.getBody());
        assertTrue(containsRestrictionOption(data.path("ftthPonTypeOptions"), "2141301", "1G-PON"), () -> "PON 接入能力字典缺失: " + response.getBody());
        assertTrue(containsRestrictionOption(data.path("addrInTypeLanOptions"), "2140770", "LAN"), () -> "LAN 接入方式字典缺失: " + response.getBody());
        assertTrue(containsRestrictionOption(data.path("areaTypeOptions"), "2140511", "城区"), () -> "城乡属性字典缺失: " + response.getBody());
        assertTrue(containsRestrictionOption(data.path("placeTypeOptions"), "2140800", "普通住宅"), () -> "房屋属性字典缺失: " + response.getBody());
    }

    /**
     * 目的：验证管理站候选接口会按区域、类型与关键字联合过滤。
     * 入参：南京区域 `320100`、维修管理站类型 `2017101`、关键字 `洪武`。
     * 出参：无。
     * 关键约束：候选必须直接来自 `spc_station`，并保留 `stationId/stationName/regionId/manageType` 字段供前端回显。
     * 异常与副作用：请求失败或过滤条件未生效会直接导致测试失败，不写入业务数据。
     */
    @Test
    void shouldExposeStandardAddressStationOptionsUnderStandaloneProfile() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = """
            {
              "regionId": "320100",
              "manageType": "2017101",
              "keyword": "洪武",
              "limit": 5
            }
            """;

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        var response = testRestTemplate.postForEntity("/address/standard/stationOptions", requestEntity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode data = root.path("data");

        assertEquals(200, root.path("code").asInt(), () -> "管理站候选接口返回异常响应: " + response.getBody());
        assertTrue(containsStationOption(data, "ST320100WX001", "洪武路维修站", "320100", "2017101"), () -> "未返回命中的维修管理站: " + response.getBody());
        assertTrue(!containsStationOption(data, "ST320100AZ001", "洪武路安装站", "320100", "2017102"), () -> "管理站类型过滤未生效: " + response.getBody());
        assertTrue(!containsStationOption(data, "ST320200WX001", "无锡维修站", "320200", "2017101"), () -> "管理站区域过滤未生效: " + response.getBody());
    }

    /**
     * 目的：验证管理站候选接口在未显式传入 `regionId` 时，会回退到当前登录用户所在区域。
     * 入参：维修管理站类型 `2017101`、关键字 `洪武`，不传 `regionId`。
     * 出参：无。
     * 关键约束：当前用户区域过滤必须作为默认条件生效，避免下拉候选跨区域串数。
     * 异常与副作用：请求失败或默认区域过滤未生效会直接导致测试失败，不写入业务数据。
     */
    @Test
    void shouldFallbackToCurrentUserRegionWhenStationOptionRegionMissingUnderStandaloneProfile() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = """
            {
              "manageType": "2017101",
              "keyword": "洪武",
              "limit": 5
            }
            """;

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        var response = testRestTemplate.postForEntity("/address/standard/stationOptions", requestEntity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode data = root.path("data");

        assertEquals(200, root.path("code").asInt(), () -> "管理站候选接口返回异常响应: " + response.getBody());
        assertTrue(containsStationOption(data, "ST320100WX001", "洪武路维修站", "320100", "2017101"), () -> "未按当前用户区域返回南京维修站: " + response.getBody());
        assertTrue(!containsStationOption(data, "ST320200WX001", "洪武东路维修站", "320200", "2017101"), () -> "未传 regionId 时仍返回了跨区域候选: " + response.getBody());
    }

    /**
     * 目的：验证标准地址列表在多条件场景下会同时应用层级与标准地址名称筛选。
     * 入参：一级/二级行政区划查询条件 `addrLevel=2`、`standName=南京`。
     * 出参：无。
     * 关键约束：`1/2` 级走 `spc_region` 时，`standName` 也必须参与过滤，不能只按层级筛选。
     * 异常与副作用：若多条件筛选未生效会直接导致测试失败，不写入业务数据。
     */
    @Test
    void shouldFilterStandardAddressListByLevelAndStandNameTogetherUnderStandaloneProfile() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("pageNum", "1");
        body.add("pageSize", "10");
        body.add("addrLevel", "2");
        body.add("standName", "南京");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        var response = testRestTemplate.postForEntity("/address/standard/list", requestEntity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"code\":200"), () -> "标准地址列表接口返回异常响应: " + response.getBody());
        assertTrue(response.getBody().contains("\"standName\":\"南京市\""), () -> "未按标准地址名称返回南京市: " + response.getBody());
        assertTrue(!response.getBody().contains("\"standName\":\"江苏省\""), () -> "层级+名称组合筛选错误返回了省级数据: " + response.getBody());
    }

    /**
     * 目的：验证一二级地址查询会按 `segm_addr_type.addr_type_id` 而不是 `levelId=1/2` 分流到 `spc_region`。
     * 入参：二级地址类型 `180001` 与地址名关键字 `南京`。
     * 出参：无。
     * 关键约束：当查询条件使用 `segmType=180001` 时，必须返回城市级区域投影，而不是落到 `ADDR_SEGM` 事实表。
     * 异常与副作用：若路由错误或仍依赖 `levelId=1/2` 判断会直接导致测试失败，不写入业务数据。
     */
    @Test
    void shouldQueryRegionProjectionByProvinceOrCityAddrTypeUnderStandaloneProfile() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("pageNum", "1");
        body.add("pageSize", "10");
        body.add("segmType", "180001");
        body.add("standName", "南京");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        var response = testRestTemplate.postForEntity("/address/standard/list", requestEntity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"segmId\":\"320100\""), () -> "未按城市地址类型返回 spc_region 城市数据: " + response.getBody());
        assertTrue(response.getBody().contains("\"standName\":\"南京市\""), () -> "未返回南京市区域投影: " + response.getBody());
        assertTrue(!response.getBody().contains("\"segmId\":\"000000000000000000000301\""), () -> "错误落到了 ADDR_SEGM 主城区事实表: " + response.getBody());
    }

    /**
     * 目的：验证 standalone 模式下导入失败明细列表接口可返回真实分页数据。
     * 入参：分页参数与导入文件名关键字。
     * 出参：无。
     * 关键约束：失败明细必须来自 `address_standard_import_fail_detail` 真表分页查询，且能联表回填批次号与文件名。
     * 异常与副作用：若 standalone 脚本未建表、未灌入样例批次数据或接口走了内存分页，该测试会直接失败。
     */
    @Test
    void shouldAccessImportFailureRecordListUnderStandaloneProfile() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("pageNum", "1");
        body.add("pageSize", "10");
        body.add("fileName", "standard-address-import-demo.xlsx");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        var response = testRestTemplate.postForEntity("/address/import-record/list", requestEntity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"total\":1"), () -> "导入失败明细分页总数不正确: " + response.getBody());
        assertTrue(response.getBody().contains("\"batchNo\":\"IMP202604020001\""), () -> "未返回批次号: " + response.getBody());
        assertTrue(response.getBody().contains("\"fileName\":\"standard-address-import-demo.xlsx\""), () -> "未返回导入文件名: " + response.getBody());
        assertTrue(response.getBody().contains("\"segmName\":\"测试失败地址\""), () -> "未返回失败地址明细: " + response.getBody());
    }

    /**
     * 目的：验证 standalone 模式下可按批次主键查询导入批次摘要。
     * 入参：样例批次主键 `1001`。
     * 出参：无。
     * 关键约束：批次摘要需返回 `batchNo/totalCount/updateSupport` 等当前导入合同字段。
     * 异常与副作用：若 standalone 脚本缺少新字段或批次样例数据，该测试会直接失败。
     */
    @Test
    void shouldAccessImportBatchSummaryUnderStandaloneProfile() {
        var response = testRestTemplate.postForEntity("/address/import-record/batch/1001", null, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"code\":200"), () -> "导入批次摘要接口返回异常响应: " + response.getBody());
        assertTrue(response.getBody().contains("\"batchNo\":\"IMP202604020001\""), () -> "未返回导入批次号: " + response.getBody());
        assertTrue(response.getBody().contains("\"totalCount\":3"), () -> "未返回总数量: " + response.getBody());
        assertTrue(response.getBody().contains("\"updateSupport\":false"), () -> "未返回更新支持标记: " + response.getBody());
    }

    private boolean containsLevelOption(JsonNode data, String addrTypeId, String name, int addrLevel, int levelId) {
        for (JsonNode item : data) {
            if (addrTypeId.equals(item.path("addrTypeId").asText())
                && name.equals(item.path("name").asText())
                && addrLevel == item.path("addrLevel").asInt()
                && levelId == item.path("levelId").asInt()) {
                return true;
            }
        }
        return false;
    }

    private boolean containsRestrictionOption(JsonNode data, String value, String label) {
        for (JsonNode item : data) {
            if (value.equals(item.path("value").asText()) && label.equals(item.path("label").asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsStationOption(JsonNode data, String stationId, String stationName, String regionId, String manageType) {
        for (JsonNode item : data) {
            if (stationId.equals(item.path("stationId").asText())
                && stationName.equals(item.path("stationName").asText())
                && regionId.equals(item.path("regionId").asText())
                && manageType.equals(item.path("manageType").asText())) {
                return true;
            }
        }
        return false;
    }
}
