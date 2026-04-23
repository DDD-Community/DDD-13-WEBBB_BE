package com.dnd.webbb.ai.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalyzeRequest(@NotBlank(message = "고민글 본문은 필수입니다.") String text, String tone) {

    public AnalyzeRequest {
        if (tone == null || tone.isBlank()) {
            tone = "친구같은";
        }
    }
}
