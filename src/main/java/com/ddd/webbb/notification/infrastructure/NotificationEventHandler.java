package com.ddd.webbb.notification.infrastructure;

import com.ddd.webbb.notification.domain.Notification;
import com.ddd.webbb.notification.domain.NotificationRepository;
import com.ddd.webbb.notification.domain.NotificationType;
import com.ddd.webbb.notification.domain.event.CommentNotificationEvent;
import com.ddd.webbb.notification.domain.event.MonsterDefeatedNotificationEvent;
import com.ddd.webbb.notification.domain.event.PostLikeNotificationEvent;
import com.ddd.webbb.notification.interfaces.dto.NotificationResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventHandler {

    private final NotificationRepository notificationRepository;
    private final SseEmitterManager sseEmitterManager;

    public NotificationEventHandler(
            NotificationRepository notificationRepository, SseEmitterManager sseEmitterManager) {
        this.notificationRepository = notificationRepository;
        this.sseEmitterManager = sseEmitterManager;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentNotification(CommentNotificationEvent event) {
        if (event.actor().getId().equals(event.postOwner().getId())) {
            return;
        }
        Notification saved =
                notificationRepository.save(
                        Notification.of(
                                event.postOwner(),
                                event.actor(),
                                event.post(),
                                NotificationType.COMMENT));
        sseEmitterManager.send(event.postOwner().getId(), NotificationResponse.from(saved));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostLikeNotification(PostLikeNotificationEvent event) {
        if (event.actor().getId().equals(event.postOwner().getId())) {
            return;
        }
        Notification saved =
                notificationRepository.save(
                        Notification.of(
                                event.postOwner(),
                                event.actor(),
                                event.post(),
                                NotificationType.POST_LIKE));
        sseEmitterManager.send(event.postOwner().getId(), NotificationResponse.from(saved));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMonsterDefeatedNotification(MonsterDefeatedNotificationEvent event) {
        Notification saved =
                notificationRepository.save(
                        Notification.of(
                                event.postOwner(),
                                null,
                                event.post(),
                                NotificationType.MONSTER_DEFEATED));
        sseEmitterManager.send(event.postOwner().getId(), NotificationResponse.from(saved));
    }
}
