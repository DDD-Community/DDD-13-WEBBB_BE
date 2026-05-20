package com.ddd.webbb.emotion.domain;

public enum EmotionType {
    ANXIETY("불안", "걱정과 긴장으로 마음이 무거운 상태"),
    LETHARGY("무기력", "지치고 의욕이 떨어져 움직이기 힘든 상태"),
    LONELINESS("외로움", "혼자 견디고 있다는 감정이 큰 상태"),
    SELF_DEPRECATION("자기비하", "스스로를 낮추고 자책하는 감정이 큰 상태"),
    IRRITATION("짜증", "예민함과 답답함이 쌓여 날카로워진 상태");

    private final String displayName;
    private final String summary;

    EmotionType(String displayName, String summary) {
        this.displayName = displayName;
        this.summary = summary;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSummary() {
        return summary;
    }

    public String monsterType() {
        return switch (this) {
            case ANXIETY -> "ANXIETY_MONSTER";
            case LETHARGY -> "LETHARGY_MONSTER";
            case LONELINESS -> "LONELINESS_MONSTER";
            case SELF_DEPRECATION -> "SELF_DEPRECATION_MONSTER";
            case IRRITATION -> "IRRITATION_MONSTER";
        };
    }
}
