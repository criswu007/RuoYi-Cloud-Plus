package org.dromara.address;

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
}
