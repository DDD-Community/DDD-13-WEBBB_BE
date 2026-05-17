package com.ddd.webbb.ai.domain;

public record CrisisDetectionResult(boolean isCrisis, String matchedKeyword) {

    public static CrisisDetectionResult safe() {
        return new CrisisDetectionResult(false, null);
    }

    public static CrisisDetectionResult crisis(String keyword) {
        return new CrisisDetectionResult(true, keyword);
    }
}
