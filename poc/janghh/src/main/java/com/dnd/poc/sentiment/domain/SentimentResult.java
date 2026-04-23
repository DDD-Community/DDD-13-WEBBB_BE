package com.dnd.poc.sentiment.domain;

import java.util.List;

public record SentimentResult(
        SentimentLabel label,
        double score,
        double confidence,
        List<String> keywords,
        String reason) {}
