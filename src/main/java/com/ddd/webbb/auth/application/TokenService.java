package com.ddd.webbb.auth.application;

import com.ddd.webbb.user.domain.User;
import java.time.Duration;
import java.util.UUID;

public interface TokenService {

    String createAccessToken(User user);

    String createRefreshToken(User user);

    UUID parseAccessToken(String token);

    UUID parseRefreshToken(String token);

    Duration getRefreshTokenTtl();
}
