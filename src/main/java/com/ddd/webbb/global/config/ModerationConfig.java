package com.ddd.webbb.global.config;

import com.ddd.webbb.global.common.moderation.ProfanityFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class ModerationConfig {

    private static final Logger log = LoggerFactory.getLogger(ModerationConfig.class);

    @Bean
    public ProfanityFilter profanityFilter() {
        ClassPathResource resource = new ClassPathResource("moderation/banned-words.txt");
        if (!resource.exists()) {
            return new ProfanityFilter(List.of());
        }
        try {
            List<String> words =
                    resource.getContentAsString(StandardCharsets.UTF_8)
                            .lines()
                            .map(String::trim)
                            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                            .toList();
            return new ProfanityFilter(words);
        } catch (IOException e) {
            log.warn("[Moderation] 금칙어 사전 로드 실패, 마스킹 없이 기동합니다: {}", e.getMessage());
            return new ProfanityFilter(List.of());
        }
    }
}
