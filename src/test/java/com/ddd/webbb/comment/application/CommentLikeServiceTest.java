package com.ddd.webbb.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ddd.webbb.category.domain.BoardCategory;
import com.ddd.webbb.category.domain.BoardCategoryRepository;
import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.comment.domain.CommentLikeRepository;
import com.ddd.webbb.comment.domain.CommentRepository;
import com.ddd.webbb.comment.interfaces.dto.CommentLikeResponse;
import com.ddd.webbb.config.TestRedisConfig;
import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.monster.domain.MonsterRepository;
import com.ddd.webbb.post.domain.CommentTone;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostRepository;
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
class CommentLikeServiceTest {

    @Autowired private CommentLikeService commentLikeService;
    @Autowired private CommentRepository commentRepository;
    @Autowired private CommentLikeRepository commentLikeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private MonsterRepository monsterRepository;
    @Autowired private BoardCategoryRepository boardCategoryRepository;

    private User user;
    private User otherUser;
    private Post post;
    private Monster monster;
    private Comment comment;

    @BeforeEach
    void setUp() {
        user = userRepository.saveAndFlush(User.create("liker@test.com", "공감러"));
        otherUser = userRepository.saveAndFlush(User.create("other@test.com", "다른유저"));
        BoardCategory category =
                boardCategoryRepository.saveAndFlush(BoardCategory.create("멘탈케어", "기본 카테고리", 0));
        post =
                postRepository.saveAndFlush(
                        Post.create(user, category, "제목", "내용", CommentTone.COMFORT_ME));
        monster = monsterRepository.saveAndFlush(Monster.create(post, EmotionType.ANXIETY, 30));
        comment = commentRepository.saveAndFlush(Comment.create(post, otherUser, null, "테스트 댓글"));
    }

    @Nested
    @DisplayName("댓글 공감 등록")
    class AddCommentLike {

        @Test
        @DisplayName("정상 공감 → likeCount 증가 + 몬스터 HP 감소")
        void success() {
            // When
            CommentLikeResponse response =
                    commentLikeService.addCommentLike(
                            user.getPublicId(), post.getId(), comment.getId());

            // Then
            assertThat(response.commentId()).isEqualTo(comment.getId());
            assertThat(response.likeCount()).isEqualTo(1);
            assertThat(response.monster().hp()).isEqualTo(29);
            assertThat(comment.getLikeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("중복 공감 → ALREADY_LIKED_COMMENT 예외")
        void duplicateLike() {
            // Given
            commentLikeService.addCommentLike(user.getPublicId(), post.getId(), comment.getId());

            // When / Then
            assertThatThrownBy(
                            () ->
                                    commentLikeService.addCommentLike(
                                            user.getPublicId(), post.getId(), comment.getId()))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.ALREADY_LIKED_COMMENT));
        }

        @Test
        @DisplayName("잘못된 postId로 공감 → COMMENT_POST_MISMATCH 예외")
        void postMismatch() {
            // When / Then
            assertThatThrownBy(
                            () ->
                                    commentLikeService.addCommentLike(
                                            user.getPublicId(), 9999L, comment.getId()))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.COMMENT_POST_MISMATCH));
        }

        @Test
        @DisplayName("존재하지 않는 댓글에 공감 → COMMENT_NOT_FOUND 예외")
        void commentNotFound() {
            // When / Then
            assertThatThrownBy(
                            () ->
                                    commentLikeService.addCommentLike(
                                            user.getPublicId(), post.getId(), 9999L))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.COMMENT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("댓글 공감 취소")
    class RemoveCommentLike {

        @Test
        @DisplayName("정상 공감 취소 → likeCount 감소")
        void success() {
            // Given
            commentLikeService.addCommentLike(user.getPublicId(), post.getId(), comment.getId());
            assertThat(comment.getLikeCount()).isEqualTo(1);

            // When
            commentLikeService.removeCommentLike(user.getPublicId(), post.getId(), comment.getId());

            // Then
            assertThat(comment.getLikeCount()).isEqualTo(0);
            assertThat(commentLikeRepository.existsByCommentAndUser(comment, user)).isFalse();
        }

        @Test
        @DisplayName("공감하지 않은 댓글 취소 → COMMENT_LIKE_NOT_FOUND 예외")
        void notLiked() {
            // When / Then
            assertThatThrownBy(
                            () ->
                                    commentLikeService.removeCommentLike(
                                            user.getPublicId(), post.getId(), comment.getId()))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.COMMENT_LIKE_NOT_FOUND));
        }

        @Test
        @DisplayName("잘못된 postId로 취소 → COMMENT_POST_MISMATCH 예외")
        void postMismatchOnRemove() {
            // Given
            commentLikeService.addCommentLike(user.getPublicId(), post.getId(), comment.getId());

            // When / Then
            assertThatThrownBy(
                            () ->
                                    commentLikeService.removeCommentLike(
                                            user.getPublicId(), 9999L, comment.getId()))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.COMMENT_POST_MISMATCH));
        }
    }
}
