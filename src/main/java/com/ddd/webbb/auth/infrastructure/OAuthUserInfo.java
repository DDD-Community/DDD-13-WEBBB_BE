package com.ddd.webbb.auth.infrastructure;

import com.ddd.webbb.user.domain.OAuthProvider;

public record OAuthUserInfo(OAuthProvider provider, String providerUserId, String email) {}
