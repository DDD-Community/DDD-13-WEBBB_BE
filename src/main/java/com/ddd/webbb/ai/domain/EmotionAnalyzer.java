package com.ddd.webbb.ai.domain;

public interface EmotionAnalyzer {

    EmotionAnalysisResult analyze(PostContent content);

    String providerName();
}
