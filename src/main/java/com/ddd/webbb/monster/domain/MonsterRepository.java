package com.ddd.webbb.monster.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonsterRepository extends JpaRepository<Monster, Long> {
    Optional<Monster> findByPostId(Long postId);
}
