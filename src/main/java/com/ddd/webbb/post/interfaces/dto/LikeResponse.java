package com.ddd.webbb.post.interfaces.dto;

public record LikeResponse(Long postId, int likeCount, MonsterInfo monster) {

    public record MonsterInfo(int hp, int maxHp, String status) {}
}
