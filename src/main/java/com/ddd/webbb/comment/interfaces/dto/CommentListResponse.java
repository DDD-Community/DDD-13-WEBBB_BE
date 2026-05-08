package com.ddd.webbb.comment.interfaces.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentListResponse(List<CommentSummary> comments, Long nextCursor) {

    public record CommentSummary(
            Long commentId, String authorNickname, String content, LocalDateTime createdAt) {}
}
