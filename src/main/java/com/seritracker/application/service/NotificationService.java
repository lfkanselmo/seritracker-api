package com.seritracker.application.service;

import com.seritracker.domain.exception.NotificationNotFoundException;
import com.seritracker.domain.model.Notification;
import com.seritracker.domain.port.in.NotificationUseCase;
import com.seritracker.domain.port.out.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {

    private final NotificationRepository notificationRepository;

    @Override
    public List<Notification> getUnreadNotifications(Long userId) {
        log.debug("Fetching unread notifications for userId={}", userId);
        return notificationRepository.findUnreadByUserId(userId);
    }

    @Override
    public void markAsRead(Long userId, Long notificationId) {
        log.info("Marking notification id={} as read", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                    log.warn("Notification id={} not found", notificationId);
                    return new NotificationNotFoundException(notificationId);
                });

        if (!notification.getUserId().equals(userId)) {
            log.warn("Notification id={} does not belong to userId={}", notificationId, userId);
            throw new NotificationNotFoundException(notificationId);
        }

        notificationRepository.markAsRead(notificationId);
    }
}
