package com.ddd.webbb.comment.domain;

import com.ddd.webbb.global.common.entity.BaseCreatedEntity;
import com.ddd.webbb.user.domain.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "comment_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "user_id"}))
public class CommentLike extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected CommentLike() {}

    public static CommentLike create(Comment comment, User user) {
        CommentLike commentLike = new CommentLike();
        commentLike.comment = comment;
        commentLike.user = user;
        return commentLike;
    }

    public Long getId() {
        return id;
    }

    public Comment getComment() {
        return comment;
    }

    public User getUser() {
        return user;
    }
}
