package com.ddd.webbb.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ddd.webbb.ai.application.AiAnalysisResponse;
import com.ddd.webbb.ai.application.AiAnalysisService;
import com.ddd.webbb.category.domain.BoardCategory;
import com.ddd.webbb.category.domain.BoardCategoryRepository;
import com.ddd.webbb.comment.application.CommentService;
import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.comment.domain.CommentLike;
import com.ddd.webbb.comment.domain.CommentLikeRepository;
import com.ddd.webbb.emotion.application.PostEmotionService;
import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.monster.application.MonsterService;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.post.domain.CommentTone;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostLike;
import com.ddd.webbb.post.domain.PostLikeRepository;
import com.ddd.webbb.post.domain.PostOrder;
import com.ddd.webbb.post.domain.PostQueryRepository;
import com.ddd.webbb.post.domain.PostRepository;
import com.ddd.webbb.post.domain.PostSearchCondition;
import com.ddd.webbb.post.interfaces.dto.PostCreateRequest;
import com.ddd.webbb.post.interfaces.dto.PostCreateResponse;
import com.ddd.webbb.post.interfaces.dto.PostDetailResponse;
import com.ddd.webbb.post.interfaces.dto.PostListResponse;
import com.ddd.webbb.user.application.UserService;
import com.ddd.webbb.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class PostServiceTest {

    private PostRepository postRepository;
    private PostQueryRepository postQueryRepository;
    private PostLikeRepository postLikeRepository;
    private BoardCategoryRepository boardCategoryRepository;
    private UserService userService;
    private AiAnalysisService aiAnalysisService;
    private MonsterService monsterService;
    private PostEmotionService postEmotionService;
    private CommentService commentService;
    private CommentLikeRepository commentLikeRepository;
    private PostService postService;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        postQueryRepository = mock(PostQueryRepository.class);
        postLikeRepository = mock(PostLikeRepository.class);
        boardCategoryRepository = mock(BoardCategoryRepository.class);
        userService = mock(UserService.class);
        aiAnalysisService = mock(AiAnalysisService.class);
        monsterService = mock(MonsterService.class);
        postEmotionService = mock(PostEmotionService.class);
        commentService = mock(CommentService.class);
        commentLikeRepository = mock(CommentLikeRepository.class);
        postService =
                new PostService(
                        postRepository,
                        postQueryRepository,
                        postLikeRepository,
                        boardCategoryRepository,
                        userService,
                        aiAnalysisService,
                        monsterService,
                        postEmotionService,
                        commentService,
                        commentLikeRepository);
    }

    @Test
    void 게시글_목록_조회_필터_조건을_레포지토리에_전달한다() {
        PostSearchCondition condition =
                new PostSearchCondition(
                        List.of("PLANNING", "DESIGN"), List.of("YEAR_3"), PostOrder.LATEST);
        given(
                        postQueryRepository.findByCursor(
                                eq(null), eq(null), eq(20), any(PostSearchCondition.class)))
                .willReturn(List.of());

        postService.getPosts(null, 20, condition);

        ArgumentCaptor<PostSearchCondition> conditionCaptor =
                ArgumentCaptor.forClass(PostSearchCondition.class);
        verify(postQueryRepository)
                .findByCursor(eq(null), eq(null), eq(20), conditionCaptor.capture());
        assertThat(conditionCaptor.getValue().jobRoles()).containsExactly("PLANNING", "DESIGN");
        assertThat(conditionCaptor.getValue().careerYears()).containsExactly("YEAR_3");
        assertThat(conditionCaptor.getValue().order()).isEqualTo(PostOrder.LATEST);
    }

    @Test
    void 인기순_조회는_커서_게시글의_좋아요수를_함께_전달한다() {
        UUID cursorAuthorId = UUID.randomUUID();
        User author = User.create("cursor@test.com", "cursor");
        ReflectionTestUtils.setField(author, "publicId", cursorAuthorId);
        BoardCategory category = BoardCategory.create("멘탈케어", "기본 글 작성 카테고리", 0);
        Post cursorPost = Post.create(author, category, "기준 글", "기준 글", CommentTone.COMFORT_ME);
        cursorPost.incrementLikeCount();
        cursorPost.incrementLikeCount();
        cursorPost.incrementLikeCount();
        cursorPost.incrementLikeCount();
        cursorPost.incrementLikeCount();
        ReflectionTestUtils.setField(cursorPost, "id", 99L);

        PostSearchCondition condition =
                new PostSearchCondition(List.of(), List.of(), PostOrder.POPULAR);
        given(postRepository.findByIdAndIsDeletedFalse(99L))
                .willReturn(java.util.Optional.of(cursorPost));
        given(postQueryRepository.findByCursor(eq(99L), eq(5), eq(20), eq(condition)))
                .willReturn(List.of());

        postService.getPosts(99L, 20, condition);

        verify(postQueryRepository).findByCursor(eq(99L), eq(5), eq(20), eq(condition));
    }

    @Test
    void 로그인한_사용자의_좋아요_여부를_목록에_반영한다() {
        UUID viewerId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        User viewer = User.create("viewer@test.com", "viewer");
        ReflectionTestUtils.setField(viewer, "publicId", viewerId);
        User author = User.create("author@test.com", "author");
        ReflectionTestUtils.setField(author, "publicId", authorId);
        ReflectionTestUtils.setField(author, "jobType", "DEVELOPMENT");
        ReflectionTestUtils.setField(author, "careerLevel", "YEAR_3");
        BoardCategory category = BoardCategory.create("멘탈케어", "기본 글 작성 카테고리", 0);
        Post post = Post.create(author, category, "제목", "내용", CommentTone.COMFORT_ME);
        ReflectionTestUtils.setField(post, "id", 1L);
        Monster monster = Monster.create(post, EmotionType.ANXIETY, 30);
        com.ddd.webbb.emotion.domain.PostEmotion postEmotion =
                mock(com.ddd.webbb.emotion.domain.PostEmotion.class);
        given(postEmotion.getPost()).willReturn(post);
        given(postEmotion.getEmotionType()).willReturn(EmotionType.ANXIETY);

        given(userService.getUserEntity(viewerId)).willReturn(viewer);
        given(
                        postQueryRepository.findByCursor(
                                eq(null), eq(null), eq(20), any(PostSearchCondition.class)))
                .willReturn(List.of(post));
        given(postEmotionService.findByPostIds(List.of(1L))).willReturn(List.of(postEmotion));
        given(monsterService.findByPostIds(List.of(1L))).willReturn(List.of(monster));
        given(postLikeRepository.findByPost_IdInAndUser(List.of(1L), viewer))
                .willReturn(List.of(PostLike.create(post, viewer)));

        PostListResponse response =
                postService.getPosts(viewerId, null, 20, PostSearchCondition.empty());

        assertThat(response.posts()).hasSize(1);
        assertThat(response.posts().get(0).likedByMe()).isTrue();
    }

    @Test
    void 게시글_상세에_댓글_작성자_메타와_공감여부를_포함한다() {
        UUID viewerId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID commentAuthorId = UUID.randomUUID();
        User viewer = User.create("viewer@test.com", "viewer");
        ReflectionTestUtils.setField(viewer, "publicId", viewerId);
        User author = User.create("author@test.com", "author");
        ReflectionTestUtils.setField(author, "publicId", authorId);
        ReflectionTestUtils.setField(author, "jobType", "DEVELOPMENT");
        ReflectionTestUtils.setField(author, "careerLevel", "YEAR_3");
        User commentAuthor = User.create("helper@test.com", "helper");
        ReflectionTestUtils.setField(commentAuthor, "publicId", commentAuthorId);
        ReflectionTestUtils.setField(commentAuthor, "jobType", "PLANNING");
        ReflectionTestUtils.setField(commentAuthor, "careerLevel", "YEAR_1");
        BoardCategory category = BoardCategory.create("멘탈케어", "기본 글 작성 카테고리", 0);
        Post post = Post.create(author, category, "제목", "내용", CommentTone.COMFORT_ME);
        ReflectionTestUtils.setField(post, "id", 1L);
        Comment comment = Comment.create(post, commentAuthor, null, "힘내세요!");
        ReflectionTestUtils.setField(comment, "id", 10L);
        comment.incrementLikeCount();
        comment.incrementLikeCount();
        Monster monster = Monster.create(post, EmotionType.ANXIETY, 30);
        com.ddd.webbb.emotion.domain.PostEmotion postEmotion =
                mock(com.ddd.webbb.emotion.domain.PostEmotion.class);
        given(postEmotion.getEmotionType()).willReturn(EmotionType.ANXIETY);

        given(userService.getUserEntity(viewerId)).willReturn(viewer);
        given(postRepository.findByIdAndIsDeletedFalse(1L)).willReturn(java.util.Optional.of(post));
        given(postEmotionService.findByPost(1L)).willReturn(postEmotion);
        given(monsterService.findByPost(1L)).willReturn(monster);
        given(commentService.findCommentsByPost(1L)).willReturn(List.of(comment));
        given(postLikeRepository.existsByPostAndUser(post, viewer)).willReturn(true);
        given(commentLikeRepository.findByComment_IdInAndUser(List.of(10L), viewer))
                .willReturn(List.of(CommentLike.create(comment, viewer)));

        PostDetailResponse response = postService.getPostDetail(viewerId, 1L);

        assertThat(response.likedByMe()).isTrue();
        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().get(0).authorId()).isEqualTo(commentAuthorId.toString());
        assertThat(response.comments().get(0).jobRole()).isEqualTo("PLANNING");
        assertThat(response.comments().get(0).careerYear()).isEqualTo("YEAR_1");
        assertThat(response.comments().get(0).likedByMe()).isTrue();
    }

    @Test
    void 글과_몬스터와_감정정보를_저장하고_응답을_반환한다() {
        UUID userId = UUID.randomUUID();
        User user = User.create("ogu@test.com", "ogu");
        ReflectionTestUtils.setField(user, "publicId", userId);
        ReflectionTestUtils.setField(user, "jobType", "DEVELOPMENT");
        ReflectionTestUtils.setField(user, "careerLevel", "YEAR_3");
        BoardCategory category = BoardCategory.create("멘탈케어", "기본 글 작성 카테고리", 0);

        Post savedPost =
                Post.create(
                        user,
                        category,
                        "면접이 계속 떨어져서 불안해요.",
                        "면접이 계속 떨어져서 불안해요.",
                        CommentTone.COMFORT_ME);
        ReflectionTestUtils.setField(savedPost, "id", 1L);
        ReflectionTestUtils.setField(savedPost, "createdAt", LocalDateTime.of(2026, 5, 20, 22, 0));

        Monster monster = Monster.create(savedPost, EmotionType.ANXIETY, 30);

        given(userService.getUserEntity(userId)).willReturn(user);
        given(boardCategoryRepository.findFirstByIsActiveTrueOrderBySortOrderAsc())
                .willReturn(java.util.Optional.of(category));
        given(postRepository.save(any(Post.class))).willReturn(savedPost);
        given(aiAnalysisService.analyze(any()))
                .willReturn(new AiAnalysisResponse("ANXIETY", 30, 0.8, "불안", false, "OPENAI"));
        given(monsterService.addMonster(savedPost, EmotionType.ANXIETY, 30)).willReturn(monster);

        PostCreateResponse response =
                postService.addPost(
                        userId, new PostCreateRequest("면접이 계속 떨어져서 불안해요.", CommentTone.COMFORT_ME));

        assertThat(response.postId()).isEqualTo(1L);
        assertThat(response.content()).isEqualTo("면접이 계속 떨어져서 불안해요.");
        assertThat(response.commentTone()).isEqualTo(CommentTone.COMFORT_ME);
        assertThat(response.emotion().type()).isEqualTo("ANXIETY");
        assertThat(response.monster().type()).isEqualTo("ANXIETY_MONSTER");
        assertThat(response.monster().hp()).isEqualTo(30);
        assertThat(response.author().nickname()).isEqualTo("ogu");

        verify(monsterService).addMonster(savedPost, EmotionType.ANXIETY, 30);
        verify(postEmotionService).addPostEmotion(savedPost, EmotionType.ANXIETY, user);
    }

    @Test
    void AI_폴백_결과도_정상적으로_반영한다() {
        UUID userId = UUID.randomUUID();
        User user = User.create("ogu@test.com", "ogu");
        ReflectionTestUtils.setField(user, "publicId", userId);
        BoardCategory category = BoardCategory.create("멘탈케어", "기본 글 작성 카테고리", 0);

        Post savedPost =
                Post.create(user, category, "그냥 다 지쳐요.", "그냥 다 지쳐요.", CommentTone.VENT_WITH_ME);
        ReflectionTestUtils.setField(savedPost, "id", 10L);

        Monster monster = Monster.create(savedPost, EmotionType.LETHARGY, 10);

        given(userService.getUserEntity(userId)).willReturn(user);
        given(boardCategoryRepository.findFirstByIsActiveTrueOrderBySortOrderAsc())
                .willReturn(java.util.Optional.of(category));
        given(postRepository.save(any(Post.class))).willReturn(savedPost);
        given(aiAnalysisService.analyze(any()))
                .willReturn(
                        new AiAnalysisResponse("LETHARGY", 10, 0.0, "fallback", false, "STATIC"));
        given(monsterService.addMonster(savedPost, EmotionType.LETHARGY, 10)).willReturn(monster);

        PostCreateResponse response =
                postService.addPost(
                        userId, new PostCreateRequest("그냥 다 지쳐요.", CommentTone.VENT_WITH_ME));

        assertThat(response.emotion().type()).isEqualTo("LETHARGY");
        assertThat(response.monster().type()).isEqualTo("LETHARGY_MONSTER");
        verify(postEmotionService)
                .addPostEmotion(eq(savedPost), eq(EmotionType.LETHARGY), eq(user));
    }

    @Test
    void 작성자는_게시글을_소프트삭제할_수_있다() {
        UUID userId = UUID.randomUUID();
        User user = User.create("ogu@test.com", "ogu");
        ReflectionTestUtils.setField(user, "publicId", userId);
        BoardCategory category = BoardCategory.create("멘탈케어", "기본 글 작성 카테고리", 0);
        Post post = Post.create(user, category, "삭제할 글", "삭제할 글", CommentTone.COMFORT_ME);

        given(userService.getUserEntity(userId)).willReturn(user);
        given(postRepository.findByIdAndIsDeletedFalse(1L)).willReturn(java.util.Optional.of(post));

        postService.deletePost(userId, 1L);

        assertThat(post.isDeleted()).isTrue();
    }

    @Test
    void 타인_게시글은_삭제할_수_없다() {
        UUID userId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        User loginUser = User.create("ogu@test.com", "ogu");
        ReflectionTestUtils.setField(loginUser, "publicId", userId);
        User author = User.create("other@test.com", "other");
        ReflectionTestUtils.setField(author, "publicId", authorId);
        BoardCategory category = BoardCategory.create("멘탈케어", "기본 글 작성 카테고리", 0);
        Post post = Post.create(author, category, "남의 글", "남의 글", CommentTone.WARM_ADVICE);

        given(userService.getUserEntity(userId)).willReturn(loginUser);
        given(postRepository.findByIdAndIsDeletedFalse(1L)).willReturn(java.util.Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(userId, 1L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void 이미_삭제되었거나_없는_게시글은_404다() {
        UUID userId = UUID.randomUUID();
        User user = User.create("ogu@test.com", "ogu");
        ReflectionTestUtils.setField(user, "publicId", userId);

        given(userService.getUserEntity(userId)).willReturn(user);
        given(postRepository.findByIdAndIsDeletedFalse(99L)).willReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(userId, 99L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }
}
