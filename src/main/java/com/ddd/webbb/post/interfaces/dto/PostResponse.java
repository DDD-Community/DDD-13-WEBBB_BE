package com.ddd.webbb.post.interfaces.dto;

import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.emotion.domain.PostEmotion;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.user.domain.User;
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

    public static PostResponse of(Post post, User user, PostEmotion postEmotion, Monster monster) {
        EmotionType emotionType = postEmotion.getEmotionType();
        return new PostResponse(
                post.getId(),
                new AuthorInfo(
                        user.getPublicId().toString(),
                        user.getNickname(),
                        user.getJobType(),
                        user.getCareerLevel()),
                post.getContent(),
                post.getCommentTone().name(),
                new EmotionInfo(
                        emotionType.name(), emotionType.getDisplayName(), emotionType.getSummary()),
                new MonsterInfo(
                        emotionType.monsterType(),
                        monster.getHp(),
                        monster.getMaxHp(),
                        monster.getStatus().name()),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getCreatedAt());
    }

    public record AuthorInfo(String id, String nickname, String jobRole, String careerYear) {}

    public record EmotionInfo(String type, String displayName, String summary) {}

    public record MonsterInfo(String type, int hp, int maxHp, String status) {}
}
