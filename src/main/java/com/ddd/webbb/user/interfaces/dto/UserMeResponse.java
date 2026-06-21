package com.ddd.webbb.user.interfaces.dto;

import com.ddd.webbb.user.domain.User;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserMeResponse(
        UUID id,
        String email,
        String nickname,
        String jobType,
        String careerLevel,
        boolean isActive,
        LocalDateTime createdAt) {

    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getPublicId(),
                user.getEmail(),
                user.getNickname(),
                user.getJobType(),
                user.getCareerLevel(),
                user.isActive(),
                user.getCreatedAt());
    }
}
