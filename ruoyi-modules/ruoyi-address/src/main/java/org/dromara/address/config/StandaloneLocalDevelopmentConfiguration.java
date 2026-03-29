package org.dromara.address.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.model.LoginUser;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * standalone 本地联调配置。
 * 目的：为地址模块提供脱离 Redis、网关和统一登录体系的本地运行基线，便于先把标准地址主链路跑通。
 * 关键约束：仅在 `standalone` profile 生效，不能影响 dev/prod 的真实鉴权与存储行为。
 * 异常与副作用：会在本地请求进入时自动注入一个开发态登录态，仅用于当前单体联调。
 */
@Profile("standalone")
@Configuration
public class StandaloneLocalDevelopmentConfiguration {

    private static final Set<String> STANDALONE_PERMISSIONS = Set.of(
        "address:standard:list",
        "address:standard:query",
        "address:standard:add",
        "address:standard:edit",
        "address:standard:remove",
        "address:standard:export",
        "address:tag:list",
        "address:tag:query",
        "address:tag:bind"
    );

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
                    LoginHelper.login(loginUser, new SaLoginParameter());
                    response.setHeader("Authorization", StpUtil.getTokenValue());
                }
                filterChain.doFilter(request, response);
            }
        });
        return registration;
    }
}
