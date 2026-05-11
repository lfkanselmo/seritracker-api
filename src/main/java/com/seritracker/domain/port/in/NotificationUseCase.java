package com.seritracker.domain.port.in;

import com.seritracker.domain.model.Notification;

import java.util.List;

public interface NotificationUseCase {
    List<Notification> getUnreadNotifications(Long userId);
    void markAsRead(Long notificationId);
    void checkUpcomingEpisodes();
}