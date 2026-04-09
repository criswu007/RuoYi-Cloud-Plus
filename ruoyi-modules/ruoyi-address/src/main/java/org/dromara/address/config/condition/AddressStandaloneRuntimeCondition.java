package org.dromara.address.config.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 地址 standalone 运行条件。
 * 目的：控制地址模块本地 standalone 能力的装配时机。
 * 入参/出参：输入 Spring 条件上下文，输出是否匹配 standalone 运行方式。
 * 关键约束：需同时兼容 legacy `standalone` profile 与 `local + address.runtime.mode=standalone`。
 * 异常与副作用：无外部副作用，仅参与 Spring 条件装配。
 */
public class AddressStandaloneRuntimeCondition implements Condition {

    /**
     * 目的：判断当前 Spring 上下文是否应装配地址 standalone 能力。
     * 入参：条件上下文与注解元数据。
     * 出参：命中 standalone 运行方式时返回 `true`。
     * 关键约束：判定逻辑统一委托给 `AddressRuntimeMode`，避免多处复制条件。
     * 异常与副作用：无。
     *
     * @param context Spring 条件上下文
     * @param metadata 注解元数据
     * @return 是否匹配
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return AddressRuntimeMode.isStandalone(context.getEnvironment());
    }
}
