package com.ddd.webbb.comment.domain;

import com.ddd.webbb.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByCommentAndUser(Comment comment, User user);

    boolean existsByCommentAndUser(Comment comment, User user);

    List<CommentLike> findByComment_IdInAndUser(List<Long> commentIds, User user);
}
