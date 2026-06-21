package com.ddd.webbb.post.domain;

import java.util.List;

public record PostSearchCondition(
        List<String> jobRoles, List<String> careerYears, PostOrder order) {

    public PostSearchCondition {
        jobRoles = normalize(jobRoles);
        careerYears = normalize(careerYears);
        order = order == null ? PostOrder.LATEST : order;
    }

    public static PostSearchCondition empty() {
        return new PostSearchCondition(List.of(), List.of(), PostOrder.LATEST);
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
