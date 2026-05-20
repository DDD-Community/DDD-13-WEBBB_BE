package com.ddd.webbb.post.application;

import com.ddd.webbb.ai.application.AiAnalysisResponse;
import com.ddd.webbb.ai.application.AiAnalysisService;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.category.domain.BoardCategory;
import com.ddd.webbb.category.domain.BoardCategoryRepository;
import com.ddd.webbb.emotion.application.PostEmotionService;
import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.monster.application.MonsterService;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostRepository;
import com.ddd.webbb.post.interfaces.dto.PostCreateRequest;
import com.ddd.webbb.post.interfaces.dto.PostCreateResponse;
import com.ddd.webbb.user.application.UserService;
import com.ddd.webbb.user.domain.User;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PostService {

    private static final String DEFAULT_CATEGORY_NAME = "멘탈케어";

    private final PostRepository postRepository;
    private final BoardCategoryRepository boardCategoryRepository;
    private final UserService userService;
    private final AiAnalysisService aiAnalysisService;
    private final MonsterService monsterService;
    private final PostEmotionService postEmotionService;

    public PostService(
            PostRepository postRepository,
            BoardCategoryRepository boardCategoryRepository,
            UserService userService,
            AiAnalysisService aiAnalysisService,
            MonsterService monsterService,
            PostEmotionService postEmotionService) {
        this.postRepository = postRepository;
        this.boardCategoryRepository = boardCategoryRepository;
        this.userService = userService;
        this.aiAnalysisService = aiAnalysisService;
        this.monsterService = monsterService;
        this.postEmotionService = postEmotionService;
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
