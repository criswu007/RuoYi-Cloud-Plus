package org.dromara.address.config.condition;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 地址运行模式判定测试。
 * 目的：验证地址模块可同时兼容 legacy `standalone` profile 与 `local + address.runtime.mode` 两类本地启动方式。
 * 入参/出参：输入模拟环境配置，输出布尔判定结果。
 * 关键约束：microservice 必须覆盖 `local + microservice` 与非 standalone 常规环境；standalone 必须兼容 legacy profile。
 * 异常与副作用：无外部副作用，仅校验纯判定逻辑。
 */
@Tag("dev")
class AddressRuntimeModeTest {

    @Test
    void shouldTreatLegacyStandaloneProfileAsStandaloneMode() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("standalone");

        assertTrue(AddressRuntimeMode.isStandalone(environment));
        assertFalse(AddressRuntimeMode.isMicroservice(environment));
    }

    @Test
    void shouldTreatLocalStandaloneModeAsStandaloneMode() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        environment.setProperty("address.runtime.mode", "standalone");

        assertTrue(AddressRuntimeMode.isStandalone(environment));
        assertFalse(AddressRuntimeMode.isMicroservice(environment));
    }

    @Test
    void shouldTreatLocalMicroserviceModeAsMicroserviceMode() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        environment.setProperty("address.runtime.mode", "microservice");

        assertFalse(AddressRuntimeMode.isStandalone(environment));
        assertTrue(AddressRuntimeMode.isMicroservice(environment));
    }

    @Test
    void shouldTreatNormalDevProfileAsMicroserviceMode() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        assertFalse(AddressRuntimeMode.isStandalone(environment));
        assertTrue(AddressRuntimeMode.isMicroservice(environment));
    }
}
