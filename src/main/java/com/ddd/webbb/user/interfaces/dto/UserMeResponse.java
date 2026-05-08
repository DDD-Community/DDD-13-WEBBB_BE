package com.ddd.webbb.user.interfaces.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserMeResponse(
        UUID id,
        String email,
        String nickname,
        String jobType,
        String careerLevel,
        boolean isActive,
        LocalDateTime createdAt) {}
