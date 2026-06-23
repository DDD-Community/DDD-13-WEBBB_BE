package com.ddd.webbb.notification.domain;

import com.ddd.webbb.user.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByReceiverOrderByCreatedAtDesc(User receiver);

    boolean existsByReceiverAndIsReadFalse(User receiver);
}
