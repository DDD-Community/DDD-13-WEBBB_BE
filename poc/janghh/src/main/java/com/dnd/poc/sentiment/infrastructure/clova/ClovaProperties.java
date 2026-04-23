package com.dnd.poc.sentiment.infrastructure.clova;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clova")
public record ClovaProperties(
        String apiKey,
        String apigwKey,
        String endpoint,
        double temperature,
        double topP,
        int maxTokens,
        double repeatPenalty) {}
