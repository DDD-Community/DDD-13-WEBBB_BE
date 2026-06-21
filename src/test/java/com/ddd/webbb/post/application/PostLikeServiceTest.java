package com.ddd.webbb.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ddd.webbb.category.domain.BoardCategory;
import com.ddd.webbb.category.domain.BoardCategoryRepository;
import com.ddd.webbb.config.TestRedisConfig;
import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.monster.domain.MonsterRepository;
import com.ddd.webbb.post.domain.CommentTone;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostLikeRepository;
import com.ddd.webbb.post.domain.PostRepository;
import com.ddd.webbb.post.interfaces.dto.LikeResponse;
import com.ddd.webbb.user.domain.User;
import com.ddd.webbb.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestRedisConfig.class)
@Transactional
class PostLikeServiceTest {

    @Autowired private PostLikeService postLikeService;
    @Autowired private PostLikeRepository postLikeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private MonsterRepository monsterRepository;
    @Autowired private BoardCategoryRepository boardCategoryRepository;

    private User user;
    private Post post;
    private Monster monster;

    @BeforeEach
    void setUp() {
        user = userRepository.saveAndFlush(User.create("liker@test.com", "공감러"));
        User author = userRepository.saveAndFlush(User.create("author@test.com", "작성자"));
        BoardCategory category =
                boardCategoryRepository.saveAndFlush(BoardCategory.create("멘탈케어", "기본 카테고리", 0));
        post =
                postRepository.saveAndFlush(
                        Post.create(author, category, "제목", "내용", CommentTone.COMFORT_ME));
        monster = monsterRepository.saveAndFlush(Monster.create(post, EmotionType.ANXIETY, 30));
    }

    @Nested
    @DisplayName("게시글 좋아요 등록")
    class AddPostLike {

        @Test
        @DisplayName("정상 좋아요 → likeCount 증가 + 몬스터 HP 감소")
        void success() {
            LikeResponse response = postLikeService.addPostLike(user.getPublicId(), post.getId());

            assertThat(response.postId()).isEqualTo(post.getId());
            assertThat(response.likeCount()).isEqualTo(1);
            assertThat(response.monster().hp()).isEqualTo(29);
            assertThat(post.getLikeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("중복 좋아요 → ALREADY_LIKED_POST 예외")
        void duplicateLike() {
            postLikeService.addPostLike(user.getPublicId(), post.getId());

            assertThatThrownBy(() -> postLikeService.addPostLike(user.getPublicId(), post.getId()))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.ALREADY_LIKED_POST));
        }

        @Test
        @DisplayName("존재하지 않는 게시글에 좋아요 → POST_NOT_FOUND 예외")
        void postNotFound() {
            assertThatThrownBy(() -> postLikeService.addPostLike(user.getPublicId(), 9999L))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.POST_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("게시글 좋아요 취소")
    class RemovePostLike {

        @Test
        @DisplayName("정상 좋아요 취소 → likeCount 감소")
        void success() {
            postLikeService.addPostLike(user.getPublicId(), post.getId());

            LikeResponse response =
                    postLikeService.removePostLike(user.getPublicId(), post.getId());

            assertThat(response.postId()).isEqualTo(post.getId());
            assertThat(response.likeCount()).isEqualTo(0);
            assertThat(post.getLikeCount()).isEqualTo(0);
            assertThat(postLikeRepository.existsByPostAndUser(post, user)).isFalse();
        }

        @Test
        @DisplayName("좋아요하지 않은 게시글 취소 → POST_LIKE_NOT_FOUND 예외")
        void likeNotFound() {
            assertThatThrownBy(
                            () -> postLikeService.removePostLike(user.getPublicId(), post.getId()))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.POST_LIKE_NOT_FOUND));
        }
    }
}
