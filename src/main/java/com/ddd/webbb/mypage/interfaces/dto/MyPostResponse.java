package com.ddd.webbb.mypage.interfaces.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MyPostResponse(List<MyPost> posts, Long nextCursor) {

    public record MyPost(
            Long postId,
            String contentPreview,
            String emotionType,
            String monsterStatus,
            LocalDateTime createdAt) {}
}
