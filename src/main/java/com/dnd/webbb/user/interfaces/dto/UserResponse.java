package com.dnd.webbb.user.interfaces.dto;

import com.dnd.webbb.user.domain.User;
import java.time.LocalDateTime;

public record UserResponse(
        Long id, String email, String nickname, String status, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getStatus().name(),
                user.getCreatedAt());
    }
}
