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
public class NaverOAuthClient implements OAuthClient {

    private static final Logger log = LoggerFactory.getLogger(NaverOAuthClient.class);
    private static final String USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    private final RestClient restClient;

    public NaverOAuthClient() {
        this.restClient = RestClient.create();
    }

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.NAVER;
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

            JsonNode responseBody = response.get("response");
            String id = responseBody.get("id").asText();
            String email = responseBody.get("email").asText();

            return new OAuthUserInfo(OAuthProvider.NAVER, id, email);
        } catch (Exception e) {
            log.error("Naver OAuth 사용자 정보 조회 실패", e);
            throw new AppException(ErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
    }
}
