package com.ddd.webbb.auth.infrastructure;

import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.user.domain.OAuthProvider;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GoogleOAuthClient implements OAuthClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthClient.class);
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final RestClient restClient;

    public GoogleOAuthClient() {
        this.restClient = RestClient.create();
    }

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        try {
            JsonNode response =
                    restClient
                            .get()
                            .uri(USER_INFO_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .body(JsonNode.class);

            String id = response.get("id").asText();
            String email = response.get("email").asText();

            return new OAuthUserInfo(OAuthProvider.GOOGLE, id, email);
        } catch (Exception e) {
            log.error("Google OAuth 사용자 정보 조회 실패", e);
            throw new AppException(ErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
    }
}
