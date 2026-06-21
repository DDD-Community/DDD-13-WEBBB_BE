package com.ddd.webbb.post.interfaces.dto;

import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.post.domain.Post;

public record LikeResponse(Long postId, int likeCount, MonsterInfo monster) {

    public record MonsterInfo(int hp, int maxHp, String status) {}

    public static LikeResponse of(Post post, Monster monster) {
        return new LikeResponse(
                post.getId(),
                post.getLikeCount(),
                new MonsterInfo(monster.getHp(), monster.getMaxHp(), monster.getStatus().name()));
    }
}
