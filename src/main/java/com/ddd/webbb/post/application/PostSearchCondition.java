package com.ddd.webbb.post.application;

import java.util.List;

public record PostSearchCondition(List<String> jobRoles, List<String> careerYears) {

    public PostSearchCondition {
        jobRoles = normalize(jobRoles);
        careerYears = normalize(careerYears);
    }

    public static PostSearchCondition empty() {
        return new PostSearchCondition(List.of(), List.of());
    }

    private static List<String> normalize(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }
}
