package com.ddd.webbb.ai.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CommentSummaryResult(
        @JsonProperty("summary") String summary, @JsonProperty("tone") String tone) {

    public boolean isValid() {
        return summary != null && !summary.isBlank() && tone != null && !tone.isBlank();
    }
}
