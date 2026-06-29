package com.ddd.webbb.auth.infrastructure;

import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetStore {

    private static final String KEY_PREFIX = "password_reset:";
    private static final long TTL_SECONDS = 300L;

    private final StringRedisTemplate redisTemplate;

    public PasswordResetStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String email, String code) {
        redisTemplate.opsForValue().set(KEY_PREFIX + email, code, TTL_SECONDS, TimeUnit.SECONDS);
    }

    public boolean verifyAndDelete(String email, String code) {
        String stored = redisTemplate.opsForValue().get(KEY_PREFIX + email);
        if (stored == null || !stored.equals(code)) {
            return false;
        }
        redisTemplate.delete(KEY_PREFIX + email);
        return true;
    }
}
