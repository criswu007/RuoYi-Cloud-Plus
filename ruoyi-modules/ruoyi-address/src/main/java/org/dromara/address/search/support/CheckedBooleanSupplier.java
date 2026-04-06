package org.dromara.address.search.support;

/**
 * 可抛检异常的布尔供应器。
 * <p>
 * 目的：统一承载双写模板中的 ES 写入、DB 写入和补偿动作，允许调用方直接透传受检异常。
 * 关键约束：实现方必须返回明确的成功/失败布尔值，返回 `false` 将被同步服务视为失败分支处理。
 * 异常与副作用：由实现方决定；接口本身不执行 I/O。
 * </p>
 */
@FunctionalInterface
public interface CheckedBooleanSupplier {

    /**
     * 目的：执行一个可能抛异常的布尔动作。
     * 入参：无。
     * 出参：`true` 表示动作成功，`false` 表示动作执行但结果失败。
     * 关键约束：调用方会把 `false` 与异常都视为失败并进入补偿或终止流程。
     * 异常与副作用：允许实现方抛出异常，并携带外部写操作副作用。
     *
     * @return 动作是否成功
     * @throws Exception 动作执行异常
     */
    boolean getAsBoolean() throws Exception;
}
