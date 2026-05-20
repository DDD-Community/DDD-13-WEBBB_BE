package com.ddd.webbb.post.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByIdAndIsDeletedFalse(Long id);
}
