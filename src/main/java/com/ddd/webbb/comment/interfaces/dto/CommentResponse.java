package com.ddd.webbb.comment.interfaces.dto;

import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.monster.domain.Monster;
import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
        Long postId,
        Long parentCommentId,
        String content,
        MonsterInfo monster,
        LocalDateTime createdAt) {

    public record MonsterInfo(int hp, int maxHp, String status) {}

    public static CommentResponse of(Comment comment, Monster monster) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getContent(),
                new MonsterInfo(monster.getHp(), monster.getMaxHp(), monster.getStatus().name()),
                comment.getCreatedAt());
    }
}
