package com.ddd.webbb.post.domain;

import com.ddd.webbb.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndUser(Post post, User user);

    boolean existsByPostAndUser(Post post, User user);

    List<PostLike> findByPost_IdInAndUser(List<Long> postIds, User user);
}
