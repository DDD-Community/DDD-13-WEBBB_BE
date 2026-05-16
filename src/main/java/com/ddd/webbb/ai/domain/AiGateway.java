package com.ddd.webbb.ai.domain;

public interface AiGateway {
    AiGatewayResult call(String prompt);
}
