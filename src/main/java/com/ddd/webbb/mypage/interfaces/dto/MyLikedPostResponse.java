package com.ddd.webbb.mypage.interfaces.dto;

import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.post.domain.Post;
import java.time.LocalDateTime;
import java.util.List;

public record MyLikedPostResponse(List<LikedPost> posts, Long nextCursor) {

    private static final int CONTENT_PREVIEW_LENGTH = 30;

    public record LikedPost(
            Long postId,
            String contentPreview,
            String authorNickname,
            String authorJobType,
            String authorCareerLevel,
            String emotionType,
            String monsterStatus,
            int currentHp,
            int maxHp,
            int likeCount,
            int commentCount,
            String commentTone,
            LocalDateTime createdAt) {

        public static LikedPost of(Post post, Monster monster) {
            String content = post.getContent() == null ? "" : post.getContent();
            String preview =
                    content.length() <= CONTENT_PREVIEW_LENGTH
                            ? content
                            : content.substring(0, CONTENT_PREVIEW_LENGTH) + "...";
            return new LikedPost(
                    post.getId(),
                    preview,
                    post.getUser().getNickname(),
                    post.getUser().getJobType(),
                    post.getUser().getCareerLevel(),
                    monster != null ? monster.getEmotionType().name() : null,
                    monster != null ? monster.getStatus().name() : null,
                    monster != null ? monster.getHp() : 0,
                    monster != null ? monster.getMaxHp() : 0,
                    post.getLikeCount(),
                    post.getCommentCount(),
                    post.getCommentTone().name(),
                    post.getCreatedAt());
        }
    }
}
