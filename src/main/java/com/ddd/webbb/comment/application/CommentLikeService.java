package com.ddd.webbb.comment.application;

import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.comment.domain.CommentLike;
import com.ddd.webbb.comment.domain.CommentLikeRepository;
import com.ddd.webbb.comment.domain.CommentRepository;
import com.ddd.webbb.comment.interfaces.dto.CommentLikeResponse;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.monster.application.MonsterService;
import com.ddd.webbb.monster.domain.HpActionType;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.user.application.UserService;
import com.ddd.webbb.user.domain.User;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommentLikeService {

    private static final int COMMENT_LIKE_HP_DELTA = 1;

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;
    private final MonsterService monsterService;

    public CommentLikeService(
            CommentLikeRepository commentLikeRepository,
            CommentRepository commentRepository,
            UserService userService,
            MonsterService monsterService) {
        this.commentLikeRepository = commentLikeRepository;
        this.commentRepository = commentRepository;
        this.userService = userService;
        this.monsterService = monsterService;
    }

    @Transactional
    public CommentLikeResponse addCommentLike(UUID userPublicId, Long postId, Long commentId) {
        User user = userService.getUserEntity(userPublicId);
        Comment comment = getCommentAndValidatePost(commentId, postId);

        try {
            commentLikeRepository.saveAndFlush(CommentLike.create(comment, user));
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.ALREADY_LIKED_COMMENT);
        }
        comment.incrementLikeCount();

        Monster monster = monsterService.getMonsterByPostId(postId);
        monsterService.decreaseMonsterHp(
                monster,
                user,
                comment.getPost(),
                comment,
                HpActionType.COMMENT_LIKE,
                COMMENT_LIKE_HP_DELTA);

        return CommentLikeResponse.of(comment, monster);
    }

    @Transactional
    public void removeCommentLike(UUID userPublicId, Long postId, Long commentId) {
        User user = userService.getUserEntity(userPublicId);
        Comment comment = getCommentAndValidatePost(commentId, postId);

        CommentLike commentLike =
                commentLikeRepository
                        .findByCommentAndUser(comment, user)
                        .orElseThrow(() -> new AppException(ErrorCode.COMMENT_LIKE_NOT_FOUND));

        commentLikeRepository.delete(commentLike);
        comment.decrementLikeCount();
    }

    private Comment getCommentAndValidatePost(Long commentId, Long postId) {
        Comment comment =
                commentRepository
                        .findByIdAndIsDeletedFalse(commentId)
                        .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getPost().getId().equals(postId)) {
            throw new AppException(ErrorCode.COMMENT_POST_MISMATCH);
        }
        return comment;
    }
}
