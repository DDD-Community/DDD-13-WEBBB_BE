package com.ddd.webbb.auth.domain;

import java.time.Duration;
import java.util.UUID;

public interface RefreshTokenStore {

    void save(UUID userPublicId, String refreshToken, Duration ttl);

    boolean matches(UUID userPublicId, String refreshToken);

    void delete(UUID userPublicId);
}
