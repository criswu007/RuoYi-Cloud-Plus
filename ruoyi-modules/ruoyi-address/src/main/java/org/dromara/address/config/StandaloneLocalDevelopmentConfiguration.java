package org.dromara.address.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.core.service.DictService;
import org.dromara.common.core.enums.UserType;
import org.dromara.address.config.condition.AddressStandaloneRuntimeCondition;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.model.LoginUser;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * standalone 本地联调配置。
 * 目的：为地址模块提供脱离 Redis、网关和统一登录体系的本地运行基线，便于先把标准地址主链路跑通。
 * 关键约束：仅在 legacy `standalone` profile 或 `local + address.runtime.mode=standalone` 时生效，不能影响真实微服务环境。
 * 异常与副作用：会在本地请求进入时自动注入一个开发态登录态，仅用于当前单体联调。
 */
@Conditional(AddressStandaloneRuntimeCondition.class)
@Configuration
public class StandaloneLocalDevelopmentConfiguration {

    private static final String DEFAULT_STANDALONE_REGION_ID = "000102140000000021128049";

    private static final String STANDALONE_REGION_PROPERTY = "address.standalone.region-id";

    private static final String STANDALONE_REGION_ENV = "ADDRESS_STANDALONE_REGION_ID";

    private static final String SYS_NORMAL_DISABLE_DICT_TYPE = "sys_normal_disable";

    private static final Map<String, LinkedHashMap<String, String>> STANDALONE_DICT_MAPPINGS = initStandaloneDictMappings();

    private static final Set<String> STANDALONE_PERMISSIONS = Set.of(
        "address:standard:list",
        "address:standard:query",
        "address:standard:add",
        "address:standard:edit",
        "address:standard:remove",
        "address:standard:merge",
        "address:standard:split",
        "address:standard:export",
        "address:standard:import",
        "address:import:record:list",
        "address:import:record:query",
        "address:import:record:export",
        "address:selection:query",
        "address:selection:create",
        "address:tag:list",
        "address:tag:query",
        "address:tag:add",
        "address:tag:edit",
        "address:tag:remove",
        "address:tag:bind",
        "address:station:list",
        "address:station:query",
        "address:station:add",
        "address:station:edit",
        "address:station:remove",
        "address:attribute:list",
        "address:attribute:query",
        "address:attribute:add",
        "address:attribute:edit",
        "address:attribute:remove",
        "address:installation:list",
        "address:installation:query",
        "address:installation:add",
        "address:installation:edit",
        "address:installation:remove",
        "address:monitor:rule:list",
        "address:monitor:rule:query",
        "address:monitor:rule:add",
        "address:monitor:rule:edit",
        "address:monitor:rule:remove",
        "address:monitor:task:query",
        "address:monitor:task:add",
        "address:monitor:task:edit",
        "address:monitor:task:execute",
        "address:monitor:record:list",
        "address:monitor:record:query",
        "address:monitor:record:edit",
        "address:monitor:record:remove",
        "address:monitor:workOrder:list",
        "address:monitor:workOrder:query",
        "address:monitor:workOrder:add",
        "address:monitor:workOrder:edit",
        "address:search:maintain",
        "address:operation:log:list",
        "address:operation:log:query"
    );

    /**
     * 目的：构造 standalone 模式下可直接使用的最小字典映射集。
     * 入参：无。
     * 出参：字典类型 -> (字典值 -> 字典标签) 的有序映射。
     * 关键约束：仅维护地址模块当前导入导出链路所需最小字典，避免与系统中心字典定义产生分叉。
     * 异常与副作用：无外部副作用，仅初始化当前进程内存常量。
     */
    private static Map<String, LinkedHashMap<String, String>> initStandaloneDictMappings() {
        LinkedHashMap<String, String> normalDisable = new LinkedHashMap<>();
        normalDisable.put("0", "正常");
        normalDisable.put("1", "停用");
        return Map.of(SYS_NORMAL_DISABLE_DICT_TYPE, normalDisable);
    }

    /**
     * 目的：解析 standalone 模式下默认注入的区域 ID。
     * 入参：无。
     * 出参：联调请求默认使用的 `regionId`。
     * 关键约束：优先使用显式环境变量 / 系统属性；若未显式指定，则固定回退到线上库可用的南京市区区域编码。
     * 异常与副作用：无持久化副作用，仅影响 standalone 自动登录态的默认区域过滤范围。
     */
    private String resolveStandaloneRegionId() {
        String configuredRegionId = resolveValue(STANDALONE_REGION_PROPERTY, STANDALONE_REGION_ENV);
        if (configuredRegionId != null) {
            return configuredRegionId;
        }
        return DEFAULT_STANDALONE_REGION_ID;
    }

    /**
     * 目的：统一读取 standalone 相关的系统属性与环境变量。
     * 入参：`propertyKey` 为 JVM 系统属性名，`envKey` 为环境变量名。
     * 出参：命中的配置值；若都未设置则返回 `null`。
     * 关键约束：字符串仅做非空判断，不做格式校验；调用方负责决定默认值。
     * 异常与副作用：无。
     */
    private String resolveValue(String propertyKey, String envKey) {
        if (propertyKey != null) {
            String propertyValue = System.getProperty(propertyKey);
            if (propertyValue != null && !propertyValue.isBlank()) {
                return propertyValue;
            }
        }
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return null;
    }

    /**
     * 目的：在 standalone 模式下将 Sa-Token 存储切换为内存实现。
     * 入参：无。
     * 出参：本地内存版 `SaTokenDao`。
     * 关键约束：只覆盖 standalone 场景，避免真实环境继续依赖本地内存会话。
     * 异常与副作用：会把 token 数据保存在当前进程内存中，应用重启后即丢失。
     */
    @Primary
    @Bean
    public SaTokenDao standaloneSaTokenDao() {
        return new SaTokenDaoDefaultImpl();
    }

    /**
     * 目的：为 standalone 本地联调提供字典服务兜底实现，避免导入导出依赖远程字典中心。
     * 入参：无。
     * 出参：本地内存版 `DictService`。
     * 关键约束：只覆盖 standalone profile；未知字典类型或未命中字典项时需原样返回，保证链路可继续。
     * 异常与副作用：无持久化副作用，仅使用当前进程内存字典映射。
     */
    @Primary
    @Bean
    public DictService standaloneDictService() {
        return new DictService() {
            @Override
            public String getDictLabel(String dictType, String dictValue, String separator) {
                return translateStandaloneDictValue(dictType, dictValue, separator, false);
            }

            @Override
            public String getDictValue(String dictType, String dictLabel, String separator) {
                return translateStandaloneDictValue(dictType, dictLabel, separator, true);
            }

            @Override
            public Map<String, String> getAllDictByDictType(String dictType) {
                LinkedHashMap<String, String> mapping = STANDALONE_DICT_MAPPINGS.get(dictType);
                if (mapping == null) {
                    return Map.of();
                }
                return new LinkedHashMap<>(mapping);
            }
        };
    }

    /**
     * 目的：按 standalone 预置字典执行标签/值双向转换。
     * 入参：`dictType` 字典类型、`source` 原始值、`separator` 分隔符、`reverse` 是否标签转值。
     * 出参：转换后的字典结果；未命中映射时返回原值。
     * 关键约束：仅基于内存映射转换，不触发任何远程调用；多值场景按传入分隔符拆分并逐项转换。
     * 异常与副作用：无外部副作用，不写数据库。
     */
    private String translateStandaloneDictValue(String dictType, String source, String separator, boolean reverse) {
        if (source == null || source.isBlank()) {
            return source;
        }
        LinkedHashMap<String, String> valueToLabel = STANDALONE_DICT_MAPPINGS.get(dictType);
        if (valueToLabel == null || valueToLabel.isEmpty()) {
            return source;
        }
        Map<String, String> mapping = reverse ? buildReverseMapping(valueToLabel) : valueToLabel;
        String actualSeparator = (separator == null || separator.isBlank()) ? "," : separator;
        if (!source.contains(actualSeparator)) {
            return mapping.getOrDefault(source, source);
        }
        return Arrays.stream(source.split(Pattern.quote(actualSeparator)))
            .map(String::trim)
            .map(item -> mapping.getOrDefault(item, item))
            .collect(Collectors.joining(actualSeparator));
    }

    /**
     * 目的：将值->标签映射转换为标签->值映射，支持导入反向转换。
     * 入参：`valueToLabel` 值->标签映射。
     * 出参：标签->值映射。
     * 关键约束：标签重复时保留首次映射，避免同标签覆盖导致不可预期的反向解析。
     * 异常与副作用：无。
     */
    private Map<String, String> buildReverseMapping(Map<String, String> valueToLabel) {
        LinkedHashMap<String, String> labelToValue = new LinkedHashMap<>();
        valueToLabel.forEach((value, label) -> labelToValue.putIfAbsent(label, value));
        return labelToValue;
    }

    /**
     * 目的：为本地联调请求自动补一个开发态登录上下文。
     * 入参：无。
     * 出参：标准 Servlet Filter 注册对象。
     * 关键约束：仅处理 `/address/*` 路径，且只在当前请求未登录时注入 mock 登录态。
     * 异常与副作用：会向当前线程和 Sa-Token 会话中写入一个固定开发用户。
     */
    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> standaloneMockLoginFilter() {
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
        // 需在 Sa-Token Servlet Filter 建立请求上下文后再补登录态，否则会触发上下文未初始化异常。
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        registration.addUrlPatterns("/address/*");
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
                if (!LoginHelper.isLogin()) {
                    LoginUser loginUser = new LoginUser();
                    loginUser.setTenantId("000000");
                    loginUser.setUserId(1L);
                    loginUser.setDeptId(1L);
                    loginUser.setDeptCategory("standalone");
                    loginUser.setDeptName("本地联调");
                    loginUser.setUsername("standalone-dev");
                    loginUser.setNickname("本地联调用户");
                    loginUser.setUserType(UserType.SYS_USER.getUserType());
                    loginUser.setMenuPermission(new LinkedHashSet<>(STANDALONE_PERMISSIONS));
                    loginUser.setRolePermission(Set.of("standalone-dev"));
                    LoginHelper.login(loginUser, new SaLoginParameter().setExtra("regionId", resolveStandaloneRegionId()));
                    response.setHeader("Authorization", StpUtil.getTokenValue());
                }
                filterChain.doFilter(request, response);
            }
        });
        return registration;
    }
}
