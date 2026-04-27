package com.dnd.poc.retrospective.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI retrospectiveOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Retrospective AI API")
                .version("0.0.1")
                .description("취준생의 감정 텍스트를 KPT(Keep/Problem/Try) + 응원 메시지로 구조화하는 AI 회고 도우미.")
                .contact(new Contact().name("DDD-13 WEBBB").url("https://github.com/DDD-Community/DDD-13-WEBBB_BE"))
                .license(new License().name("Internal PoC")));
    }
}
