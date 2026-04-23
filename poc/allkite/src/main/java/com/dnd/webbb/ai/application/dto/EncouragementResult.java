package com.dnd.webbb.ai.application.dto;

import java.util.List;

public record EncouragementResult(
        List<String> emotionTags,
        String summary,
        String encouragementMessage,
        String smallActionTip) {}
