package com.dnd.poc.retrospective.interfaces.dto;

import com.dnd.poc.retrospective.domain.RetrospectiveContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회고 분석 요청")
public record RetrospectiveRequest(
        @Schema(
                description = "사용자가 작성한 회고 원문 (1~2000자)",
                example = "오늘 코테에서 시간이 부족해서 마지막 문제를 못 풀었어요. 자료구조 공부가 부족했던 것 같아요.",
                minLength = 1,
                maxLength = RetrospectiveContext.MAX_LENGTH,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "context must not be blank")
        @Size(max = RetrospectiveContext.MAX_LENGTH, message = "context too long")
        String context
) {
}
