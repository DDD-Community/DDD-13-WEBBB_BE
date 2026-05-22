package com.ddd.webbb.auth.infrastructure;

import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OAuthCodeStore {

    private static final String KEY_PREFIX = "oauth_code:";
    private static final long CODE_TTL_SECONDS = 30;
    private static final int CODE_BYTE_LENGTH = 32;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuthCodeStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public String save(String accessToken, String refreshToken) {
        String code = generateCode();
        TokenPair tokenPair = new TokenPair(accessToken, refreshToken);
        try {
            String json = objectMapper.writeValueAsString(tokenPair);
            redisTemplate
                    .opsForValue()
                    .set(KEY_PREFIX + code, json, CODE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return code;
    }

    public TokenPair exchange(String code) {
        String key = KEY_PREFIX + code;
        String json = redisTemplate.opsForValue().getAndDelete(key);
        if (json == null) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
        try {
            return objectMapper.readValue(json, TokenPair.class);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String generateCode() {
        byte[] bytes = new byte[CODE_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record TokenPair(String accessToken, String refreshToken) {}
}
