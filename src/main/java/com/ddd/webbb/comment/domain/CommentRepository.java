package com.ddd.webbb.comment.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPost_IdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId);
}
