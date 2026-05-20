package com.ddd.webbb.auth.infrastructure;

import com.ddd.webbb.auth.domain.RefreshTokenStore;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisRefreshTokenStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void save(UUID userPublicId, String refreshToken, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key(userPublicId), refreshToken, ttl);
    }

    @Override
    public boolean matches(UUID userPublicId, String refreshToken) {
        String storedToken = stringRedisTemplate.opsForValue().get(key(userPublicId));
        return refreshToken.equals(storedToken);
    }

    @Override
    public void delete(UUID userPublicId) {
        stringRedisTemplate.delete(key(userPublicId));
    }

    private String key(UUID userPublicId) {
        return "auth:refresh:" + userPublicId;
    }
}
