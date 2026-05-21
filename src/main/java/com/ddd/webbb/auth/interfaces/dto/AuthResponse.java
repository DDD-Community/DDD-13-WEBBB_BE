package com.ddd.webbb.auth.interfaces.dto;

import com.ddd.webbb.auth.domain.AuthToken;
import com.ddd.webbb.user.domain.User;

public record AuthResponse(UserInfo user, TokenInfo tokens, boolean isNewUser) {

    public record UserInfo(
            String id,
            String email,
            String nickname,
            String jobRole,
            String careerYear,
            String status) {

        public static UserInfo from(User user) {
            return new UserInfo(
                    user.getPublicId().toString(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getJobType(),
                    user.getCareerLevel(),
                    user.isActive() ? "ACTIVE" : "INACTIVE");
        }
    }

    public record TokenInfo(String accessToken, String refreshToken) {

        public static TokenInfo from(AuthToken authToken) {
            return new TokenInfo(authToken.accessToken(), authToken.refreshToken());
        }
    }
}
