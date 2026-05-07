package com.ddd.webbb.user.interfaces.dto;

import java.time.LocalDateTime;

public record UserMeResponse(
        String id,
        String email,
        String nickname,
        String jobRole,
        String careerYear,
        String status,
        LocalDateTime createdAt) {}
