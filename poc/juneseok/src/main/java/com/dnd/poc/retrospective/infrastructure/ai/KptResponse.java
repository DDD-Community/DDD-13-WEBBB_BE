package com.dnd.poc.retrospective.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

record KptResponse(
        List<String> keep,
        List<String> problem,
        @JsonProperty("try") List<String> tries,
        @JsonProperty("cheer_message") String cheerMessage
) {
}
