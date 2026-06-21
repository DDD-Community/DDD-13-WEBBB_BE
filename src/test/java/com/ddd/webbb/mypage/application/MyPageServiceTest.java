package com.ddd.webbb.mypage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ddd.webbb.category.domain.BoardCategory;
import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.mypage.domain.MyPageReadRepository;
import com.ddd.webbb.mypage.interfaces.dto.MonsterStatsResponse;
import com.ddd.webbb.mypage.interfaces.dto.MyCommentResponse;
import com.ddd.webbb.mypage.interfaces.dto.MyPostResponse;
import com.ddd.webbb.post.domain.CommentTone;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.user.application.UserService;
import com.ddd.webbb.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MyPageServiceTest {

    private UserService userService;
    private MyPageReadRepository myPageReadRepository;
    private MyPageService myPageService;

    private Post post;
    private User user;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        myPageReadRepository = mock(MyPageReadRepository.class);
        myPageService = new MyPageService(userService, myPageReadRepository);

        user = User.create("ogu@test.com", "오구");
        BoardCategory category = BoardCategory.create("멘탈케어", "기본 카테고리", 0);
        post = Post.create(user, category, "title", "content", CommentTone.COMFORT_ME);
    }

    @Test
    @DisplayName("페이지 크기가 100을 초과하면 INVALID_INPUT 예외를 반환한다")
    void sizeOverLimit_throwsInvalidInput() {
        assertThatThrownBy(() -> myPageService.getMyPosts(UUID.randomUUID(), null, 101))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("내 게시글 조회는 size+1 결과를 바탕으로 nextCursor를 계산한다")
    void myPosts_returnsNextCursorWhenHasNext() {
        Post newestPost =
                Post.create(user, post.getCategory(), "new", "최신 글", CommentTone.COMFORT_ME);
        ReflectionTestUtils.setField(newestPost, "id", 2L);
        ReflectionTestUtils.setField(newestPost, "createdAt", LocalDateTime.of(2026, 6, 21, 10, 0));

        Post olderPost =
                Post.create(user, post.getCategory(), "old", "이전 글", CommentTone.COMFORT_ME);
        ReflectionTestUtils.setField(olderPost, "id", 1L);
        ReflectionTestUtils.setField(olderPost, "createdAt", LocalDateTime.of(2026, 6, 20, 10, 0));

        Monster monster = Monster.create(newestPost, EmotionType.ANXIETY, 10);

        given(userService.getUserEntity(any(UUID.class))).willReturn(user);
        given(myPageReadRepository.findMyPosts(user, null, 1))
                .willReturn(List.of(newestPost, olderPost));
        given(myPageReadRepository.findMonstersByPostIds(List.of(2L))).willReturn(List.of(monster));

        MyPostResponse response = myPageService.getMyPosts(UUID.randomUUID(), null, 1);

        assertThat(response.posts()).hasSize(1);
        assertThat(response.posts().get(0).postId()).isEqualTo(2L);
        assertThat(response.nextCursor()).isEqualTo(2L);
    }

    @Test
    @DisplayName("게시글 내용이 null이어도 미리보기 생성에서 예외가 발생하지 않는다")
    void myPosts_handlesNullContentPreview() {
        Post nullContentPost =
                Post.create(user, post.getCategory(), "title", "temp", CommentTone.COMFORT_ME);
        ReflectionTestUtils.setField(nullContentPost, "id", 3L);
        ReflectionTestUtils.setField(nullContentPost, "content", null);

        given(userService.getUserEntity(any(UUID.class))).willReturn(user);
        given(myPageReadRepository.findMyPosts(user, null, 20))
                .willReturn(List.of(nullContentPost));
        given(myPageReadRepository.findMonstersByPostIds(List.of(3L))).willReturn(List.of());

        MyPostResponse response = myPageService.getMyPosts(UUID.randomUUID(), null, 20);

        assertThat(response.posts()).hasSize(1);
        assertThat(response.posts().get(0).contentPreview()).isEmpty();
    }

    @Test
    @DisplayName("내 댓글 조회는 size+1 결과를 바탕으로 nextCursor를 계산한다")
    void myComments_returnsNextCursorWhenHasNext() {
        Comment newestComment = Comment.create(post, user, null, "최신 댓글");
        ReflectionTestUtils.setField(newestComment, "id", 2L);
        ReflectionTestUtils.setField(
                newestComment, "createdAt", LocalDateTime.of(2026, 6, 21, 10, 0));

        Comment olderComment = Comment.create(post, user, null, "이전 댓글");
        ReflectionTestUtils.setField(olderComment, "id", 1L);
        ReflectionTestUtils.setField(
                olderComment, "createdAt", LocalDateTime.of(2026, 6, 20, 10, 0));

        given(userService.getUserEntity(any(UUID.class))).willReturn(user);
        given(myPageReadRepository.findMyComments(user, null, 1))
                .willReturn(List.of(newestComment, olderComment));

        MyCommentResponse response = myPageService.getMyComments(UUID.randomUUID(), null, 1);

        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().get(0).commentId()).isEqualTo(2L);
        assertThat(response.nextCursor()).isEqualTo(2L);
    }

    @Test
    @DisplayName("몬스터가 없으면 전체 0, 물리친 0, 최빈 감정 null을 반환한다")
    void noMonsters_returnsZeroAndNullEmotion() {
        given(userService.getUserEntity(any(UUID.class))).willReturn(user);
        given(myPageReadRepository.findMonstersByUserId(any())).willReturn(List.of());

        MonsterStatsResponse result = myPageService.getMonsterStats(UUID.randomUUID());

        assertThat(result.totalMonsterCount()).isEqualTo(0);
        assertThat(result.defeatedMonsterCount()).isEqualTo(0);
        assertThat(result.mostFrequentEmotion()).isNull();
    }

    @Test
    @DisplayName("물리친 몬스터 수를 정확히 집계한다")
    void countsDefeatedMonstersCorrectly() {
        given(userService.getUserEntity(any(UUID.class))).willReturn(user);

        Monster alive = Monster.create(post, EmotionType.ANXIETY, 10);
        Monster dead1 = Monster.create(post, EmotionType.LETHARGY, 10);
        dead1.decreaseHp(10);
        Monster dead2 = Monster.create(post, EmotionType.IRRITATION, 10);
        dead2.decreaseHp(10);
        given(myPageReadRepository.findMonstersByUserId(any()))
                .willReturn(List.of(alive, dead1, dead2));

        MonsterStatsResponse result = myPageService.getMonsterStats(UUID.randomUUID());

        assertThat(result.totalMonsterCount()).isEqualTo(3);
        assertThat(result.defeatedMonsterCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("가장 많이 나타난 감정 타입과 비율을 반환한다")
    void returnsMostFrequentEmotionWithPercentage() {
        given(userService.getUserEntity(any(UUID.class))).willReturn(user);

        Monster a1 = Monster.create(post, EmotionType.ANXIETY, 10);
        Monster a2 = Monster.create(post, EmotionType.ANXIETY, 10);
        Monster a3 = Monster.create(post, EmotionType.ANXIETY, 10);
        Monster l1 = Monster.create(post, EmotionType.LETHARGY, 10);
        Monster l2 = Monster.create(post, EmotionType.LETHARGY, 10);
        given(myPageReadRepository.findMonstersByUserId(any()))
                .willReturn(List.of(a1, a2, a3, l1, l2));

        MonsterStatsResponse result = myPageService.getMonsterStats(UUID.randomUUID());

        assertThat(result.mostFrequentEmotion()).isNotNull();
        assertThat(result.mostFrequentEmotion().type()).isEqualTo("ANXIETY");
        assertThat(result.mostFrequentEmotion().displayName()).isEqualTo("불안");
        assertThat(result.mostFrequentEmotion().count()).isEqualTo(3);
        assertThat(result.mostFrequentEmotion().percentage()).isEqualTo(60);
    }
}
