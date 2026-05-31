package com.ddd.webbb.post.domain;

import com.ddd.webbb.category.domain.BoardCategory;
import com.ddd.webbb.global.common.entity.BaseEntity;
import com.ddd.webbb.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "post")
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private BoardCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommentTone commentTone;

    @Column(nullable = false)
    private int viewCount = 0;

    @Column(nullable = false)
    private int likeCount = 0;

    @Column(nullable = false)
    private int commentCount = 0;

    @Column(nullable = false)
    private boolean isDeleted = false;

    protected Post() {}

    public static Post create(
            User user,
            BoardCategory category,
            String title,
            String content,
            CommentTone commentTone) {
        Post post = new Post();
        post.user = user;
        post.category = category;
        post.title = title;
        post.content = content;
        post.commentTone = commentTone;
        return post;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void update(String content, CommentTone commentTone) {
        String normalized = content == null ? "" : content.trim();
        this.content = content;
        this.title =
                normalized.isEmpty()
                        ? "고민글"
                        : normalized.substring(0, Math.min(normalized.length(), 30));
        this.commentTone = commentTone;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public BoardCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public CommentTone getCommentTone() {
        return commentTone;
    }

    public int getViewCount() {
        return viewCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public boolean isDeleted() {
        return isDeleted;
    }
}
