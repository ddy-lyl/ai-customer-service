package com.ddy.aicustomerservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 接口文档配置
 *
 * 在 Swagger UI 顶部加入 Authorize 按钮，
 * 输入登录返回的 token 后会自动追加 Bearer 前缀。
 *
 * 访问入口：http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI 智能售后客服 + Agent 工单系统 API")
                        .version("1.0.0")
                        .description("基于 Spring Boot 3 + Spring AI + DeepSeek API + RAG 的智能售后客服后端，" +
                                "覆盖：用户认证、AI 多轮对话、知识库检索、工具调用（Function Calling）、" +
                                "工单全生命周期、客服/管理员协同等核心场景")
                        .contact(new Contact().name("AI Customer Service Backend")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("登录成功后将返回的 token 填入此处")));
    }
}
