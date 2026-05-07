package com.ddd.webbb.post.interfaces.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
        Long postId,
        AuthorInfo author,
        String content,
        String commentTone,
        EmotionInfo emotion,
        MonsterInfo monster,
        int likeCount,
        int commentCount,
        List<CommentInfo> comments,
        LocalDateTime createdAt) {

    public record AuthorInfo(String id, String nickname, String jobRole, String careerYear) {}

    public record EmotionInfo(String type, String displayName) {}

    public record MonsterInfo(String type, int hp, int maxHp, String status) {}

    public record CommentInfo(
            Long commentId, String authorNickname, String content, LocalDateTime createdAt) {}
}
