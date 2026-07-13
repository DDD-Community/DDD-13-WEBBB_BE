package com.ddd.webbb.ai.application;

import java.util.List;

public record AiAnalysisResponse(
        String emotionType,
        int hp,
        double confidence,
        String reason,
        List<String> profanityWords,
        boolean crisisDetected,
        String usedProvider) {}
