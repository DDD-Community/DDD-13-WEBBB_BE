CREATE TABLE notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    receiver_id BIGINT      NOT NULL,
    actor_id    BIGINT      NULL,
    post_id     BIGINT      NOT NULL,
    type        VARCHAR(30) NOT NULL,
    is_read     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6) NOT NULL,
    FOREIGN KEY (receiver_id) REFERENCES users (id),
    FOREIGN KEY (actor_id) REFERENCES users (id),
    FOREIGN KEY (post_id) REFERENCES post (id),
    INDEX idx_notifications_receiver (receiver_id),
    INDEX idx_notifications_receiver_unread (receiver_id, is_read)
);
