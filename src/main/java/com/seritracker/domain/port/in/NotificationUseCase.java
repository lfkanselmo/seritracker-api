package com.seritracker.domain.port.in;

import com.seritracker.domain.model.Notification;
import com.seritracker.domain.model.PageRequest;
import com.seritracker.domain.model.PageResult;

public interface NotificationUseCase {
    PageResult<Notification> getUnreadNotifications(Long userId, PageRequest pageRequest);
    void markAsRead(Long userId, Long notificationId);
}
