package com.ddd.webbb.ai.infrastructure.adapter;

import com.ddd.webbb.ai.domain.EmotionAnalysisResult;
import com.ddd.webbb.ai.domain.EmotionAnalyzer;
import com.ddd.webbb.ai.domain.PostContent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MAX_VALUE)
public class StaticFallbackAnalyzer implements EmotionAnalyzer {

    @Override
    public EmotionAnalysisResult analyze(PostContent content) {
        return EmotionAnalysisResult.safeDefault();
    }

    @Override
    public String providerName() {
        return "STATIC";
    }
}
