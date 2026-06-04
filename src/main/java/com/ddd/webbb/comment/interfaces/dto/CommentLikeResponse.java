package com.ddd.webbb.comment.interfaces.dto;

import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.monster.domain.Monster;

public record CommentLikeResponse(Long commentId, int likeCount, MonsterInfo monster) {

    public record MonsterInfo(int hp, int maxHp, String status) {}

    public static CommentLikeResponse of(Comment comment, Monster monster) {
        return new CommentLikeResponse(
                comment.getId(),
                comment.getLikeCount(),
                new MonsterInfo(monster.getHp(), monster.getMaxHp(), monster.getStatus().name()));
    }
}
