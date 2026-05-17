package com.ddd.webbb.ai.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        String promptVersion, Duration timeout, @DefaultValue Providers providers) {

    public record Providers(
            @DefaultValue("false") boolean claudeEnabled,
            @DefaultValue("true") boolean openaiEnabled) {}
}
