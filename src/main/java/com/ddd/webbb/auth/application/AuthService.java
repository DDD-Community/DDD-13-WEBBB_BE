package com.ddd.webbb.auth.application;

import com.ddd.webbb.auth.domain.AuthToken;
import com.ddd.webbb.auth.infrastructure.OAuthClient;
import com.ddd.webbb.auth.infrastructure.OAuthUserInfo;
import com.ddd.webbb.auth.infrastructure.RefreshTokenStore;
import com.ddd.webbb.auth.interfaces.dto.AuthResponse;
import com.ddd.webbb.auth.interfaces.dto.AuthResponse.TokenInfo;
import com.ddd.webbb.auth.interfaces.dto.AuthResponse.UserInfo;
import com.ddd.webbb.auth.interfaces.dto.EmailLoginRequest;
import com.ddd.webbb.auth.interfaces.dto.EmailSignupRequest;
import com.ddd.webbb.auth.interfaces.dto.OAuthLoginRequest;
import com.ddd.webbb.global.auth.JwtProvider;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.user.domain.OAuthProvider;
import com.ddd.webbb.user.domain.User;
import com.ddd.webbb.user.domain.UserOauth;
import com.ddd.webbb.user.domain.UserOauthRepository;
import com.ddd.webbb.user.domain.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final UserOauthRepository userOauthRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;
    private final Map<OAuthProvider, OAuthClient> oauthClients;

    public AuthService(
            UserRepository userRepository,
            UserOauthRepository userOauthRepository,
            JwtProvider jwtProvider,
            PasswordEncoder passwordEncoder,
            RefreshTokenStore refreshTokenStore,
            List<OAuthClient> oauthClientList) {
        this.userRepository = userRepository;
        this.userOauthRepository = userOauthRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenStore = refreshTokenStore;
        this.oauthClients =
                oauthClientList.stream()
                        .collect(Collectors.toMap(OAuthClient::getProvider, Function.identity()));
    }

    @Transactional
    public AuthResponse signupEmail(EmailSignupRequest request) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new AppException(ErrorCode.DUPLICATED_EMAIL);
        }

        String nickname = normalizeNullable(request.nickname());
        if (nickname != null && userRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
            throw new AppException(ErrorCode.DUPLICATED_NICKNAME);
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user =
                User.createWithPassword(
                        request.email(),
                        nickname,
                        encodedPassword,
                        request.jobRole() != null ? request.jobRole().name() : null,
                        request.careerYear() != null ? request.careerYear().name() : null);
        userRepository.save(user);

        AuthToken authToken = issueTokens(user.getPublicId());
        return new AuthResponse(UserInfo.from(user), TokenInfo.from(authToken), true);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailAndDeletedAtIsNull(email);
    }

    public AuthResponse loginEmail(EmailLoginRequest request) {
        User user =
                userRepository
                        .findByEmailAndDeletedAtIsNull(request.email())
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!user.isActive()) {
            throw new AppException(ErrorCode.ALREADY_WITHDRAWN_USER);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        AuthToken authToken = issueTokens(user.getPublicId());
        return new AuthResponse(UserInfo.from(user), TokenInfo.from(authToken), false);
    }

    @Transactional
    public AuthResponse oauthLogin(String providerName, OAuthLoginRequest request) {
        OAuthProvider provider = parseProvider(providerName);
        OAuthClient client = getOAuthClient(provider);

        OAuthUserInfo oauthUserInfo = client.getUserInfo(request.oauthAccessToken());
        validateOAuthUserInfo(oauthUserInfo);

        Optional<UserOauth> existingOauth =
                userOauthRepository.findByProviderAndProviderUserId(
                        oauthUserInfo.provider(), oauthUserInfo.providerUserId());

        if (existingOauth.isPresent()) {
            User user = existingOauth.get().getUser();
            if (!user.isActive()) {
                throw new AppException(ErrorCode.ALREADY_WITHDRAWN_USER);
            }
            AuthToken authToken = issueTokens(user.getPublicId());
            return new AuthResponse(UserInfo.from(user), TokenInfo.from(authToken), false);
        }

        if (userRepository.existsByEmailAndDeletedAtIsNull(oauthUserInfo.email())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS_LINK_REQUIRED);
        }

        String nickname = request.nickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = generateUniqueNickname(oauthUserInfo.email().split("@")[0]);
        }

        User user =
                User.createOAuthUser(
                        oauthUserInfo.email(),
                        nickname,
                        request.jobRole() != null ? request.jobRole().name() : null,
                        request.careerYear() != null ? request.careerYear().name() : null);
        userRepository.save(user);

        UserOauth userOauth =
                UserOauth.create(user, oauthUserInfo.provider(), oauthUserInfo.providerUserId());
        userOauthRepository.save(userOauth);

        AuthToken authToken = issueTokens(user.getPublicId());
        return new AuthResponse(UserInfo.from(user), TokenInfo.from(authToken), true);
    }

    @Transactional
    public AuthToken oauthLoginFromProvider(
            String providerName, String providerUserId, String email) {
        OAuthProvider provider = OAuthProvider.valueOf(providerName);

        Optional<UserOauth> existingOauth =
                userOauthRepository.findByProviderAndProviderUserId(provider, providerUserId);

        if (existingOauth.isPresent()) {
            User user = existingOauth.get().getUser();
            if (!user.isActive()) {
                throw new AppException(ErrorCode.ALREADY_WITHDRAWN_USER);
            }
            return issueTokens(user.getPublicId());
        }

        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS_LINK_REQUIRED);
        }

        String nickname = generateUniqueNickname(email.split("@")[0]);
        User user = User.createOAuthUser(email, nickname, null, null);
        userRepository.save(user);

        UserOauth userOauth = UserOauth.create(user, provider, providerUserId);
        userOauthRepository.save(userOauth);

        return issueTokens(user.getPublicId());
    }

    public TokenInfo refresh(String refreshToken) {
        if (!jwtProvider.validate(refreshToken)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        UUID publicId = jwtProvider.getPublicId(refreshToken);
        String storedToken = refreshTokenStore.find(publicId);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        AuthToken authToken = issueTokens(publicId);
        return TokenInfo.from(authToken);
    }

    @Transactional
    public void linkOAuth(UUID publicId, String providerName, String oauthAccessToken) {
        OAuthProvider provider = parseProvider(providerName);
        OAuthClient client = getOAuthClient(provider);

        User user =
                userRepository
                        .findByPublicIdAndDeletedAtIsNull(publicId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (userOauthRepository.existsByUserIdAndProvider(user.getId(), provider)) {
            throw new AppException(ErrorCode.OAUTH_PROVIDER_ALREADY_LINKED);
        }

        OAuthUserInfo oauthUserInfo = client.getUserInfo(oauthAccessToken);
        validateOAuthUserInfo(oauthUserInfo);

        Optional<UserOauth> existingOauth =
                userOauthRepository.findByProviderAndProviderUserId(
                        oauthUserInfo.provider(), oauthUserInfo.providerUserId());
        if (existingOauth.isPresent()) {
            throw new AppException(ErrorCode.OAUTH_ALREADY_LINKED);
        }

        UserOauth userOauth =
                UserOauth.create(user, oauthUserInfo.provider(), oauthUserInfo.providerUserId());
        userOauthRepository.save(userOauth);
    }

    @Transactional
    public void unlinkOAuth(UUID publicId, String providerName) {
        OAuthProvider provider = parseProvider(providerName);

        User user =
                userRepository
                        .findByPublicIdAndDeletedAtIsNull(publicId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        UserOauth userOauth =
                userOauthRepository
                        .findByUserIdAndProvider(user.getId(), provider)
                        .orElseThrow(() -> new AppException(ErrorCode.OAUTH_PROVIDER_NOT_LINKED));

        boolean hasPassword = user.getPasswordHash() != null;
        long oauthCount = userOauthRepository.countByUserId(user.getId());

        if (!hasPassword && oauthCount <= 1) {
            throw new AppException(ErrorCode.CANNOT_UNLINK_LAST_AUTH);
        }

        userOauthRepository.delete(userOauth);
    }

    public void logout(UUID publicId) {
        refreshTokenStore.delete(publicId);
    }

    private void validateOAuthUserInfo(OAuthUserInfo oauthUserInfo) {
        if (oauthUserInfo.email() == null
                || oauthUserInfo.email().isBlank()
                || oauthUserInfo.providerUserId() == null
                || oauthUserInfo.providerUserId().isBlank()) {
            throw new AppException(ErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
    }

    private OAuthProvider parseProvider(String providerName) {
        try {
            return OAuthProvider.valueOf(providerName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
    }

    private OAuthClient getOAuthClient(OAuthProvider provider) {
        OAuthClient client = oauthClients.get(provider);
        if (client == null) {
            throw new AppException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
        return client;
    }

    private String generateUniqueNickname(String base) {
        if (!userRepository.existsByNicknameAndDeletedAtIsNull(base)) {
            return base;
        }
        for (int i = 0; i < 10; i++) {
            String candidate = base + "_" + UUID.randomUUID().toString().substring(0, 6);
            if (!userRepository.existsByNicknameAndDeletedAtIsNull(candidate)) {
                return candidate;
            }
        }
        return base + "_" + UUID.randomUUID().toString().substring(0, 12);
    }

    private AuthToken issueTokens(UUID publicId) {
        String accessToken = jwtProvider.createAccessToken(publicId);
        String refreshToken = jwtProvider.createRefreshToken(publicId);
        refreshTokenStore.save(publicId, refreshToken);
        return new AuthToken(accessToken, refreshToken);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
