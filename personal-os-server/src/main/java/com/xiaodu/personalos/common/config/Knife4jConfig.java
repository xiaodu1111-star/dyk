package com.xiaodu.personalos.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / SpringDoc OpenAPI 配置。
 *
 * <p>文档访问地址：{@code http://localhost:8080/doc.html}</p>
 *
 * @author Kou
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI personalOsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Personal OS API")
                        .description("自托管个人管理系统 · 登录闭环（P0）")
                        .version("v1.0")
                        .contact(new Contact().name("小杜")));
    }
}
