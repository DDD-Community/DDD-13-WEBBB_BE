package com.ddd.webbb.global.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final String frontendUrl;

    public OAuth2LoginFailureHandler(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {

        String errorMessage = URLEncoder.encode("OAuth 인증에 실패했습니다.", StandardCharsets.UTF_8);

        String redirectUrl = frontendUrl + "/oauth/callback?error=" + errorMessage;

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
