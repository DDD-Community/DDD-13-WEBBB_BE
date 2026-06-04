package com.ddd.webbb.comment.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByIdAndIsDeletedFalse(Long id);

    List<Comment> findByParentAndIsDeletedFalse(Comment parent);

    List<Comment> findByPost_IdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId);
}
