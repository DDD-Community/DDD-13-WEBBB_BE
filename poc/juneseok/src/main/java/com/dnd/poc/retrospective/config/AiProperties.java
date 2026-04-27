package com.dnd.poc.retrospective.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "retrospective.ai")
public record AiProperties(
        @NotBlank String promptLocation,
        @NotNull @Positive Duration timeout
) {
}
