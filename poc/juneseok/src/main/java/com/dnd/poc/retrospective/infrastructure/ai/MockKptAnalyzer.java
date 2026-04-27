package com.dnd.poc.retrospective.infrastructure.ai;

import com.dnd.poc.retrospective.application.port.KptAnalyzer;
import com.dnd.poc.retrospective.config.AiConfig;
import com.dnd.poc.retrospective.domain.KptAnalysis;
import com.dnd.poc.retrospective.domain.RetrospectiveContext;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnExpression(AiConfig.MOCK_KEY_CONDITION)
class MockKptAnalyzer implements KptAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(MockKptAnalyzer.class);

    @PostConstruct
    void warnOnStartup() {
        log.warn("==========================================================================");
        log.warn(" OPENAI_API_KEY not configured — running with MOCK KptAnalyzer.");
        log.warn(" Responses are static, NOT real LLM output. DO NOT deploy to production.");
        log.warn("==========================================================================");
    }

    @Override
    public KptAnalysis analyze(RetrospectiveContext context) {
        log.debug("Mock analyze invoked. length={}", context.length());
        return new KptAnalysis(
                List.of(
                        "끝까지 자리에 앉아 시도를 멈추지 않은 점",
                        "본인 부족한 부분을 솔직히 인지하고 표현한 점"
                ),
                List.of(
                        "시간 분배 전략이 정해져 있지 않음",
                        "어려운 문제에서 손을 떼는 시점을 정해두지 않음"
                ),
                List.of(
                        "내일 30분 동안 약점 영역 1챕터 복습하기",
                        "타이머 두고 1문제 25분 룰 적용해보기"
                ),
                "오늘도 시도한 것만으로 충분히 잘하고 계세요. 한 걸음씩 가요."
        );
    }

    @Override
    public boolean isMock() {
        return true;
    }
}
