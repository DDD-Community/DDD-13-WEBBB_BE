package com.ddd.webbb.user.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOauthRepository extends JpaRepository<UserOauth, Long> {

    Optional<UserOauth> findByProviderAndProviderUserId(
            OAuthProvider provider, String providerUserId);

    boolean existsByUserIdAndProvider(Long userId, OAuthProvider provider);

    Optional<UserOauth> findByUserIdAndProvider(Long userId, OAuthProvider provider);

    long countByUserId(Long userId);
}
