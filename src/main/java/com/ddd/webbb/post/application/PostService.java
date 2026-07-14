package com.ddd.webbb.post.application;

import com.ddd.webbb.ai.application.AiAnalysisResponse;
import com.ddd.webbb.ai.application.AiAnalysisService;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.category.domain.BoardCategory;
import com.ddd.webbb.category.domain.BoardCategoryRepository;
import com.ddd.webbb.comment.application.CommentService;
import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.comment.domain.CommentLikeRepository;
import com.ddd.webbb.emotion.application.PostEmotionService;
import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.emotion.domain.PostEmotion;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.global.common.moderation.ProfanityFilter;
import com.ddd.webbb.monster.application.MonsterService;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostLikeRepository;
import com.ddd.webbb.post.domain.PostOrder;
import com.ddd.webbb.post.domain.PostQueryRepository;
import com.ddd.webbb.post.domain.PostRepository;
import com.ddd.webbb.post.domain.PostSearchCondition;
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
import java.util.Set;
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
    private final PostQueryRepository postQueryRepository;
    private final PostLikeRepository postLikeRepository;
    private final BoardCategoryRepository boardCategoryRepository;
    private final UserService userService;
    private final AiAnalysisService aiAnalysisService;
    private final MonsterService monsterService;
    private final PostEmotionService postEmotionService;
    private final CommentService commentService;
    private final CommentLikeRepository commentLikeRepository;
    private final ProfanityFilter profanityFilter;

    public PostService(
            PostRepository postRepository,
            PostQueryRepository postQueryRepository,
            PostLikeRepository postLikeRepository,
            BoardCategoryRepository boardCategoryRepository,
            UserService userService,
            AiAnalysisService aiAnalysisService,
            MonsterService monsterService,
            PostEmotionService postEmotionService,
            CommentService commentService,
            CommentLikeRepository commentLikeRepository,
            ProfanityFilter profanityFilter) {
        this.postRepository = postRepository;
        this.postQueryRepository = postQueryRepository;
        this.postLikeRepository = postLikeRepository;
        this.boardCategoryRepository = boardCategoryRepository;
        this.userService = userService;
        this.aiAnalysisService = aiAnalysisService;
        this.monsterService = monsterService;
        this.postEmotionService = postEmotionService;
        this.commentService = commentService;
        this.commentLikeRepository = commentLikeRepository;
        this.profanityFilter = profanityFilter;
    }

    public Post getPostEntity(Long postId) {
        return postRepository
                .findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
    }

    public PostCreateResponse addPost(UUID userPublicId, PostCreateRequest request) {
        User user = userService.getUserEntity(userPublicId);
        BoardCategory category = resolveDefaultCategory();
        String content = profanityFilter.mask(request.content());
        String title = buildDefaultTitle(content);
        Post post =
                postRepository.save(
                        Post.create(user, category, title, content, request.commentTone()));

        AiAnalysisResponse analysis =
                aiAnalysisService.analyze(new PostContent(post.getId(), post.getContent()));
        EmotionType emotionType = EmotionType.valueOf(analysis.emotionType());

        Monster monster = monsterService.addMonster(post, emotionType, analysis.hp());
        postEmotionService.addPostEmotion(post, emotionType, user);

        return PostCreateResponse.of(post, user, emotionType, monster);
    }

    @Transactional(readOnly = true)
    public PostListResponse getPosts(Long cursor, int size) {
        return getPosts(null, cursor, size, PostSearchCondition.empty());
    }

    @Transactional(readOnly = true)
    public PostListResponse getPosts(Long cursor, int size, PostSearchCondition condition) {
        return getPosts(null, cursor, size, condition);
    }

    @Transactional(readOnly = true)
    public PostListResponse getPosts(
            UUID userPublicId, Long cursor, int size, PostSearchCondition condition) {
        User currentUser = getCurrentUserOrNull(userPublicId);
        Integer cursorLikeCount = resolveCursorLikeCount(cursor, condition);
        List<Post> fetched =
                postQueryRepository.findByCursor(cursor, cursorLikeCount, size, condition);
        boolean hasNext = fetched.size() > size;
        List<Post> posts = hasNext ? fetched.subList(0, size) : fetched;

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Map<Long, PostEmotion> emotionMap =
                postEmotionService.findByPostIds(postIds).stream()
                        .collect(Collectors.toMap(e -> e.getPost().getId(), e -> e));
        Map<Long, Monster> monsterMap =
                monsterService.findByPostIds(postIds).stream()
                        .collect(Collectors.toMap(m -> m.getPost().getId(), m -> m));
        Set<Long> likedPostIds = findLikedPostIds(postIds, currentUser);

        List<PostSummary> summaries =
                posts.stream()
                        .map(
                                post ->
                                        toPostSummary(
                                                post,
                                                emotionMap.get(post.getId()),
                                                monsterMap.get(post.getId()),
                                                likedPostIds.contains(post.getId())))
                        .toList();

        Long nextCursor = hasNext && !posts.isEmpty() ? posts.get(posts.size() - 1).getId() : null;
        return new PostListResponse(summaries, nextCursor);
    }

    public PostDetailResponse getPostDetail(Long postId) {
        return getPostDetail(null, postId);
    }

    public PostDetailResponse getPostDetail(UUID userPublicId, Long postId) {
        Post post =
                postRepository
                        .findByIdAndIsDeletedFalse(postId)
                        .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        post.incrementViewCount();

        PostEmotion postEmotion = postEmotionService.findByPost(postId);
        Monster monster = monsterService.findByPost(postId);
        List<Comment> comments = commentService.findCommentsByPost(postId);
        User currentUser = getCurrentUserOrNull(userPublicId);
        Set<Long> likedCommentIds =
                findLikedCommentIds(comments.stream().map(Comment::getId).toList(), currentUser);

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
                currentUser != null && postLikeRepository.existsByPostAndUser(post, currentUser),
                post.getCommentCount(),
                comments.stream()
                        .map(
                                c ->
                                        new PostDetailResponse.CommentInfo(
                                                c.getId(),
                                                toPublicId(c.getUser()),
                                                c.getUser().getNickname(),
                                                c.getUser().getJobType(),
                                                c.getUser().getCareerLevel(),
                                                c.getContent(),
                                                c.getLikeCount(),
                                                likedCommentIds.contains(c.getId()),
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

        String content = profanityFilter.mask(request.content());
        boolean contentChanged = !post.getContent().equals(content);
        post.update(content, request.commentTone());

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

    private PostSummary toPostSummary(
            Post post, PostEmotion postEmotion, Monster monster, boolean likedByMe) {
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
                likedByMe,
                post.getCommentCount(),
                post.getCreatedAt());
    }

    private User getCurrentUserOrNull(UUID userPublicId) {
        return userPublicId == null ? null : userService.getUserEntity(userPublicId);
    }

    private Integer resolveCursorLikeCount(Long cursor, PostSearchCondition condition) {
        if (cursor == null || condition.order() != PostOrder.POPULAR) {
            return null;
        }
        return getPostEntity(cursor).getLikeCount();
    }

    private Set<Long> findLikedPostIds(List<Long> postIds, User user) {
        if (user == null || postIds.isEmpty()) {
            return Set.of();
        }

        return postLikeRepository.findByPost_IdInAndUser(postIds, user).stream()
                .map(postLike -> postLike.getPost().getId())
                .collect(Collectors.toSet());
    }

    private Set<Long> findLikedCommentIds(List<Long> commentIds, User user) {
        if (user == null || commentIds.isEmpty()) {
            return Set.of();
        }

        return commentLikeRepository.findByComment_IdInAndUser(commentIds, user).stream()
                .map(commentLike -> commentLike.getComment().getId())
                .collect(Collectors.toSet());
    }

    private String toPublicId(User user) {
        UUID publicId = user.getPublicId();
        return publicId != null ? publicId.toString() : null;
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
