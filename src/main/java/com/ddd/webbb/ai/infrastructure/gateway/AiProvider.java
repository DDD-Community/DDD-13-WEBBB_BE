package com.ddd.webbb.ai.infrastructure.gateway;

interface AiProvider {
    String call(String prompt);

    String providerName();
}
