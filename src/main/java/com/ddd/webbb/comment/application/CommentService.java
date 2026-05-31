package com.ddd.webbb.comment.application;

import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.comment.domain.CommentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public List<Comment> findCommentsByPost(Long postId) {
        return commentRepository.findByPost_IdAndIsDeletedFalseOrderByCreatedAtAsc(postId);
    }
}
