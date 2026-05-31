package com.ddd.webbb.emotion.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostEmotionRepository extends JpaRepository<PostEmotion, Long> {
    Optional<PostEmotion> findByPost_Id(Long postId);

    List<PostEmotion> findByPost_IdIn(List<Long> postIds);
}
