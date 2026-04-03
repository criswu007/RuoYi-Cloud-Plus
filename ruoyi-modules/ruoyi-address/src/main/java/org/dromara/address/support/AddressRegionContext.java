package org.dromara.address.support;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.convert.Convert;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 地址模块区域上下文。
 * 目的：统一解析标准地址模块当前请求可用的 `regionId`，避免查询、下拉字典等逻辑各自直接读取登录态扩展字段。
 * 入参/出参：输入显式传入的 `regionId`，输出优先使用显式值、否则回退当前登录上下文的区域 ID。
 * 关键约束：仅解析当前请求上下文中的 `regionId` 扩展字段，不改写登录用户模型，也不引入持久化副作用。
 * 异常与副作用：读取 Sa-Token 扩展字段时若上下文不存在则返回 `null`，无数据库写入副作用。
 */
@Component
public class AddressRegionContext {

    public static final String REGION_ID_KEY = "regionId";

    /**
     * 目的：解析本次业务请求应生效的区域 ID。
     * 入参：显式传入的区域 ID，可为空。
     * 出参：显式区域 ID 优先；为空时回退当前登录态扩展字段中的区域 ID；仍未命中时返回 `null`。
     * 关键约束：显式入参优先级必须高于登录态默认值，避免覆盖调用方主动指定的查询区域。
     * 异常与副作用：若当前线程不存在 Sa-Token 上下文则返回 `null`，无写入副作用。
     */
    public String resolveRegionId(String explicitRegionId) {
        if (StringUtils.isNotBlank(explicitRegionId)) {
            return explicitRegionId;
        }
        return getCurrentRegionId();
    }

    /**
     * 目的：获取当前登录上下文中的默认区域 ID。
     * 入参：无。
     * 出参：当前登录态扩展字段 `regionId`；若未登录或未注入则返回 `null`。
     * 关键约束：只读取 Sa-Token 扩展字段，不推断、不缓存，也不回写登录态。
     * 异常与副作用：Sa-Token 上下文缺失或读取异常时兜底返回 `null`，无写入副作用。
     */
    public String getCurrentRegionId() {
        try {
            return Convert.toStr(StpUtil.getExtra(REGION_ID_KEY), null);
        } catch (Exception ignored) {
            return null;
        }
    }
}
