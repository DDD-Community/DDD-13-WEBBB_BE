package com.ddd.webbb.notification.interfaces.dto;

import com.ddd.webbb.notification.domain.Notification;
import com.ddd.webbb.notification.domain.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String actorNickname,
        Long postId,
        boolean isRead,
        LocalDateTime createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getActor() != null ? notification.getActor().getNickname() : null,
                notification.getPost().getId(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
