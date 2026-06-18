package com.ddd.webbb.user.domain;

public enum CareerLevel {
    NEWCOMER("신입"),
    YEAR_1("1년차"),
    YEAR_2("2년차"),
    YEAR_3("3년차"),
    YEAR_4("4년차"),
    YEAR_5("5년차"),
    YEAR_6("6년차"),
    YEAR_7_PLUS("7년차 이상");

    private final String displayName;

    CareerLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
