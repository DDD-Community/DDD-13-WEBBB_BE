package com.dnd.poc.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Sentiment Analysis PoC API")
                                .description("NAVER CLOVA 기반 한국어 감성 분석 PoC")
                                .version("v1"));
    }
}
