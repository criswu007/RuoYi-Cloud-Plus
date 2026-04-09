package org.dromara.address;

import org.dromara.address.config.AddressSearchProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * standalone 运行模式冒烟测试。
 * 目的：验证地址模块在脱离完整微服务环境时，至少能够基于 local profile + standalone mode 完成 Spring 上下文启动。
 * 入参/出参：无显式入参，测试通过表示上下文成功加载。
 * 关键约束：仅验证 standalone 启动基线，不依赖 Nacos、Redis 与远程注册中心；数据源默认直连线上地址库。
 * 异常与副作用：上下文加载失败时测试直接失败；无业务数据写入副作用。
 */
@Tag("dev")
@ActiveProfiles("local")
@TestPropertySource(properties = "address.runtime.mode=standalone")
@SpringBootTest(
    classes = RuoYiAddressApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class StandaloneProfileSmokeTest {

    @MockBean
    private RedissonClient redissonClient;

    @Autowired
    private AddressSearchProperties addressSearchProperties;

    @Autowired
    private RedisProperties redisProperties;

    @Value("${easy-es.address:}")
    private String easyEsAddress;

    /**
     * 目的：验证 local profile + standalone mode 下已绑定本地 Redis、ES 与 Kibana 联调地址。
     * 入参：无。
     * 出参：无。
     * 关键约束：standalone 必须显式指向本地 Redis/ES/Kibana，不能继续依赖远端默认值或缺省配置。
     * 异常与副作用：上下文启动异常或属性绑定不符合预期时测试失败，无业务数据写入副作用。
     */
    @Test
    void shouldExposeLocalRedisAndEsEndpointsUnderStandaloneProfile() {
        assertEquals("127.0.0.1", redisProperties.getHost());
        assertEquals(6379, redisProperties.getPort());
        assertEquals("ruoyi123", redisProperties.getPassword());
        assertEquals("127.0.0.1:9200", easyEsAddress);
        assertEquals("http://127.0.0.1:5601", addressSearchProperties.getKibanaUrl());
        assertEquals("address:search:maintenance:running", addressSearchProperties.getMaintenanceLockKey());
        assertEquals(Boolean.TRUE, addressSearchProperties.getStandard().getReadEnabled());
        assertEquals(Boolean.TRUE, addressSearchProperties.getStandard().getWriteEnabled());
        assertEquals(Boolean.TRUE, addressSearchProperties.getInstallation().getReadEnabled());
        assertEquals(Boolean.TRUE, addressSearchProperties.getInstallation().getWriteEnabled());
    }
}
