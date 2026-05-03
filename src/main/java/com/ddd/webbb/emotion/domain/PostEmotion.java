package com.ddd.webbb.emotion.domain;

import com.ddd.webbb.global.common.entity.BaseCreatedEntity;
import com.ddd.webbb.post.domain.Post;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "post_emotion",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id"}))
public class PostEmotion extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmotionType emotionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected PostEmotion() {}

    public static PostEmotion create(Post post, EmotionType emotionType, User user) {
        PostEmotion postEmotion = new PostEmotion();
        postEmotion.post = post;
        postEmotion.emotionType = emotionType;
        postEmotion.user = user;
        return postEmotion;
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public EmotionType getEmotionType() {
        return emotionType;
    }

    public User getUser() {
        return user;
    }
}
