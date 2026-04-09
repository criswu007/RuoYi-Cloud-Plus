package org.dromara.address.config.condition;

import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * 地址运行模式判定工具。
 * 目的：统一判定地址模块当前应按 standalone 还是 microservice 方式装配，兼容 legacy `standalone` profile 与新的 `local + address.runtime.mode` 配置开关。
 * 入参/出参：输入 Spring `Environment`，输出运行模式布尔判定结果。
 * 关键约束：legacy `standalone` profile 优先级最高；`local` profile 下默认按 standalone 处理，避免本地联调升级后失去原有直连能力。
 * 异常与副作用：无外部副作用，仅执行环境判定。
 */
public final class AddressRuntimeMode {

    public static final String LOCAL_PROFILE = "local";
    public static final String LEGACY_STANDALONE_PROFILE = "standalone";
    public static final String MODE_PROPERTY = "address.runtime.mode";
    public static final String STANDALONE = "standalone";
    public static final String MICROSERVICE = "microservice";

    private AddressRuntimeMode() {
    }

    /**
     * 目的：判断地址模块当前是否应按 standalone 方式运行。
     * 入参：Spring 运行环境。
     * 出参：若命中 legacy `standalone` profile，或命中 `local + address.runtime.mode=standalone`，则返回 `true`。
     * 关键约束：`local` profile 未显式配置模式时默认回退 standalone，保证本地联调兼容。
     * 异常与副作用：无。
     *
     * @param environment Spring 运行环境
     * @return 是否按 standalone 方式运行
     */
    public static boolean isStandalone(Environment environment) {
        if (hasActiveProfile(environment, LEGACY_STANDALONE_PROFILE)) {
            return true;
        }
        if (!hasActiveProfile(environment, LOCAL_PROFILE)) {
            return false;
        }
        return !MICROSERVICE.equalsIgnoreCase(environment.getProperty(MODE_PROPERTY, STANDALONE));
    }

    /**
     * 目的：判断地址模块当前是否应按 microservice 方式运行。
     * 入参：Spring 运行环境。
     * 出参：若未命中 standalone，且位于 `local + address.runtime.mode=microservice` 或常规非 standalone 环境，则返回 `true`。
     * 关键约束：任何命中 standalone 的场景都必须返回 `false`，避免 Dubbo 与本地 HTTP workflow 适配器同时装配。
     * 异常与副作用：无。
     *
     * @param environment Spring 运行环境
     * @return 是否按 microservice 方式运行
     */
    public static boolean isMicroservice(Environment environment) {
        return !isStandalone(environment);
    }

    private static boolean hasActiveProfile(Environment environment, String profile) {
        return Arrays.asList(environment.getActiveProfiles()).contains(profile);
    }
}
