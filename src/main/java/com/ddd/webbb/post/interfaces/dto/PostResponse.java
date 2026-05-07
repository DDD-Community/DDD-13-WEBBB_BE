package com.ddd.webbb.post.interfaces.dto;

import java.time.LocalDateTime;

public record PostResponse(
        Long postId,
        AuthorInfo author,
        String content,
        String commentTone,
        EmotionInfo emotion,
        MonsterInfo monster,
        int likeCount,
        int commentCount,
        LocalDateTime createdAt) {

    public record AuthorInfo(String id, String nickname, String jobRole, String careerYear) {}

    public record EmotionInfo(String type, String displayName, String summary) {}

    public record MonsterInfo(String type, int hp, int maxHp, String status) {}
}
