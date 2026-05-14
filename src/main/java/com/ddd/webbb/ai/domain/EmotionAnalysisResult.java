package com.ddd.webbb.ai.domain;

import com.ddd.webbb.emotion.domain.EmotionType;
import com.fasterxml.jackson.annotation.JsonProperty;

public record EmotionAnalysisResult(
        @JsonProperty("emotionType") EmotionType emotionType,
        @JsonProperty("hp") int hp,
        @JsonProperty("confidence") double confidence,
        @JsonProperty("reason") String reason) {
    private static final EmotionAnalysisResult SAFE_DEFAULT =
            new EmotionAnalysisResult(EmotionType.LETHARGY, 10, 0.0, "fallback");

    public static EmotionAnalysisResult safeDefault() {
        return SAFE_DEFAULT;
    }

    public boolean isValid() {
        return emotionType != null && (hp == 10 || hp == 20 || hp == 30) && confidence >= 0.0;
    }
}
