package com.xiaodu.personalos.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * 跨域配置。
 *
 * <p>使用 {@link CorsFilter}（而非 MVC 层配置），确保 OPTIONS 预检请求在
 * 进入 Sa-Token 拦截器之前就被正确处理。</p>
 *
 * <p><b>安全约定</b>：必须使用显式来源白名单（{@link CorsConfiguration#setAllowedOrigins}），
 * 严禁使用 {@code addAllowedOriginPattern("*")} 配合 {@code allowCredentials=true} —— 那等于
 * 允许任意站点携带凭证跨域调用。白名单可通过 {@code personal-os.cors.allowed-origins} 配置。</p>
 *
 * @author Kou
 */
@Configuration
public class CorsConfig {

    /** 允许跨域的来源白名单，逗号分隔。 */
    @Value("${personal-os.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        // 显式来源白名单；非白名单来源不会回显 Access-Control-Allow-Origin
        config.setAllowedOrigins(origins);
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
