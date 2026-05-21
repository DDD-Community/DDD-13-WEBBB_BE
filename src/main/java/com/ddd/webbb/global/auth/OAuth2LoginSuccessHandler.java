package com.ddd.webbb.global.auth;

import com.ddd.webbb.auth.application.AuthService;
import com.ddd.webbb.auth.domain.AuthToken;
import com.ddd.webbb.auth.infrastructure.OAuthCodeStore;
import com.ddd.webbb.global.common.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final OAuthCodeStore oAuthCodeStore;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(
            AuthService authService,
            OAuthCodeStore oAuthCodeStore,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.authService = authService;
        this.oAuthCodeStore = oAuthCodeStore;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String providerName = oAuth2User.getAttribute("provider");
        String providerUserId = oAuth2User.getAttribute("providerUserId");
        String email = oAuth2User.getAttribute("email");

        try {
            AuthToken authToken =
                    authService.oauthLoginFromProvider(providerName, providerUserId, email);
            String code = oAuthCodeStore.save(authToken.accessToken(), authToken.refreshToken());
            String redirectUrl = frontendUrl + "/oauth/callback#code=" + code;
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        } catch (AppException e) {
            redirectWithError(request, response, e.getErrorCode().getMessage());
        }
    }

    private void redirectWithError(
            HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        String errorMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String redirectUrl = frontendUrl + "/oauth/callback?error=" + errorMessage;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
