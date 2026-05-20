package com.ddd.webbb.post.interfaces.dto;

import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.post.domain.CommentTone;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

public record PostCreateResponse(
        @Schema(description = "생성된 게시글 ID", example = "1") Long postId,
        @Schema(description = "작성자 정보") AuthorInfo author,
        @Schema(description = "저장된 고민글 본문", example = "면접에서 계속 떨어져서 점점 자신감이 사라져요.") String content,
        @Schema(
                        description =
                                "선택한 댓글 톤. "
                                        + "VENT_WITH_ME=대신 욕해주기, "
                                        + "COMFORT_ME=무조건 위로해주기, "
                                        + "WARM_ADVICE=따뜻한 조언해주기, "
                                        + "MAKE_ME_LAUGH=웃겨주기",
                        example = "COMFORT_ME")
                CommentTone commentTone,
        @Schema(description = "AI 감정 분석 결과") EmotionInfo emotion,
        @Schema(description = "감정에 매핑된 몬스터 정보") MonsterInfo monster,
        @Schema(description = "좋아요 수", example = "0") int likeCount,
        @Schema(description = "댓글 수", example = "0") int commentCount,
        @Schema(description = "게시글 생성 시각", example = "2026-05-20T21:00:00")
                LocalDateTime createdAt) {

    public static PostCreateResponse of(
            Post post, User user, EmotionType emotionType, Monster monster) {
        return new PostCreateResponse(
                post.getId(),
                AuthorInfo.from(user),
                post.getContent(),
                post.getCommentTone(),
                EmotionInfo.from(emotionType),
                MonsterInfo.from(monster),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getCreatedAt());
    }

    public record AuthorInfo(
            @Schema(description = "작성자 publicId", example = "01939b10-7b0f-7c8f-9a2b-111111111111")
                    String id,
            @Schema(description = "작성자 닉네임", example = "ogu") String nickname,
            @Schema(description = "작성자 직군", example = "DEVELOPMENT") String jobRole,
            @Schema(description = "작성자 경력", example = "YEAR_3") String careerYear) {

        private static AuthorInfo from(User user) {
            UUID publicId = user.getPublicId();
            return new AuthorInfo(
                    publicId != null ? publicId.toString() : null,
                    user.getNickname(),
                    user.getJobType(),
                    user.getCareerLevel());
        }
    }

    public record EmotionInfo(
            @Schema(description = "감정 타입", example = "ANXIETY") String type,
            @Schema(description = "감정 표시명", example = "불안") String displayName,
            @Schema(description = "감정 요약 설명", example = "걱정과 긴장으로 마음이 무거운 상태") String summary) {

        private static EmotionInfo from(EmotionType emotionType) {
            return new EmotionInfo(
                    emotionType.name(), emotionType.getDisplayName(), emotionType.getSummary());
        }
    }

    public record MonsterInfo(
            @Schema(
                            description =
                                    "프론트 에셋 선택용 몬스터 타입. "
                                            + "ANXIETY_MONSTER=불안 몬스터, "
                                            + "LETHARGY_MONSTER=무기력 몬스터, "
                                            + "LONELINESS_MONSTER=외로움 몬스터, "
                                            + "SELF_DEPRECATION_MONSTER=자기비하 몬스터, "
                                            + "IRRITATION_MONSTER=짜증 몬스터",
                            example = "ANXIETY_MONSTER")
                    String type,
            @Schema(description = "현재 HP", example = "30") int hp,
            @Schema(description = "최대 HP", example = "30") int maxHp,
            @Schema(description = "몬스터 상태", example = "ALIVE") String status) {

        private static MonsterInfo from(Monster monster) {
            return new MonsterInfo(
                    monster.getEmotionType().monsterType(),
                    monster.getHp(),
                    monster.getMaxHp(),
                    monster.getStatus().name());
        }
    }
}
