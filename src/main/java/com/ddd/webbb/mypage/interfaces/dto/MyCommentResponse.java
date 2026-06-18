package com.ddd.webbb.mypage.interfaces.dto;

import com.ddd.webbb.comment.domain.Comment;
import java.time.LocalDateTime;
import java.util.List;

public record MyCommentResponse(List<MyComment> comments, Long nextCursor) {

    public record MyComment(Long commentId, Long postId, String content, LocalDateTime createdAt) {

        public static MyComment from(Comment comment) {
            return new MyComment(
                    comment.getId(),
                    comment.getPost().getId(),
                    comment.getContent(),
                    comment.getCreatedAt());
        }
    }
}
