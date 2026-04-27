package com.dnd.poc.retrospective.domain;

import java.util.List;
import java.util.Objects;

public record KptAnalysis(
        List<String> keep,
        List<String> problem,
        List<String> tries,
        String cheerMessage
) {
    public KptAnalysis {
        Objects.requireNonNull(keep, "keep must not be null");
        Objects.requireNonNull(problem, "problem must not be null");
        Objects.requireNonNull(tries, "tries must not be null");
        Objects.requireNonNull(cheerMessage, "cheerMessage must not be null");
        keep = List.copyOf(keep);
        problem = List.copyOf(problem);
        tries = List.copyOf(tries);
        if (cheerMessage.isBlank()) {
            throw new IllegalArgumentException("cheerMessage must not be blank");
        }
    }

    public boolean isEmpty() {
        return keep.isEmpty() && problem.isEmpty() && tries.isEmpty();
    }
}
