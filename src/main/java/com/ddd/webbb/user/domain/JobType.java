package com.ddd.webbb.user.domain;

public enum JobType {
    PLANNING("기획"),
    DESIGN("디자인"),
    DEVELOPMENT("개발"),
    MARKETING("마케팅"),
    SALES("영업"),
    HR("인사"),
    GENERAL_AFFAIRS("총무"),
    PRODUCTION("생산"),
    ACCOUNTING("회계"),
    OTHER("기타");

    private final String displayName;

    JobType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
