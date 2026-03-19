package com.quiz.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 配置
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("在线答题系统 API")
                        .description("在线答题系统接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Quiz AI Online")
                                .url("https://github.com/quiz-ai-online")
                        )
                );
    }

    /**
     * APP 端接口分组
     */
    @Bean
    public GroupedOpenApi appApi() {
        return GroupedOpenApi.builder()
                .group("APP端接口")
                .pathsToMatch("/api/app/**")
                .build();
    }

    /**
     * Admin 后台接口分组
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("Admin后台接口")
                .pathsToMatch("/api/admin/**")
                .build();
    }
}
