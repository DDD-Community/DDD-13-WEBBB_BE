package com.ddd.webbb.global.common.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfanityFilterTest {

    private ProfanityFilter profanityFilter;

    @BeforeEach
    void setUp() {
        profanityFilter = new ProfanityFilter(List.of("바보", "바보멍청이", "쓰레기"));
    }

    @Test
    void 금칙어를_글자_수만큼_별표로_치환한다() {
        assertThat(profanityFilter.mask("이 바보 같은 회사")).isEqualTo("이 ** 같은 회사");
    }

    @Test
    void 여러_금칙어를_모두_치환한다() {
        assertThat(profanityFilter.mask("바보 쓰레기 회사")).isEqualTo("** *** 회사");
    }

    @Test
    void 포함_관계인_단어는_긴_단어를_우선_치환한다() {
        assertThat(profanityFilter.mask("이 바보멍청이야")).isEqualTo("이 *****야");
    }

    @Test
    void 금칙어가_없으면_원문을_그대로_반환한다() {
        assertThat(profanityFilter.mask("좋은 하루 보내세요")).isEqualTo("좋은 하루 보내세요");
    }

    @Test
    void null_입력은_그대로_반환한다() {
        assertThat(profanityFilter.mask(null)).isNull();
    }

    @Test
    void 빈_문자열은_그대로_반환한다() {
        assertThat(profanityFilter.mask("   ")).isEqualTo("   ");
    }
}
