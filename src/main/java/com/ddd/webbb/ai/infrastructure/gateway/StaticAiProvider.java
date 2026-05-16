package com.ddd.webbb.ai.infrastructure.gateway;

public class StaticAiProvider implements AiProvider {

    private static final String SAFE_DEFAULT_JSON =
            "{\"emotionType\":\"LETHARGY\",\"hp\":10,\"confidence\":0.0,\"reason\":\"fallback\"}";

    @Override
    public String call(String prompt) {
        return SAFE_DEFAULT_JSON;
    }

    @Override
    public String providerName() {
        return "STATIC";
    }
}
