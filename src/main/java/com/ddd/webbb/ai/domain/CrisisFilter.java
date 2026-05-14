package com.ddd.webbb.ai.domain;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CrisisFilter {

    private static final List<String> CRISIS_KEYWORDS =
            List.of("죽고 싶", "자살", "자해", "스스로 목숨", "삶을 끝", "죽어버리고 싶");

    public CrisisDetectionResult check(String text) {
        if (text == null || text.isBlank()) {
            return CrisisDetectionResult.safe();
        }
        return CRISIS_KEYWORDS.stream()
                .filter(text::contains)
                .findFirst()
                .map(CrisisDetectionResult::crisis)
                .orElseGet(CrisisDetectionResult::safe);
    }
}
