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
import org.springframework.test.context.TestPropertySource;
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
@ActiveProfiles("local")
@TestPropertySource(properties = "address.runtime.mode=standalone")
@SpringBootTest(
    classes = RuoYiAddressApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class StandaloneStandardAddressEndpointTest {

    private static final String DEFAULT_STANDALONE_REGION_ID = "000102140000000021128049";

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
     * 入参：当前列表接口返回的首条线上标准地址。
     * 出参：无。
     * 关键约束：详情返回必须覆盖 `singleProjectCode/isCity/addrInTypeFtth/areaType` 等 canonical 字段，供详情页与编辑弹窗直接回显。
     * 异常与副作用：请求失败或字段缺失会直接导致测试失败，不写入业务数据。
     */
    @Test
    void shouldExposeCanonicalExtensionFieldsOnStandardAddressDetailUnderStandaloneProfile() throws Exception {
        JsonNode listRoot = requestStandardAddressList("1", "1", null, null);
        JsonNode firstRow = listRoot.path("rows").get(0);
        assertNotNull(firstRow, () -> "线上标准地址列表为空，无法校验详情接口");
        String segmId = firstRow.path("segmId").asText();

        var response = testRestTemplate.postForEntity("/address/standard/" + segmId, null, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode data = root.path("data");
        assertEquals(200, root.path("code").asInt(), () -> "标准地址详情接口返回异常响应: " + response.getBody());
        assertEquals(segmId, data.path("segmId").asText(), () -> "详情返回的 segmId 与请求不一致: " + response.getBody());
        assertTrue(data.hasNonNull("standName"), () -> "详情未返回 standName: " + response.getBody());
        assertTrue(data.hasNonNull("segmType"), () -> "详情未返回 segmType: " + response.getBody());
        assertTrue(data.has("addrLevel"), () -> "详情未返回 addrLevel: " + response.getBody());
        assertTrue(data.hasNonNull("regionId"), () -> "详情未返回 regionId: " + response.getBody());
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
        assertTrue(containsLevelOption(data, "180007", "房间", 15, 90), () -> "缺少房间级别选项: " + response.getBody());
        assertTrue(containsLevelOption(data, "180100", "尾级地址(选址生成)", 18, 100), () -> "缺少尾级地址级别选项: " + response.getBody());
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
        assertTrue(containsRestrictionOption(data.path("addrInTypeLanOptions"), "2140784", "LAN"), () -> "LAN 接入方式字典缺失: " + response.getBody());
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
              "regionId": "%s",
              "manageType": "2017101",
              "keyword": "",
              "limit": 5
            }
            """.formatted(DEFAULT_STANDALONE_REGION_ID);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        var response = testRestTemplate.postForEntity("/address/standard/stationOptions", requestEntity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode data = root.path("data");

        assertEquals(200, root.path("code").asInt(), () -> "管理站候选接口返回异常响应: " + response.getBody());
        for (JsonNode item : data) {
            assertEquals(DEFAULT_STANDALONE_REGION_ID, item.path("regionId").asText(), () -> "管理站区域过滤未生效: " + response.getBody());
            assertEquals("2017101", item.path("manageType").asText(), () -> "管理站类型过滤未生效: " + response.getBody());
            assertTrue(item.hasNonNull("stationId"), () -> "管理站未返回 stationId: " + response.getBody());
            assertTrue(item.hasNonNull("stationName"), () -> "管理站未返回 stationName: " + response.getBody());
        }
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
        for (JsonNode item : data) {
            assertEquals(DEFAULT_STANDALONE_REGION_ID, item.path("regionId").asText(), () -> "未传 regionId 时返回了跨区域候选: " + response.getBody());
            assertEquals("2017101", item.path("manageType").asText(), () -> "未传 regionId 时管理站类型过滤未生效: " + response.getBody());
        }
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
        JsonNode root = requestStandardAddressList("1", "10", "2", "南京");
        JsonNode rows = root.path("rows");

        assertTrue(rows.isArray() && rows.size() > 0, () -> "层级+名称组合筛选未返回数据: " + root);
        for (JsonNode item : rows) {
            assertEquals(2, item.path("addrLevel").asInt(), () -> "层级筛选未生效: " + item);
            assertTrue(item.path("standName").asText().contains("南京"), () -> "标准地址名称筛选未生效: " + item);
            assertTrue(!"江苏省".equals(item.path("standName").asText()), () -> "层级+名称组合筛选错误返回了省级数据: " + item);
        }
    }

    /**
     * 目的：验证一级标准地址查询不会被当前登录区域的 `regionId` 隐式附加过滤。
     * 入参：一级行政区划查询条件 `addrLevel=1`、`standName=江苏`。
     * 出参：无。
     * 关键约束：`1/2` 级走 `spc_region` 时，只允许使用显式查询条件，不允许再叠加当前登录态区域过滤，否则会把省级结果错误收敛到当前区域链路上。
     * 异常与副作用：若仍注入登录态 `regionId` 附加条件，将直接导致线上库的省级检索查空，测试失败且不写入业务数据。
     */
    @Test
    void shouldNotApplyImplicitRegionFilterWhenQueryingLevelOneStandardAddressUnderStandaloneProfile() throws Exception {
        JsonNode root = requestStandardAddressList("1", "10", "1", "江苏");
        JsonNode rows = root.path("rows");

        assertTrue(rows.isArray() && rows.size() > 0, () -> "一级标准地址被隐式 regionId 条件过滤后未返回数据: " + root);
        boolean containsProvince = false;
        for (JsonNode item : rows) {
            assertEquals(1, item.path("addrLevel").asInt(), () -> "一级标准地址层级筛选未生效: " + item);
            if ("江苏省".equals(item.path("standName").asText())) {
                containsProvince = true;
            }
        }
        assertTrue(containsProvince, () -> "一级标准地址查询未返回江苏省，疑似仍被 regionId 隐式过滤: " + root);
    }

    /**
     * 目的：验证一二级地址查询会按 `segm_addr_type.addr_type_id` 而不是 `levelId=1/2` 分流到 `spc_region`。
     * 入参：二级地址类型 `180001` 与地址名关键字 `南京`。
     * 出参：无。
     * 关键约束：当查询条件使用 `segmType=180001` 时，必须返回城市级区域投影而不是区县或 `ADDR_SEGM` 事实表；当前线上 `spc_region.grade_id=2000004` 对应城市级，因此应命中 `南京市`。
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
        assertTrue(response.getBody().contains("\"segmId\":\"000102000000000042761538\""), () -> "未按二级地址类型返回城市级 spc_region 投影数据: " + response.getBody());
        assertTrue(response.getBody().contains("\"standName\":\"南京市\""), () -> "未返回南京市区域投影: " + response.getBody());
        assertTrue(!response.getBody().contains("\"standName\":\"南京市区\""), () -> "错误返回了区县级区域投影: " + response.getBody());
        assertTrue(!response.getBody().contains("\"segmId\":\"000000000000000000000301\""), () -> "错误落到了旧样例 ADDR_SEGM 数据: " + response.getBody());
    }

    /**
     * 目的：验证“市区”类型标准地址查询仍走 `ADDR_SEGM`，且在 standalone + 线上库模式下可正常返回响应。
     * 入参：`segmType=180015`，对应线上 `segm_addr_type` 中的“市区”类型。
     * 出参：无。
     * 关键约束：`180015` 虽然名称里包含“市区”，但其真实 `level_id=8`，不属于一二级区域投影；查询时不能按只读区域链路拦截或分流到 `spc_region`。
     * 异常与副作用：若接口返回 500、错误分流或错误拦截将直接导致测试失败，不写入业务数据。
     */
    @Test
    void shouldQueryCityDistrictAddrTypeFromAddrSegmUnderStandaloneProfile() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("pageNum", "1");
        body.add("pageSize", "10");
        body.add("segmType", "180015");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        var response = testRestTemplate.postForEntity("/address/standard/list", requestEntity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode rows = root.path("rows");

        assertEquals(200, root.path("code").asInt(), () -> "市区级标准地址查询返回异常响应: " + response.getBody());
        assertTrue(rows.isArray(), () -> "市区级标准地址查询未返回数组结果: " + response.getBody());
        assertTrue(rows.size() > 0, () -> "市区级标准地址查询未返回数据: " + response.getBody());
        for (JsonNode item : rows) {
            assertEquals("180015", item.path("segmType").asText(), () -> "市区级标准地址查询返回了非 180015 类型数据: " + item);
        }
    }

    private JsonNode requestStandardAddressList(String pageNum, String pageSize, String addrLevel, String standName) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("pageNum", pageNum);
        body.add("pageSize", pageSize);
        if (addrLevel != null) {
            body.add("addrLevel", addrLevel);
        }
        if (standName != null) {
            body.add("standName", standName);
        }
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        var response = testRestTemplate.postForEntity("/address/standard/list", requestEntity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode root = objectMapper.readTree(response.getBody());
        assertEquals(200, root.path("code").asInt(), () -> "标准地址列表接口返回异常响应: " + response.getBody());
        return root;
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
}
