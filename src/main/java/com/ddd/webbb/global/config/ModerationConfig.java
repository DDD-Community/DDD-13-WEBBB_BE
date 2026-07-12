package com.ddd.webbb.global.config;

import com.ddd.webbb.global.common.moderation.ProfanityFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class ModerationConfig {

    @Bean
    public ProfanityFilter profanityFilter() throws IOException {
        ClassPathResource resource = new ClassPathResource("moderation/banned-words.txt");
        if (!resource.exists()) {
            return new ProfanityFilter(List.of());
        }
        List<String> words =
                resource.getContentAsString(StandardCharsets.UTF_8)
                        .lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .toList();
        return new ProfanityFilter(words);
    }
}
