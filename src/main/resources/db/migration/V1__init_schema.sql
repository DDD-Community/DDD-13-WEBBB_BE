-- ============================================================
-- Baseline schema (current production state)
-- Tables ordered by FK dependency.
-- Uses IF NOT EXISTS so existing local DBs are not disrupted.
-- ============================================================

CREATE TABLE IF NOT EXISTS users
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    public_id     CHAR(36)     NOT NULL,
    email         VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255),
    nickname      VARCHAR(50)  NOT NULL,
    job_type      VARCHAR(50),
    career_level  VARCHAR(50),
    is_active     BIT(1)       NOT NULL DEFAULT b'1',
    deleted_at    DATETIME(6),
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_public_id (public_id),
    UNIQUE KEY uq_users_email (email),
    UNIQUE KEY uq_users_nickname (nickname)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS board_category
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(200),
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   BIT(1)       NOT NULL DEFAULT b'1',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS post
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    category_id   BIGINT       NOT NULL,
    title         VARCHAR(200) NOT NULL,
    content       TEXT         NOT NULL,
    comment_tone  VARCHAR(30)  NOT NULL,
    view_count    INT          NOT NULL DEFAULT 0,
    like_count    INT          NOT NULL DEFAULT 0,
    comment_count INT          NOT NULL DEFAULT 0,
    is_deleted    BIT(1)       NOT NULL DEFAULT b'0',
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_post_user     FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT fk_post_category FOREIGN KEY (category_id) REFERENCES board_category (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS post_like
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    post_id    BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_post_like (post_id, user_id),
    CONSTRAINT fk_post_like_post FOREIGN KEY (post_id) REFERENCES post (id),
    CONSTRAINT fk_post_like_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS comment
(
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    post_id           BIGINT      NOT NULL,
    user_id           BIGINT      NOT NULL,
    parent_comment_id BIGINT,
    content           TEXT        NOT NULL,
    like_count        INT         NOT NULL DEFAULT 0,
    is_deleted        BIT(1)      NOT NULL DEFAULT b'0',
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_post   FOREIGN KEY (post_id)           REFERENCES post (id),
    CONSTRAINT fk_comment_user   FOREIGN KEY (user_id)           REFERENCES users (id),
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_comment_id) REFERENCES comment (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS comment_like
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    comment_id BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_comment_like (comment_id, user_id),
    CONSTRAINT fk_comment_like_comment FOREIGN KEY (comment_id) REFERENCES comment (id),
    CONSTRAINT fk_comment_like_user    FOREIGN KEY (user_id)    REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS post_emotion
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    post_id      BIGINT      NOT NULL,
    emotion_type VARCHAR(20) NOT NULL,
    user_id      BIGINT      NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_post_emotion_post (post_id),
    CONSTRAINT fk_post_emotion_post FOREIGN KEY (post_id) REFERENCES post (id),
    CONSTRAINT fk_post_emotion_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS monster
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    post_id      BIGINT      NOT NULL,
    emotion_type VARCHAR(20) NOT NULL,
    hp           INT         NOT NULL,
    max_hp       INT         NOT NULL,
    status       VARCHAR(10) NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_monster_post (post_id),
    CONSTRAINT fk_monster_post FOREIGN KEY (post_id) REFERENCES post (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS monster_hp_log
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    monster_id  BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    post_id     BIGINT      NOT NULL,
    comment_id  BIGINT,
    action_type VARCHAR(20) NOT NULL,
    hp_delta    INT         NOT NULL,
    before_hp   INT         NOT NULL,
    after_hp    INT         NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_hp_log_monster FOREIGN KEY (monster_id) REFERENCES monster (id),
    CONSTRAINT fk_hp_log_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_hp_log_post    FOREIGN KEY (post_id)    REFERENCES post (id),
    CONSTRAINT fk_hp_log_comment FOREIGN KEY (comment_id) REFERENCES comment (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS report
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    reporter_user_id BIGINT       NOT NULL,
    post_id          BIGINT,
    comment_id       BIGINT,
    report_type      VARCHAR(10)  NOT NULL,
    reason           VARCHAR(500) NOT NULL,
    status           VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    processed_at     DATETIME(6),
    created_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_user_id) REFERENCES users (id),
    CONSTRAINT fk_report_post     FOREIGN KEY (post_id)          REFERENCES post (id),
    CONSTRAINT fk_report_comment  FOREIGN KEY (comment_id)       REFERENCES comment (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS user_oauth
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    provider         VARCHAR(20)  NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_oauth_provider (provider, provider_user_id),
    CONSTRAINT fk_user_oauth_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
