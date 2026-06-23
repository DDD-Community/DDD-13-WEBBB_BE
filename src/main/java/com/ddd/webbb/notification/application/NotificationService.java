package com.ddd.webbb.notification.application;

import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.notification.domain.Notification;
import com.ddd.webbb.notification.domain.NotificationRepository;
import com.ddd.webbb.notification.interfaces.dto.NotificationResponse;
import com.ddd.webbb.notification.interfaces.dto.UnreadNotificationResponse;
import com.ddd.webbb.user.application.UserService;
import com.ddd.webbb.user.domain.User;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public NotificationService(
            NotificationRepository notificationRepository, UserService userService) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
    }

    public List<NotificationResponse> getNotifications(UUID userPublicId) {
        User user = userService.getUserEntity(userPublicId);
        return notificationRepository.findByReceiverOrderByCreatedAtDesc(user).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public void markAsRead(UUID userPublicId, Long notificationId) {
        User user = userService.getUserEntity(userPublicId);
        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getReceiver().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        notification.markAsRead();
    }

    public UnreadNotificationResponse hasUnread(UUID userPublicId) {
        User user = userService.getUserEntity(userPublicId);
        return new UnreadNotificationResponse(
                notificationRepository.existsByReceiverAndIsReadFalse(user));
    }
}
