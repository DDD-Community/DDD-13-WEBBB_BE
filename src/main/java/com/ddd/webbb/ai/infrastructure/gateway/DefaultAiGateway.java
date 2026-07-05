package com.ddd.webbb.ai.infrastructure.gateway;

import com.ddd.webbb.ai.domain.AiGateway;
import com.ddd.webbb.ai.domain.AiGatewayResult;
import com.ddd.webbb.ai.domain.exception.AiErrorCode;
import com.ddd.webbb.ai.domain.exception.PermanentAiException;
import com.ddd.webbb.ai.domain.exception.RetryableAiException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultAiGateway implements AiGateway {

    private static final Logger log = LoggerFactory.getLogger(DefaultAiGateway.class);

    private final List<AiProvider> providers;

    public DefaultAiGateway(List<AiProvider> providers) {
        this.providers = providers;
    }

    @Override
    public AiGatewayResult call(String prompt) {
        for (AiProvider provider : providers) {
            try {
                String raw = provider.call(prompt);
                return new AiGatewayResult(raw, provider.providerName());
            } catch (PermanentAiException e) {
                log.warn(
                        "[AI] provider={} permanent failure, skipping: {}",
                        provider.providerName(),
                        e.getMessage());
            } catch (Exception e) {
                log.warn(
                        "[AI] provider={} transient failure, skipping: {}",
                        provider.providerName(),
                        e.getMessage());
            }
        }
        throw new RetryableAiException(AiErrorCode.SERVICE_UNAVAILABLE, "모든 AI 프로바이더 호출 실패");
    }
}
