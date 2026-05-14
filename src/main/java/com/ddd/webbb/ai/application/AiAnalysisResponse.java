package com.ddd.webbb.ai.application;

public record AiAnalysisResponse(
        String emotionType,
        int hp,
        double confidence,
        String reason,
        boolean crisisDetected,
        String usedProvider) {}
