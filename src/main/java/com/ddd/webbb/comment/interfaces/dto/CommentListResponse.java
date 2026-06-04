package com.ddd.webbb.comment.interfaces.dto;

import com.ddd.webbb.comment.domain.Comment;
import java.time.LocalDateTime;
import java.util.List;

public record CommentListResponse(List<CommentSummary> comments, Long nextCursor) {

    public record CommentSummary(
            Long commentId,
            String authorNickname,
            String content,
            int likeCount,
            List<ReplySummary> replies,
            LocalDateTime createdAt) {

        public static CommentSummary of(Comment comment, List<ReplySummary> replies) {
            return new CommentSummary(
                    comment.getId(),
                    comment.getUser().getNickname(),
                    comment.getContent(),
                    comment.getLikeCount(),
                    replies,
                    comment.getCreatedAt());
        }
    }

    public record ReplySummary(
            Long commentId,
            Long parentCommentId,
            String authorNickname,
            String content,
            int likeCount,
            LocalDateTime createdAt) {

        public static ReplySummary from(Comment reply) {
            return new ReplySummary(
                    reply.getId(),
                    reply.getParent().getId(),
                    reply.getUser().getNickname(),
                    reply.getContent(),
                    reply.getLikeCount(),
                    reply.getCreatedAt());
        }
    }
}
