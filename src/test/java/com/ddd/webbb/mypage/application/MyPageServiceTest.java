package com.ddd.webbb.mypage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ddd.webbb.category.domain.BoardCategory;
import com.ddd.webbb.comment.domain.CommentQueryRepository;
import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.monster.application.MonsterService;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.mypage.interfaces.dto.MonsterStatsResponse;
import com.ddd.webbb.post.domain.CommentTone;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostQueryRepository;
import com.ddd.webbb.user.application.UserService;
import com.ddd.webbb.user.domain.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MyPageServiceTest {

    private MonsterService monsterService;
    private UserService userService;
    private PostQueryRepository postQueryRepository;
    private CommentQueryRepository commentQueryRepository;
    private MyPageService myPageService;

    private Post post;

    @BeforeEach
    void setUp() {
        monsterService = mock(MonsterService.class);
        userService = mock(UserService.class);
        postQueryRepository = mock(PostQueryRepository.class);
        commentQueryRepository = mock(CommentQueryRepository.class);
        myPageService =
                new MyPageService(
                        monsterService, userService, postQueryRepository, commentQueryRepository);

        User user = User.create("ogu@test.com", "오구");
        BoardCategory category = BoardCategory.create("멘탈케어", "기본 카테고리", 0);
        post = Post.create(user, category, "title", "content", CommentTone.COMFORT_ME);
    }

    @Test
    @DisplayName("몬스터가 없으면 전체 0, 물리친 0, 최빈 감정 null을 반환한다")
    void noMonsters_returnsZeroAndNullEmotion() {
        User user = User.create("ogu@test.com", "오구");
        given(userService.getUserEntity(any(UUID.class))).willReturn(user);
        given(monsterService.findByUserId(any())).willReturn(List.of());

        MonsterStatsResponse result = myPageService.getMonsterStats(UUID.randomUUID());

        assertThat(result.totalMonsterCount()).isEqualTo(0);
        assertThat(result.defeatedMonsterCount()).isEqualTo(0);
        assertThat(result.mostFrequentEmotion()).isNull();
    }

    @Test
    @DisplayName("물리친 몬스터 수를 정확히 집계한다")
    void countsDefeatedMonstersCorrectly() {
        User user = User.create("ogu@test.com", "오구");
        given(userService.getUserEntity(any(UUID.class))).willReturn(user);

        Monster alive = Monster.create(post, EmotionType.ANXIETY, 10);
        Monster dead1 = Monster.create(post, EmotionType.LETHARGY, 10);
        dead1.decreaseHp(10);
        Monster dead2 = Monster.create(post, EmotionType.IRRITATION, 10);
        dead2.decreaseHp(10);
        given(monsterService.findByUserId(any())).willReturn(List.of(alive, dead1, dead2));

        MonsterStatsResponse result = myPageService.getMonsterStats(UUID.randomUUID());

        assertThat(result.totalMonsterCount()).isEqualTo(3);
        assertThat(result.defeatedMonsterCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("가장 많이 나타난 감정 타입과 비율을 반환한다")
    void returnsMostFrequentEmotionWithPercentage() {
        User user = User.create("ogu@test.com", "오구");
        given(userService.getUserEntity(any(UUID.class))).willReturn(user);

        Monster a1 = Monster.create(post, EmotionType.ANXIETY, 10);
        Monster a2 = Monster.create(post, EmotionType.ANXIETY, 10);
        Monster a3 = Monster.create(post, EmotionType.ANXIETY, 10);
        Monster l1 = Monster.create(post, EmotionType.LETHARGY, 10);
        Monster l2 = Monster.create(post, EmotionType.LETHARGY, 10);
        given(monsterService.findByUserId(any())).willReturn(List.of(a1, a2, a3, l1, l2));

        MonsterStatsResponse result = myPageService.getMonsterStats(UUID.randomUUID());

        assertThat(result.mostFrequentEmotion()).isNotNull();
        assertThat(result.mostFrequentEmotion().type()).isEqualTo("ANXIETY");
        assertThat(result.mostFrequentEmotion().displayName()).isEqualTo("불안");
        assertThat(result.mostFrequentEmotion().count()).isEqualTo(3);
        assertThat(result.mostFrequentEmotion().percentage()).isEqualTo(60);
    }
}
