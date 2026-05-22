package com.ddd.webbb.global.auth;

import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String providerUserId;
        String email;

        switch (registrationId) {
            case "google" -> {
                providerUserId = oAuth2User.getAttribute("sub");
                email = oAuth2User.getAttribute("email");
            }
            case "kakao" -> {
                Object kakaoId = oAuth2User.getAttribute("id");
                providerUserId = kakaoId != null ? String.valueOf(kakaoId) : null;
                Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
                email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
            }
            case "naver" -> {
                Map<String, Object> response = oAuth2User.getAttribute("response");
                providerUserId = response != null ? (String) response.get("id") : null;
                email = response != null ? (String) response.get("email") : null;
            }
            default ->
                    throw new OAuth2AuthenticationException(
                            "Unsupported provider: " + registrationId);
        }

        if (providerUserId == null || providerUserId.isBlank()) {
            throw new OAuth2AuthenticationException(
                    "Failed to retrieve user ID from " + registrationId);
        }
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    "Failed to retrieve email from "
                            + registrationId
                            + ". Please grant email permission.");
        }

        Map<String, Object> attributes =
                Map.of(
                        "provider", registrationId.toUpperCase(),
                        "providerUserId", providerUserId,
                        "email", email);

        return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, "providerUserId");
    }
}
