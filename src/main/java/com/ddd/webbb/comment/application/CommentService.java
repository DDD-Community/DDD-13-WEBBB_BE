package com.ddd.webbb.comment.application;

import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.comment.domain.CommentQueryRepository;
import com.ddd.webbb.comment.domain.CommentRepository;
import com.ddd.webbb.comment.interfaces.dto.CommentCreateRequest;
import com.ddd.webbb.comment.interfaces.dto.CommentListResponse;
import com.ddd.webbb.comment.interfaces.dto.CommentResponse;
import com.ddd.webbb.comment.interfaces.dto.CommentUpdateRequest;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.monster.application.MonsterService;
import com.ddd.webbb.monster.domain.HpActionType;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.post.application.PostService;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.user.application.UserService;
import com.ddd.webbb.user.domain.User;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private static final int COMMENT_HP_DELTA = 1;

    private final CommentRepository commentRepository;
    private final CommentQueryRepository commentQueryRepository;
    private final PostService postService;
    private final UserService userService;
    private final MonsterService monsterService;

    public CommentService(
            CommentRepository commentRepository,
            CommentQueryRepository commentQueryRepository,
            @Lazy PostService postService,
            UserService userService,
            MonsterService monsterService) {
        this.commentRepository = commentRepository;
        this.commentQueryRepository = commentQueryRepository;
        this.postService = postService;
        this.userService = userService;
        this.monsterService = monsterService;
    }

    public List<Comment> findCommentsByPost(Long postId) {
        return commentRepository.findByPost_IdAndIsDeletedFalseOrderByCreatedAtAsc(postId);
    }

    public CommentListResponse getComments(Long postId, Long cursor, int size) {
        if (size <= 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        List<Comment> rootComments =
                commentQueryRepository.findRootCommentsByPostId(postId, cursor, size);

        List<Long> parentIds = rootComments.stream().map(Comment::getId).toList();
        Map<Long, List<Comment>> repliesByParentId =
                parentIds.isEmpty()
                        ? Map.of()
                        : commentQueryRepository.findRepliesByParentIds(parentIds).stream()
                                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        List<CommentListResponse.CommentSummary> summaries =
                rootComments.stream()
                        .map(
                                root -> {
                                    List<CommentListResponse.ReplySummary> replies =
                                            repliesByParentId
                                                    .getOrDefault(root.getId(), List.of())
                                                    .stream()
                                                    .map(CommentListResponse.ReplySummary::from)
                                                    .toList();
                                    return CommentListResponse.CommentSummary.of(root, replies);
                                })
                        .toList();

        Long nextCursor =
                rootComments.size() == size
                        ? rootComments.get(rootComments.size() - 1).getId()
                        : null;
        return new CommentListResponse(summaries, nextCursor);
    }

    @Transactional
    public CommentResponse addComment(
            UUID userPublicId, Long postId, CommentCreateRequest request) {
        User user = userService.getUserEntity(userPublicId);
        Post post = postService.getPostEntity(postId);
        Monster monster = monsterService.getMonsterByPostId(postId);

        Comment parent = null;
        if (request.parentCommentId() != null) {
            parent =
                    commentRepository
                            .findByIdAndIsDeletedFalse(request.parentCommentId())
                            .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
            validateParentComment(parent, post);
        }

        Comment comment =
                commentRepository.save(Comment.create(post, user, parent, request.content()));
        post.incrementCommentCount();
        monsterService.decreaseMonsterHp(
                monster, user, post, comment, HpActionType.COMMENT, COMMENT_HP_DELTA);

        return CommentResponse.of(comment, monster);
    }

    @Transactional
    public CommentResponse modifyComment(
            UUID userPublicId, Long commentId, CommentUpdateRequest request) {
        User user = userService.getUserEntity(userPublicId);
        Comment comment =
                commentRepository
                        .findByIdAndIsDeletedFalse(commentId)
                        .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getPublicId().equals(user.getPublicId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        comment.updateContent(request.content());
        Monster monster = monsterService.getMonsterByPostId(comment.getPost().getId());
        return CommentResponse.of(comment, monster);
    }

    @Transactional
    public void removeComment(UUID userPublicId, Long commentId) {
        User user = userService.getUserEntity(userPublicId);
        Comment comment =
                commentRepository
                        .findByIdAndIsDeletedFalse(commentId)
                        .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getPublicId().equals(user.getPublicId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        comment.delete();
        Post post = comment.getPost();
        int deletedCount = 1 + softDeleteDescendants(comment);

        for (int i = 0; i < deletedCount; i++) {
            post.decrementCommentCount();
        }
    }

    private int softDeleteDescendants(Comment parent) {
        List<Comment> children = commentRepository.findByParentAndIsDeletedFalse(parent);
        int count = 0;
        for (Comment child : children) {
            child.delete();
            count++;
            count += softDeleteDescendants(child);
        }
        return count;
    }

    private void validateParentComment(Comment parent, Post post) {
        if (!parent.getPost().getId().equals(post.getId())) {
            throw new AppException(ErrorCode.INVALID_PARENT_COMMENT);
        }
        if (parent.getParent() != null) {
            throw new AppException(ErrorCode.INVALID_PARENT_COMMENT);
        }
    }
}
