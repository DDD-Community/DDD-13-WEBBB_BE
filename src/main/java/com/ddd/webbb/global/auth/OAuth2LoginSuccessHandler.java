package com.ddd.webbb.global.auth;

import com.ddd.webbb.auth.infrastructure.OAuthCodeStore;
import com.ddd.webbb.auth.infrastructure.RefreshTokenStore;
import com.ddd.webbb.user.domain.OAuthProvider;
import com.ddd.webbb.user.domain.User;
import com.ddd.webbb.user.domain.UserOauth;
import com.ddd.webbb.user.domain.UserOauthRepository;
import com.ddd.webbb.user.domain.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final UserOauthRepository userOauthRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final OAuthCodeStore oAuthCodeStore;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            UserOauthRepository userOauthRepository,
            JwtProvider jwtProvider,
            RefreshTokenStore refreshTokenStore,
            OAuthCodeStore oAuthCodeStore,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.userOauthRepository = userOauthRepository;
        this.jwtProvider = jwtProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.oAuthCodeStore = oAuthCodeStore;
        this.frontendUrl = frontendUrl;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String providerName = oAuth2User.getAttribute("provider");
        String providerUserId = oAuth2User.getAttribute("providerUserId");
        String email = oAuth2User.getAttribute("email");

        OAuthProvider provider = OAuthProvider.valueOf(providerName);

        Optional<UserOauth> existingOauth =
                userOauthRepository.findByProviderAndProviderUserId(provider, providerUserId);

        User user;
        if (existingOauth.isPresent()) {
            user = existingOauth.get().getUser();
            if (!user.isActive()) {
                redirectWithError(request, response, "이미 탈퇴한 회원입니다.");
                return;
            }
        } else {
            if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
                redirectWithError(
                        request, response, "이미 해당 이메일로 가입된 계정이 있습니다. 기존 계정으로 로그인 후 계정 연동을 진행해주세요.");
                return;
            }

            String nickname = generateUniqueNickname(email.split("@")[0]);
            user = User.createOAuthUser(email, nickname, null, null);
            userRepository.save(user);

            UserOauth userOauth = UserOauth.create(user, provider, providerUserId);
            userOauthRepository.save(userOauth);
        }

        String accessToken = jwtProvider.createAccessToken(user.getPublicId());
        String refreshToken = jwtProvider.createRefreshToken(user.getPublicId());
        refreshTokenStore.save(user.getPublicId(), refreshToken);

        String code = oAuthCodeStore.save(accessToken, refreshToken);

        String redirectUrl = frontendUrl + "/oauth/callback#code=" + code;

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
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

    private void redirectWithError(
            HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        String errorMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String redirectUrl = frontendUrl + "/oauth/callback?error=" + errorMessage;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
