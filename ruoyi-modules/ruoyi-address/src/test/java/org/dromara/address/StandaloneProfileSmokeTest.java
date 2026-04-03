package org.dromara.address;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * standalone profile 冒烟测试。
 * 目的：验证地址模块在脱离完整微服务环境时，至少能够基于 standalone profile 完成 Spring 上下文启动。
 * 入参/出参：无显式入参，测试通过表示上下文成功加载。
 * 关键约束：仅验证 standalone 启动基线，不依赖 Nacos、Redis 与远程注册中心；数据源默认直连线上地址库。
 * 异常与副作用：上下文加载失败时测试直接失败；无业务数据写入副作用。
 */
@Tag("dev")
@ActiveProfiles("standalone")
@SpringBootTest(
    classes = RuoYiAddressApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class StandaloneProfileSmokeTest {

    /**
     * 目的：验证 standalone profile 下应用上下文可正常启动。
     * 入参：无。
     * 出参：无。
     * 关键约束：若 standalone 缺少最小配置、线上数据源或安全兜底，该测试必须失败。
     * 异常与副作用：上下文启动异常会导致测试失败，无其他副作用。
     */
    @Test
    void shouldLoadApplicationContextUnderStandaloneProfile() {
    }
}
