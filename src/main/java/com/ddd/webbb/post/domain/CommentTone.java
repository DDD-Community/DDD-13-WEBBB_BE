package com.ddd.webbb.post.domain;

public enum CommentTone {
    VENT_WITH_ME("대신 욕해주기"),
    COMFORT_ME("무조건 위로해주기"),
    WARM_ADVICE("따뜻한 조언해주기"),
    MAKE_ME_LAUGH("웃겨주기");

    private final String displayName;

    CommentTone(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
