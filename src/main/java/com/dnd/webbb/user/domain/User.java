package com.dnd.webbb.user.domain;

import com.dnd.webbb.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    private LocalDateTime deletedAt;

    protected User() {}

    public static User create(String email, String nickname) {
        User user = new User();
        user.publicId = UUID.randomUUID();
        user.email = email;
        user.nickname = nickname;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    public void update(String nickname, UserStatus status) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (status != null) {
            this.status = status;
        }
    }

    public void withdraw() {
        this.status = UserStatus.INACTIVE;
        this.deletedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public UserStatus getStatus() {
        return status;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
