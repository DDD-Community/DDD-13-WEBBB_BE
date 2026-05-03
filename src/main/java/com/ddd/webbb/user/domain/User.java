package com.ddd.webbb.user.domain;

import com.ddd.webbb.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(length = 255)
    private String passwordHash;

    @Column(unique = true, nullable = false, length = 50)
    private String nickname;

    @Column(length = 50)
    private String jobType;

    @Column(length = 50)
    private String careerLevel;

    @Column(nullable = false)
    private boolean isActive = true;

    private LocalDateTime deletedAt;

    protected User() {}

    public static User create(String email, String nickname) {
        User user = new User();
        user.publicId = UUID.randomUUID();
        user.email = email;
        user.nickname = nickname;
        user.isActive = true;
        return user;
    }

    public void update(String nickname, String jobType, String careerLevel) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (jobType != null) {
            this.jobType = jobType;
        }
        if (careerLevel != null) {
            this.careerLevel = careerLevel;
        }
    }

    public void withdraw() {
        this.isActive = false;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public String getJobType() {
        return jobType;
    }

    public String getCareerLevel() {
        return careerLevel;
    }

    public boolean isActive() {
        return isActive;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
