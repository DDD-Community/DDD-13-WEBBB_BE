package com.ddd.webbb.ai.domain;

import com.ddd.webbb.emotion.domain.EmotionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record EmotionAnalysisResult(
        @JsonProperty("emotionType") EmotionType emotionType,
        @JsonProperty("hp") int hp,
        @JsonProperty("confidence") double confidence,
        @JsonProperty("reason") String reason,
        @JsonProperty("profanityWords") List<String> profanityWords)
        implements AiResult {

    private static final EmotionAnalysisResult SAFE_DEFAULT =
            new EmotionAnalysisResult(EmotionType.LETHARGY, 10, 0.0, "fallback", List.of());

    public EmotionAnalysisResult {
        profanityWords = profanityWords == null ? List.of() : List.copyOf(profanityWords);
    }

    public static EmotionAnalysisResult safeDefault() {
        return SAFE_DEFAULT;
    }

    @Override
    public boolean isValid() {
        return emotionType != null && (hp == 10 || hp == 20 || hp == 30) && confidence >= 0.0;
    }
}
