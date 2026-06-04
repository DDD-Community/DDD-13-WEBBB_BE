package com.ddd.webbb.monster.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ddd.webbb.category.domain.BoardCategory;
import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.monster.domain.MonsterHpLogRepository;
import com.ddd.webbb.monster.domain.MonsterRepository;
import com.ddd.webbb.post.domain.CommentTone;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MonsterServiceTest {

    private MonsterRepository monsterRepository;
    private MonsterHpLogRepository monsterHpLogRepository;
    private MonsterService monsterService;

    @BeforeEach
    void setUp() {
        monsterRepository = mock(MonsterRepository.class);
        monsterHpLogRepository = mock(MonsterHpLogRepository.class);
        monsterService = new MonsterService(monsterRepository, monsterHpLogRepository);
    }

    @Test
    void hp가_10이하이면_10으로_저장한다() {
        User user = User.create("ogu@test.com", "ogu");
        BoardCategory category = BoardCategory.create("멘탈케어", "기본 글 작성 카테고리", 0);
        Post post = Post.create(user, category, "title", "content", CommentTone.COMFORT_ME);
        given(monsterRepository.save(any(Monster.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Monster monster = monsterService.addMonster(post, EmotionType.LETHARGY, 7);

        assertThat(monster.getHp()).isEqualTo(10);
        assertThat(monster.getMaxHp()).isEqualTo(10);
    }

    @Test
    void hp가_11이상_20이하이면_20으로_저장한다() {
        User user = User.create("ogu@test.com", "ogu");
        BoardCategory category = BoardCategory.create("멘탈케어", "기본 글 작성 카테고리", 0);
        Post post = Post.create(user, category, "title", "content", CommentTone.WARM_ADVICE);
        given(monsterRepository.save(any(Monster.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Monster monster = monsterService.addMonster(post, EmotionType.IRRITATION, 17);

        assertThat(monster.getHp()).isEqualTo(20);
        assertThat(monster.getMaxHp()).isEqualTo(20);
    }

    @Test
    void hp가_21이상이면_30으로_저장한다() {
        User user = User.create("ogu@test.com", "ogu");
        BoardCategory category = BoardCategory.create("멘탈케어", "기본 글 작성 카테고리", 0);
        Post post = Post.create(user, category, "title", "content", CommentTone.MAKE_ME_LAUGH);
        given(monsterRepository.save(any(Monster.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Monster monster = monsterService.addMonster(post, EmotionType.ANXIETY, 29);

        assertThat(monster.getHp()).isEqualTo(30);
        assertThat(monster.getMaxHp()).isEqualTo(30);
    }
}
