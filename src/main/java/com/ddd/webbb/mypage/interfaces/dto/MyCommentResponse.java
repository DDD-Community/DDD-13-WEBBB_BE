package com.ddd.webbb.mypage.interfaces.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MyCommentResponse(List<MyComment> comments, Long nextCursor) {

    public record MyComment(Long commentId, Long postId, String content, LocalDateTime createdAt) {}
}
