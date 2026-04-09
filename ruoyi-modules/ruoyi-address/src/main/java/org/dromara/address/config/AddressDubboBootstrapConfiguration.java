package org.dromara.address.config;

import org.dromara.address.config.condition.AddressMicroserviceRuntimeCondition;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * 地址模块 Dubbo 启动配置。
 * 目的：仅在真实联调/部署环境中启用 Dubbo 能力，避免 standalone 本地模式被注册中心与元数据中心阻塞。
 * 入参/出参：无显式入参或出参，由 Spring 在匹配 profile 时自动装配。
 * 关键约束：legacy `standalone` profile 与 `local + address.runtime.mode=standalone` 下禁止生效，避免本地单体联调依赖完整微服务基础设施。
 * 异常与副作用：生效后会触发 Dubbo 服务扫描、导出与相关基础设施初始化。
 */
@EnableDubbo
@Configuration
@Conditional(AddressMicroserviceRuntimeCondition.class)
public class AddressDubboBootstrapConfiguration {
}
