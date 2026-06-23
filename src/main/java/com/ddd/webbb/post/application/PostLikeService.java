package com.ddd.webbb.post.application;

import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.monster.application.MonsterService;
import com.ddd.webbb.monster.domain.HpActionType;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.notification.domain.event.PostLikeNotificationEvent;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostLike;
import com.ddd.webbb.post.domain.PostLikeRepository;
import com.ddd.webbb.post.domain.PostRepository;
import com.ddd.webbb.post.interfaces.dto.LikeResponse;
import com.ddd.webbb.user.application.UserService;
import com.ddd.webbb.user.domain.User;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostLikeService {

    private static final int POST_LIKE_HP_DELTA = 1;

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserService userService;
    private final MonsterService monsterService;
    private final ApplicationEventPublisher eventPublisher;

    public PostLikeService(
            PostLikeRepository postLikeRepository,
            PostRepository postRepository,
            UserService userService,
            MonsterService monsterService,
            ApplicationEventPublisher eventPublisher) {
        this.postLikeRepository = postLikeRepository;
        this.postRepository = postRepository;
        this.userService = userService;
        this.monsterService = monsterService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public LikeResponse addPostLike(UUID userPublicId, Long postId) {
        User user = userService.getUserEntity(userPublicId);
        Post post = getPost(postId);

        try {
            postLikeRepository.saveAndFlush(PostLike.create(post, user));
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.ALREADY_LIKED_POST);
        }
        post.incrementLikeCount();

        Monster monster = monsterService.getMonsterByPostId(postId);
        monsterService.decreaseMonsterHp(
                monster, user, post, null, HpActionType.POST_LIKE, POST_LIKE_HP_DELTA);

        eventPublisher.publishEvent(new PostLikeNotificationEvent(post.getUser(), user, post));
        return LikeResponse.of(post, monster);
    }

    @Transactional
    public LikeResponse removePostLike(UUID userPublicId, Long postId) {
        User user = userService.getUserEntity(userPublicId);
        Post post = getPost(postId);

        PostLike postLike =
                postLikeRepository
                        .findByPostAndUser(post, user)
                        .orElseThrow(() -> new AppException(ErrorCode.POST_LIKE_NOT_FOUND));

        postLikeRepository.delete(postLike);
        post.decrementLikeCount();
        Monster monster = monsterService.getMonsterByPostId(postId);
        return LikeResponse.of(post, monster);
    }

    private Post getPost(Long postId) {
        return postRepository
                .findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
    }
}
