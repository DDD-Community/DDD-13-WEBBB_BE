package com.dnd.webbb.user.interfaces.dto;

import com.dnd.webbb.user.domain.User;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id, String email, String nickname, String status, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getPublicId(),
                user.getEmail(),
                user.getNickname(),
                user.getStatus().name(),
                user.getCreatedAt());
    }
}
