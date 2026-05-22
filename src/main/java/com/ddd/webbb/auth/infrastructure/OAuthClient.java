package com.ddd.webbb.auth.infrastructure;

import com.ddd.webbb.user.domain.OAuthProvider;

public interface OAuthClient {

    OAuthProvider getProvider();

    OAuthUserInfo getUserInfo(String accessToken);
}
