package com.ddd.webbb.post.application;

import com.ddd.webbb.ai.application.AiAnalysisResponse;
import com.ddd.webbb.ai.application.AiAnalysisService;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.category.domain.BoardCategory;
import com.ddd.webbb.category.domain.BoardCategoryRepository;
import com.ddd.webbb.comment.application.CommentService;
import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.emotion.application.PostEmotionService;
import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.emotion.domain.PostEmotion;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.monster.application.MonsterService;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostRepository;
import com.ddd.webbb.post.infrastructure.PostRepositoryImpl;
import com.ddd.webbb.post.interfaces.dto.PostCreateRequest;
import com.ddd.webbb.post.interfaces.dto.PostCreateResponse;
import com.ddd.webbb.post.interfaces.dto.PostDetailResponse;
import com.ddd.webbb.post.interfaces.dto.PostListResponse;
import com.ddd.webbb.post.interfaces.dto.PostListResponse.MonsterInfo;
import com.ddd.webbb.post.interfaces.dto.PostListResponse.PostSummary;
import com.ddd.webbb.post.interfaces.dto.PostResponse;
import com.ddd.webbb.user.application.UserService;
import com.ddd.webbb.user.domain.User;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PostService {

    private static final String DEFAULT_CATEGORY_NAME = "멘탈케어";
    private static final int CONTENT_PREVIEW_LENGTH = 50;

    private final PostRepository postRepository;
    private final PostRepositoryImpl postRepositoryImpl;
    private final BoardCategoryRepository boardCategoryRepository;
    private final UserService userService;
    private final AiAnalysisService aiAnalysisService;
    private final MonsterService monsterService;
    private final PostEmotionService postEmotionService;
    private final CommentService commentService;

    public PostService(
            PostRepository postRepository,
            PostRepositoryImpl postRepositoryImpl,
            BoardCategoryRepository boardCategoryRepository,
            UserService userService,
            AiAnalysisService aiAnalysisService,
            MonsterService monsterService,
            PostEmotionService postEmotionService,
            CommentService commentService) {
        this.postRepository = postRepository;
        this.postRepositoryImpl = postRepositoryImpl;
        this.boardCategoryRepository = boardCategoryRepository;
        this.userService = userService;
        this.aiAnalysisService = aiAnalysisService;
        this.monsterService = monsterService;
        this.postEmotionService = postEmotionService;
        this.commentService = commentService;
    }

    public Post getPostEntity(Long postId) {
        return postRepository
                .findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
    }

    public PostCreateResponse addPost(UUID userPublicId, PostCreateRequest request) {
        User user = userService.getUserEntity(userPublicId);
        BoardCategory category = resolveDefaultCategory();
        String title = buildDefaultTitle(request.content());
        Post post =
                postRepository.save(
                        Post.create(
                                user, category, title, request.content(), request.commentTone()));

        AiAnalysisResponse analysis =
                aiAnalysisService.analyze(new PostContent(post.getId(), post.getContent()));
        EmotionType emotionType = EmotionType.valueOf(analysis.emotionType());

        Monster monster = monsterService.addMonster(post, emotionType, analysis.hp());
        postEmotionService.addPostEmotion(post, emotionType, user);

        return PostCreateResponse.of(post, user, emotionType, monster);
    }

    @Transactional(readOnly = true)
    public PostListResponse getPosts(Long cursor, int size) {
        List<Post> fetched = postRepositoryImpl.findByCursor(cursor, size);
        boolean hasNext = fetched.size() > size;
        List<Post> posts = hasNext ? fetched.subList(0, size) : fetched;

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Map<Long, PostEmotion> emotionMap =
                postEmotionService.findByPostIds(postIds).stream()
                        .collect(Collectors.toMap(e -> e.getPost().getId(), e -> e));
        Map<Long, Monster> monsterMap =
                monsterService.findByPostIds(postIds).stream()
                        .collect(Collectors.toMap(m -> m.getPost().getId(), m -> m));

        List<PostSummary> summaries =
                posts.stream()
                        .map(
                                post ->
                                        toPostSummary(
                                                post,
                                                emotionMap.get(post.getId()),
                                                monsterMap.get(post.getId())))
                        .toList();

        Long nextCursor = hasNext ? posts.get(posts.size() - 1).getId() : null;
        return new PostListResponse(summaries, nextCursor);
    }

    public PostDetailResponse getPostDetail(Long postId) {
        Post post =
                postRepository
                        .findByIdAndIsDeletedFalse(postId)
                        .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        post.incrementViewCount();

        PostEmotion postEmotion = postEmotionService.findByPost(postId);
        Monster monster = monsterService.findByPost(postId);
        List<Comment> comments = commentService.findCommentsByPost(postId);

        EmotionType emotionType = postEmotion.getEmotionType();
        return new PostDetailResponse(
                post.getId(),
                new PostDetailResponse.AuthorInfo(
                        post.getUser().getPublicId().toString(),
                        post.getUser().getNickname(),
                        post.getUser().getJobType(),
                        post.getUser().getCareerLevel()),
                post.getContent(),
                post.getCommentTone().name(),
                new PostDetailResponse.EmotionInfo(
                        emotionType.name(), emotionType.getDisplayName()),
                new PostDetailResponse.MonsterInfo(
                        emotionType.monsterType(),
                        monster.getHp(),
                        monster.getMaxHp(),
                        monster.getStatus().name()),
                post.getLikeCount(),
                post.getCommentCount(),
                comments.stream()
                        .map(
                                c ->
                                        new PostDetailResponse.CommentInfo(
                                                c.getId(),
                                                c.getUser().getNickname(),
                                                c.getContent(),
                                                c.getCreatedAt()))
                        .toList(),
                post.getCreatedAt());
    }

    public PostResponse modifyPost(UUID userPublicId, Long postId, PostCreateRequest request) {
        User user = userService.getUserEntity(userPublicId);
        Post post =
                postRepository
                        .findByIdAndIsDeletedFalse(postId)
                        .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUser().getPublicId().equals(user.getPublicId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        boolean contentChanged = !post.getContent().equals(request.content());
        post.update(request.content(), request.commentTone());

        if (contentChanged) {
            AiAnalysisResponse analysis =
                    aiAnalysisService.analyze(new PostContent(post.getId(), post.getContent()));
            EmotionType emotionType = EmotionType.valueOf(analysis.emotionType());
            postEmotionService.modifyPostEmotion(postId, emotionType);
            monsterService.resetMonster(postId, emotionType, analysis.hp());
        }

        PostEmotion postEmotion = postEmotionService.findByPost(postId);
        Monster monster = monsterService.findByPost(postId);
        return PostResponse.of(post, user, postEmotion, monster);
    }

    public void deletePost(UUID userPublicId, Long postId) {
        User user = userService.getUserEntity(userPublicId);
        Post post =
                postRepository
                        .findByIdAndIsDeletedFalse(postId)
                        .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUser().getPublicId().equals(user.getPublicId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        post.delete();
    }

    private PostSummary toPostSummary(Post post, PostEmotion postEmotion, Monster monster) {
        EmotionType emotionType = postEmotion != null ? postEmotion.getEmotionType() : null;
        String emotionTypeName = emotionType != null ? emotionType.name() : null;
        MonsterInfo monsterInfo =
                monster != null
                        ? new MonsterInfo(
                                emotionType != null ? emotionType.monsterType() : null,
                                monster.getHp(),
                                monster.getMaxHp(),
                                monster.getStatus().name())
                        : null;
        String preview =
                post.getContent().length() <= CONTENT_PREVIEW_LENGTH
                        ? post.getContent()
                        : post.getContent().substring(0, CONTENT_PREVIEW_LENGTH) + "...";
        return new PostSummary(
                post.getId(),
                post.getUser().getNickname(),
                post.getUser().getJobType(),
                post.getUser().getCareerLevel(),
                preview,
                emotionTypeName,
                monsterInfo,
                post.getLikeCount(),
                post.getCommentCount(),
                post.getCreatedAt());
    }

    private BoardCategory resolveDefaultCategory() {
        return boardCategoryRepository
                .findFirstByIsActiveTrueOrderBySortOrderAsc()
                .orElseGet(
                        () ->
                                boardCategoryRepository.save(
                                        BoardCategory.create(
                                                DEFAULT_CATEGORY_NAME, "기본 글 작성 카테고리", 0)));
    }

    private String buildDefaultTitle(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            return "고민글";
        }
        return normalized.substring(0, Math.min(normalized.length(), 30));
    }
}
