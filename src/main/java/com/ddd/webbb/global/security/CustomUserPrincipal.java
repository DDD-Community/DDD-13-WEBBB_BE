package com.ddd.webbb.global.security;

import com.ddd.webbb.user.domain.User;
import java.util.UUID;

public record CustomUserPrincipal(UUID publicId, String email, String nickname) {

    public static CustomUserPrincipal from(User user) {
        return new CustomUserPrincipal(user.getPublicId(), user.getEmail(), user.getNickname());
    }
}
