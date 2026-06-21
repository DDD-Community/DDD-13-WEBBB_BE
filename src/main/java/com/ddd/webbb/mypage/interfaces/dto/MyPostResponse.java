package com.ddd.webbb.mypage.interfaces.dto;

import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.post.domain.Post;
import java.time.LocalDateTime;
import java.util.List;

public record MyPostResponse(List<MyPost> posts, Long nextCursor) {

    private static final int CONTENT_PREVIEW_LENGTH = 30;

    public record MyPost(
            Long postId,
            String contentPreview,
            String emotionType,
            String monsterStatus,
            LocalDateTime createdAt) {

        public static MyPost of(Post post, Monster monster) {
            String content = post.getContent() == null ? "" : post.getContent();
            String preview =
                    content.length() <= CONTENT_PREVIEW_LENGTH
                            ? content
                            : content.substring(0, CONTENT_PREVIEW_LENGTH) + "...";
            return new MyPost(
                    post.getId(),
                    preview,
                    monster != null ? monster.getEmotionType().name() : null,
                    monster != null ? monster.getStatus().name() : null,
                    post.getCreatedAt());
        }
    }
}
