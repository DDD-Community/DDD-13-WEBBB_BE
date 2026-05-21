package com.ddd.webbb.auth.infrastructure;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final long refreshTokenExpiry;

    public RefreshTokenStore(
            StringRedisTemplate redisTemplate,
            @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry) {
        this.redisTemplate = redisTemplate;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    public void save(UUID publicId, String refreshToken) {
        redisTemplate
                .opsForValue()
                .set(
                        KEY_PREFIX + publicId.toString(),
                        refreshToken,
                        refreshTokenExpiry,
                        TimeUnit.MILLISECONDS);
    }

    public String find(UUID publicId) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + publicId.toString());
    }

    public void delete(UUID publicId) {
        redisTemplate.delete(KEY_PREFIX + publicId.toString());
    }
}
