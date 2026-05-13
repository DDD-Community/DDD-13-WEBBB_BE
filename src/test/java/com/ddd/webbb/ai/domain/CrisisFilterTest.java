package com.ddd.webbb.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CrisisFilterTest {

    private CrisisFilter crisisFilter;

    @BeforeEach
    void setUp() {
        crisisFilter = new CrisisFilter();
    }

    @Test
    void 위기_키워드가_없으면_safe를_반환한다() {
        CrisisDetectionResult result = crisisFilter.check("면접에서 떨어져서 속상해요");

        assertThat(result.isCrisis()).isFalse();
        assertThat(result.matchedKeyword()).isNull();
    }

    @Test
    void 죽고_싶다_키워드를_감지한다() {
        CrisisDetectionResult result = crisisFilter.check("정말 죽고 싶어요 더는 못하겠어요");

        assertThat(result.isCrisis()).isTrue();
        assertThat(result.matchedKeyword()).isEqualTo("죽고 싶");
    }

    @Test
    void 자살_키워드를_감지한다() {
        CrisisDetectionResult result = crisisFilter.check("자살 충동이 계속 들어요");

        assertThat(result.isCrisis()).isTrue();
        assertThat(result.matchedKeyword()).isEqualTo("자살");
    }

    @Test
    void null_입력은_safe를_반환한다() {
        CrisisDetectionResult result = crisisFilter.check(null);

        assertThat(result.isCrisis()).isFalse();
    }

    @Test
    void 빈_문자열은_safe를_반환한다() {
        CrisisDetectionResult result = crisisFilter.check("   ");

        assertThat(result.isCrisis()).isFalse();
    }
}
