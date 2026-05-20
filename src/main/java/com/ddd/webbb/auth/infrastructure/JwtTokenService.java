package com.ddd.webbb.auth.infrastructure;

import com.ddd.webbb.auth.application.TokenService;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService implements TokenService {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final AuthTokenProperties properties;
    private final Key signingKey;

    public JwtTokenService(AuthTokenProperties properties) {
        this.properties = properties;
        this.signingKey =
                Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String createAccessToken(User user) {
        return createToken(user, ACCESS_TOKEN_TYPE, properties.getAccessTokenExpiration());
    }

    @Override
    public String createRefreshToken(User user) {
        return createToken(user, REFRESH_TOKEN_TYPE, properties.getRefreshTokenExpiration());
    }

    @Override
    public UUID parseAccessToken(String token) {
        return parseToken(token, ACCESS_TOKEN_TYPE);
    }

    @Override
    public UUID parseRefreshToken(String token) {
        return parseToken(token, REFRESH_TOKEN_TYPE);
    }

    @Override
    public Duration getRefreshTokenTtl() {
        return properties.getRefreshTokenExpiration();
    }

    private String createToken(User user, String tokenType, Duration expiration) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getPublicId().toString())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    private UUID parseToken(String token, String expectedType) {
        try {
            Claims claims =
                    Jwts.parser()
                            .verifyWith((javax.crypto.SecretKey) signingKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
            String actualType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            if (!expectedType.equals(actualType)) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException | JwtException e) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
}
