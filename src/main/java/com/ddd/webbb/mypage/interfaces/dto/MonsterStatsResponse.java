package com.ddd.webbb.mypage.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 몬스터 통계 응답")
public record MonsterStatsResponse(
        @Schema(description = "사용자가 작성한 게시글에 생성된 전체 몬스터 수", example = "5") int totalMonsterCount,
        @Schema(description = "HP가 0이 되어 처치된 몬스터 수", example = "3") int defeatedMonsterCount,
        @Schema(
                        description =
                                "가장 많이 나타난 감정 유형 정보. 게시글에 몬스터가 하나도 없으면 null이며, "
                                        + "프론트에서는 null일 때 '두드러진 감정이 없어요'를 표시한다.",
                        nullable = true)
                MostFrequentEmotion mostFrequentEmotion) {

    @Schema(description = "최다 빈도 감정 유형 상세")
    public record MostFrequentEmotion(
            @Schema(
                            description =
                                    "감정 타입 enum. "
                                            + "ANXIETY=불안, "
                                            + "LETHARGY=무기력, "
                                            + "LONELINESS=외로움, "
                                            + "SELF_DEPRECATION=자기비하, "
                                            + "IRRITATION=짜증",
                            example = "ANXIETY")
                    String type,
            @Schema(description = "감정 한국어 표시명", example = "불안") String displayName,
            @Schema(description = "해당 감정 타입 몬스터 수", example = "2") int count,
            @Schema(description = "전체 몬스터 중 해당 감정의 비율 (0~100, 반올림)", example = "40")
                    int percentage) {}
}
