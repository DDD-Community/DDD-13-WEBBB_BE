package com.ddd.webbb.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(
                        new Info()
                                .title("WEBBB API")
                                .description("WEBBB 백엔드 API 문서")
                                .version("v1")
                                .contact(
                                        new Contact()
                                                .name("DDD-13 WEBBB")
                                                .url(
                                                        "https://github.com/DDD-Community/DDD-13-WEBBB_BE")))
                .servers(
                        List.of(
                                new Server().url("http://localhost:8080").description("Local"),
                                new Server()
                                        .url("https://api.webbb.site")
                                        .description("Production")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
    }
}
