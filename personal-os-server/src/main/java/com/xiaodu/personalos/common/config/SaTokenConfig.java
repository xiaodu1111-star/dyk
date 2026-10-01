package com.xiaodu.personalos.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 鉴权配置：拦截 {@code /api/**} 做全局登录校验。
 *
 * <p>对齐《backend-architecture.md》4.4 节：拦截器（全局登录校验）+ 注解（细粒度权限）双保险。
 * 登录接口与 Knife4j 文档相关路径放行。</p>
 *
 * @author Kou
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /** 无需登录即可访问的路径。 */
    private static final String[] EXCLUDE_PATHS = {
            "/api/auth/login",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/favicon.ico",
            "/error"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns(EXCLUDE_PATHS);
    }
}
