package com.ddd.webbb.comment.interfaces.dto;

import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId, Long postId, String content, MonsterInfo monster, LocalDateTime createdAt) {

    public record MonsterInfo(int hp, int maxHp, String status) {}
}
