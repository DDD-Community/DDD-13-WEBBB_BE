package com.dnd.poc.retrospective.domain;

public record RetrospectiveContext(String value) {

    public static final int MAX_LENGTH = 2000;

    public RetrospectiveContext {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("context must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "context exceeds max length: " + value.length() + " > " + MAX_LENGTH);
        }
    }

    public int length() {
        return value.length();
    }
}
