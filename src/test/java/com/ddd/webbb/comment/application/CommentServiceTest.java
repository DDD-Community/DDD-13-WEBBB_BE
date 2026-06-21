package com.ddd.webbb.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ddd.webbb.category.domain.BoardCategory;
import com.ddd.webbb.category.domain.BoardCategoryRepository;
import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.comment.domain.CommentRepository;
import com.ddd.webbb.comment.interfaces.dto.CommentCreateRequest;
import com.ddd.webbb.comment.interfaces.dto.CommentListResponse;
import com.ddd.webbb.comment.interfaces.dto.CommentResponse;
import com.ddd.webbb.comment.interfaces.dto.CommentUpdateRequest;
import com.ddd.webbb.config.TestRedisConfig;
import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.monster.domain.HpActionType;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.monster.domain.MonsterHpLog;
import com.ddd.webbb.monster.domain.MonsterHpLogRepository;
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
class CommentServiceTest {

    @Autowired private CommentService commentService;
    @Autowired private CommentRepository commentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private MonsterRepository monsterRepository;
    @Autowired private MonsterHpLogRepository monsterHpLogRepository;
    @Autowired private BoardCategoryRepository boardCategoryRepository;

    private User user;
    private User otherUser;
    private Post post;
    private Monster monster;

    @BeforeEach
    void setUp() {
        user = userRepository.saveAndFlush(User.create("test@test.com", "테스터"));
        otherUser = userRepository.saveAndFlush(User.create("other@test.com", "다른유저"));
        BoardCategory category =
                boardCategoryRepository.saveAndFlush(BoardCategory.create("멘탈케어", "기본 카테고리", 0));
        post =
                postRepository.saveAndFlush(
                        Post.create(user, category, "제목", "내용", CommentTone.COMFORT_ME));
        monster = monsterRepository.saveAndFlush(Monster.create(post, EmotionType.ANXIETY, 30));
    }

    @Nested
    @DisplayName("댓글 작성")
    class AddComment {

        @Test
        @DisplayName("루트 댓글 작성 → 댓글 저장 + commentCount 증가 + 몬스터 HP 감소")
        void rootComment() {
            // Given
            CommentCreateRequest request = new CommentCreateRequest(null, "힘내세요!");

            // When
            CommentResponse response =
                    commentService.addComment(user.getPublicId(), post.getId(), request);

            // Then
            assertThat(response.commentId()).isNotNull();
            assertThat(response.postId()).isEqualTo(post.getId());
            assertThat(response.parentCommentId()).isNull();
            assertThat(response.content()).isEqualTo("힘내세요!");
            MonsterHpLog hpLog = monsterHpLogRepository.findAll().get(0);
            assertThat(hpLog.getActionType()).isEqualTo(HpActionType.COMMENT);
            assertThat(hpLog.getHpDelta()).isEqualTo(3);
            assertThat(hpLog.getBeforeHp()).isEqualTo(30);
            assertThat(hpLog.getAfterHp()).isEqualTo(27);
            assertThat(response.monster().hp()).isEqualTo(27);
            assertThat(post.getCommentCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("대댓글 작성 → 부모 댓글 ID 포함 응답")
        void replyComment() {
            // Given
            Comment parent =
                    commentRepository.saveAndFlush(Comment.create(post, otherUser, null, "부모 댓글"));
            CommentCreateRequest request = new CommentCreateRequest(parent.getId(), "대댓글입니다");

            // When
            CommentResponse response =
                    commentService.addComment(user.getPublicId(), post.getId(), request);

            // Then
            assertThat(response.parentCommentId()).isEqualTo(parent.getId());
            assertThat(response.monster().hp()).isEqualTo(27);
        }

        @Test
        @DisplayName("존재하지 않는 부모 댓글 → COMMENT_NOT_FOUND 예외")
        void parentNotFound() {
            // Given
            CommentCreateRequest request = new CommentCreateRequest(9999L, "대댓글");

            // When / Then
            assertThatThrownBy(
                            () ->
                                    commentService.addComment(
                                            user.getPublicId(), post.getId(), request))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.COMMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("다른 게시글의 댓글을 부모로 지정 → INVALID_PARENT_COMMENT 예외")
        void parentFromDifferentPost() {
            // Given
            BoardCategory category =
                    boardCategoryRepository
                            .findFirstByIsActiveTrueOrderBySortOrderAsc()
                            .orElseThrow();
            Post otherPost =
                    postRepository.saveAndFlush(
                            Post.create(user, category, "다른글", "내용", CommentTone.WARM_ADVICE));
            monsterRepository.saveAndFlush(Monster.create(otherPost, EmotionType.LETHARGY, 20));
            Comment otherPostComment =
                    commentRepository.saveAndFlush(
                            Comment.create(otherPost, user, null, "다른 게시글 댓글"));

            CommentCreateRequest request =
                    new CommentCreateRequest(otherPostComment.getId(), "잘못된 대댓글");

            // When / Then
            assertThatThrownBy(
                            () ->
                                    commentService.addComment(
                                            user.getPublicId(), post.getId(), request))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.INVALID_PARENT_COMMENT));
        }

        @Test
        @DisplayName("대댓글의 대댓글 시도 → INVALID_PARENT_COMMENT 예외")
        void nestedReplyNotAllowed() {
            // Given
            Comment root =
                    commentRepository.saveAndFlush(Comment.create(post, user, null, "루트 댓글"));
            Comment reply =
                    commentRepository.saveAndFlush(Comment.create(post, otherUser, root, "대댓글"));

            CommentCreateRequest request = new CommentCreateRequest(reply.getId(), "대대댓글 시도");

            // When / Then
            assertThatThrownBy(
                            () ->
                                    commentService.addComment(
                                            user.getPublicId(), post.getId(), request))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.INVALID_PARENT_COMMENT));
        }
    }

    @Nested
    @DisplayName("댓글 목록 조회")
    class GetComments {

        @Test
        @DisplayName("루트 댓글과 대댓글을 함께 조회")
        void withReplies() {
            // Given
            Comment root =
                    commentRepository.saveAndFlush(Comment.create(post, user, null, "루트 댓글"));
            commentRepository.saveAndFlush(Comment.create(post, otherUser, root, "대댓글"));

            // When
            CommentListResponse response = commentService.getComments(post.getId(), null, 20);

            // Then
            assertThat(response.comments()).hasSize(1);
            assertThat(response.comments().get(0).content()).isEqualTo("루트 댓글");
            assertThat(response.comments().get(0).replies()).hasSize(1);
            assertThat(response.comments().get(0).replies().get(0).content()).isEqualTo("대댓글");
        }

        @Test
        @DisplayName("삭제된 댓글은 조회되지 않음")
        void excludeDeleted() {
            // Given
            Comment comment =
                    commentRepository.saveAndFlush(Comment.create(post, user, null, "삭제될 댓글"));
            comment.delete();
            commentRepository.saveAndFlush(comment);

            // When
            CommentListResponse response = commentService.getComments(post.getId(), null, 20);

            // Then
            assertThat(response.comments()).isEmpty();
        }

        @Test
        @DisplayName("커서 기반 페이지네이션 동작 확인")
        void cursorPagination() {
            // Given
            Comment first = commentRepository.saveAndFlush(Comment.create(post, user, null, "첫번째"));
            commentRepository.saveAndFlush(Comment.create(post, user, null, "두번째"));

            // When
            CommentListResponse response = commentService.getComments(post.getId(), null, 1);

            // Then
            assertThat(response.comments()).hasSize(1);
            assertThat(response.comments().get(0).content()).isEqualTo("두번째");
            assertThat(response.nextCursor()).isNotNull();
        }
    }

    @Nested
    @DisplayName("댓글 수정")
    class ModifyComment {

        @Test
        @DisplayName("본인 댓글 수정 성공")
        void success() {
            // Given
            Comment comment =
                    commentRepository.saveAndFlush(Comment.create(post, user, null, "원본 내용"));
            CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용");

            // When
            CommentResponse response =
                    commentService.modifyComment(user.getPublicId(), comment.getId(), request);

            // Then
            assertThat(response.content()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("타인 댓글 수정 → FORBIDDEN 예외")
        void forbiddenOtherUser() {
            // Given
            Comment comment =
                    commentRepository.saveAndFlush(Comment.create(post, user, null, "원본 내용"));
            CommentUpdateRequest request = new CommentUpdateRequest("수정 시도");

            // When / Then
            assertThatThrownBy(
                            () ->
                                    commentService.modifyComment(
                                            otherUser.getPublicId(), comment.getId(), request))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.FORBIDDEN));
        }

        @Test
        @DisplayName("삭제된 댓글 수정 → COMMENT_NOT_FOUND 예외")
        void deletedComment() {
            // Given
            Comment comment =
                    commentRepository.saveAndFlush(Comment.create(post, user, null, "삭제될 댓글"));
            comment.delete();
            commentRepository.saveAndFlush(comment);
            CommentUpdateRequest request = new CommentUpdateRequest("수정 시도");

            // When / Then
            assertThatThrownBy(
                            () ->
                                    commentService.modifyComment(
                                            user.getPublicId(), comment.getId(), request))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.COMMENT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class RemoveComment {

        @Test
        @DisplayName("본인 댓글 삭제 → 소프트 삭제 + commentCount 감소")
        void success() {
            // Given
            CommentCreateRequest request = new CommentCreateRequest(null, "삭제될 댓글");
            CommentResponse created =
                    commentService.addComment(user.getPublicId(), post.getId(), request);

            // When
            commentService.removeComment(user.getPublicId(), created.commentId());

            // Then
            Comment deleted = commentRepository.findById(created.commentId()).orElseThrow();
            assertThat(deleted.isDeleted()).isTrue();
            assertThat(post.getCommentCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("루트 댓글 삭제 → 대댓글도 함께 소프트 삭제 + commentCount 보정")
        void cascadeDeleteReplies() {
            // Given
            Comment root =
                    commentRepository.saveAndFlush(Comment.create(post, user, null, "루트 댓글"));
            Comment reply1 =
                    commentRepository.saveAndFlush(Comment.create(post, otherUser, root, "대댓글1"));
            Comment reply2 =
                    commentRepository.saveAndFlush(Comment.create(post, otherUser, root, "대댓글2"));
            post.incrementCommentCount();
            post.incrementCommentCount();
            post.incrementCommentCount();

            // When
            commentService.removeComment(user.getPublicId(), root.getId());

            // Then
            assertThat(commentRepository.findById(root.getId()).orElseThrow().isDeleted()).isTrue();
            assertThat(commentRepository.findById(reply1.getId()).orElseThrow().isDeleted())
                    .isTrue();
            assertThat(commentRepository.findById(reply2.getId()).orElseThrow().isDeleted())
                    .isTrue();
            assertThat(post.getCommentCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("대댓글만 삭제 → 해당 대댓글만 소프트 삭제, 루트와 다른 대댓글은 유지")
        void deleteReplyOnly() {
            // Given
            Comment root =
                    commentRepository.saveAndFlush(Comment.create(post, otherUser, null, "루트"));
            Comment reply =
                    commentRepository.saveAndFlush(Comment.create(post, user, root, "삭제할 대댓글"));
            post.incrementCommentCount();
            post.incrementCommentCount();

            // When
            commentService.removeComment(user.getPublicId(), reply.getId());

            // Then
            assertThat(commentRepository.findById(root.getId()).orElseThrow().isDeleted())
                    .isFalse();
            assertThat(commentRepository.findById(reply.getId()).orElseThrow().isDeleted())
                    .isTrue();
            assertThat(post.getCommentCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("루트 삭제 → 레거시 depth>1 대댓글까지 재귀 소프트 삭제")
        void cascadeDeleteLegacyNestedReplies() {
            // Given — 검증 로직 추가 이전에 생성된 depth>1 데이터를 시뮬레이션
            Comment root = commentRepository.saveAndFlush(Comment.create(post, user, null, "루트"));
            Comment depth1 =
                    commentRepository.saveAndFlush(Comment.create(post, otherUser, root, "depth1"));
            Comment depth2 =
                    commentRepository.saveAndFlush(
                            Comment.create(post, otherUser, depth1, "depth2 레거시"));
            post.incrementCommentCount();
            post.incrementCommentCount();
            post.incrementCommentCount();

            // When
            commentService.removeComment(user.getPublicId(), root.getId());

            // Then
            assertThat(commentRepository.findById(root.getId()).orElseThrow().isDeleted()).isTrue();
            assertThat(commentRepository.findById(depth1.getId()).orElseThrow().isDeleted())
                    .isTrue();
            assertThat(commentRepository.findById(depth2.getId()).orElseThrow().isDeleted())
                    .isTrue();
            assertThat(post.getCommentCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("중간 노드 삭제 → 하위 트리만 소프트 삭제, 상위 노드는 유지")
        void deleteMiddleNodeCascadesSubtree() {
            // Given — 레거시 depth>1 데이터에서 중간 노드를 삭제하는 시나리오
            Comment root = commentRepository.saveAndFlush(Comment.create(post, user, null, "루트"));
            Comment middle =
                    commentRepository.saveAndFlush(Comment.create(post, otherUser, root, "중간 노드"));
            Comment leaf =
                    commentRepository.saveAndFlush(Comment.create(post, otherUser, middle, "잎 노드"));
            post.incrementCommentCount();
            post.incrementCommentCount();
            post.incrementCommentCount();

            // When
            commentService.removeComment(otherUser.getPublicId(), middle.getId());

            // Then
            assertThat(commentRepository.findById(root.getId()).orElseThrow().isDeleted())
                    .isFalse();
            assertThat(commentRepository.findById(middle.getId()).orElseThrow().isDeleted())
                    .isTrue();
            assertThat(commentRepository.findById(leaf.getId()).orElseThrow().isDeleted()).isTrue();
            assertThat(post.getCommentCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("타인 댓글 삭제 → FORBIDDEN 예외")
        void forbiddenOtherUser() {
            // Given
            Comment comment =
                    commentRepository.saveAndFlush(Comment.create(post, user, null, "남의 댓글"));

            // When / Then
            assertThatThrownBy(
                            () ->
                                    commentService.removeComment(
                                            otherUser.getPublicId(), comment.getId()))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.FORBIDDEN));
        }

        @Test
        @DisplayName("존재하지 않는 댓글 삭제 → COMMENT_NOT_FOUND 예외")
        void notFound() {
            // When / Then
            assertThatThrownBy(() -> commentService.removeComment(user.getPublicId(), 9999L))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.COMMENT_NOT_FOUND));
        }
    }
}
