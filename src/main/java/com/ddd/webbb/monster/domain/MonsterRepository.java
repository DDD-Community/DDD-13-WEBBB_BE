package com.ddd.webbb.monster.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonsterRepository extends JpaRepository<Monster, Long> {
    Optional<Monster> findByPost_Id(Long postId);

    List<Monster> findByPost_IdIn(List<Long> postIds);
}
