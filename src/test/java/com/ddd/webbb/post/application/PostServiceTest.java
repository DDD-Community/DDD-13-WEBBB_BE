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
import com.ddd.webbb.emotion.application.PostEmotionService;
import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.monster.application.MonsterService;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.post.domain.CommentTone;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostRepository;
import com.ddd.webbb.post.infrastructure.PostRepositoryImpl;
import com.ddd.webbb.post.interfaces.dto.PostCreateRequest;
import com.ddd.webbb.post.interfaces.dto.PostCreateResponse;
import com.ddd.webbb.user.application.UserService;
import com.ddd.webbb.user.domain.User;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PostServiceTest {

    private PostRepository postRepository;
    private PostRepositoryImpl postRepositoryImpl;
    private BoardCategoryRepository boardCategoryRepository;
    private UserService userService;
    private AiAnalysisService aiAnalysisService;
    private MonsterService monsterService;
    private PostEmotionService postEmotionService;
    private CommentService commentService;
    private PostService postService;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        postRepositoryImpl = mock(PostRepositoryImpl.class);
        boardCategoryRepository = mock(BoardCategoryRepository.class);
        userService = mock(UserService.class);
        aiAnalysisService = mock(AiAnalysisService.class);
        monsterService = mock(MonsterService.class);
        postEmotionService = mock(PostEmotionService.class);
        commentService = mock(CommentService.class);
        postService =
                new PostService(
                        postRepository,
                        postRepositoryImpl,
                        boardCategoryRepository,
                        userService,
                        aiAnalysisService,
                        monsterService,
                        postEmotionService,
                        commentService);
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
