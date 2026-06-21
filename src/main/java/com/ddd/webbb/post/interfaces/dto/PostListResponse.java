package com.ddd.webbb.post.interfaces.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostListResponse(List<PostSummary> posts, Long nextCursor) {

    public record PostSummary(
            Long postId,
            String authorNickname,
            String jobRole,
            String careerYear,
            String contentPreview,
            String emotionType,
            MonsterInfo monster,
            int likeCount,
            boolean likedByMe,
            int commentCount,
            LocalDateTime createdAt) {}

    public record MonsterInfo(String type, int hp, int maxHp, String status) {}
}
