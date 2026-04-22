package com.dnd.webbb.user.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    Optional<User> findByPublicIdAndDeletedAtIsNull(UUID publicId);

    boolean existsByEmailAndDeletedAtIsNull(String email);
}
