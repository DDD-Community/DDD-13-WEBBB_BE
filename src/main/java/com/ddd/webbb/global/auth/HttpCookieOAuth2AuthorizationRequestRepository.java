package com.ddd.webbb.global.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String KEY_PREFIX = "oauth2_auth_req:";
    private static final long TTL_SECONDS = 180;

    private final StringRedisTemplate redisTemplate;

    public HttpCookieOAuth2AuthorizationRequestRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter("state");
        if (state == null) {
            return null;
        }
        return getFromRedis(state);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (authorizationRequest == null) {
            String state = request.getParameter("state");
            if (state != null) {
                redisTemplate.delete(KEY_PREFIX + state);
            }
            return;
        }

        String state = authorizationRequest.getState();
        redisTemplate
                .opsForValue()
                .set(
                        KEY_PREFIX + state,
                        serialize(authorizationRequest),
                        TTL_SECONDS,
                        TimeUnit.SECONDS);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {

        String state = request.getParameter("state");
        if (state == null) {
            return null;
        }

        String key = KEY_PREFIX + state;
        String value = redisTemplate.opsForValue().getAndDelete(key);
        if (value == null) {
            return null;
        }
        return deserialize(value);
    }

    private OAuth2AuthorizationRequest getFromRedis(String state) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + state);
        if (value == null) {
            return null;
        }
        return deserialize(value);
    }

    @SuppressWarnings("deprecation")
    private String serialize(OAuth2AuthorizationRequest request) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(request));
    }

    @SuppressWarnings("deprecation")
    private OAuth2AuthorizationRequest deserialize(String value) {
        return (OAuth2AuthorizationRequest)
                SerializationUtils.deserialize(Base64.getUrlDecoder().decode(value));
    }
}
