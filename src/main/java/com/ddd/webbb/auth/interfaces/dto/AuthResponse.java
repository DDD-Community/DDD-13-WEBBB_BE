package com.ddd.webbb.auth.interfaces.dto;

public record AuthResponse(UserInfo user, TokenInfo tokens, boolean isNewUser) {

    public record UserInfo(
            String id,
            String email,
            String nickname,
            String jobRole,
            String careerYear,
            String status) {}

    public record TokenInfo(String accessToken, String refreshToken) {}
}
