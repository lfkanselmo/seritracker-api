package com.seritracker.domain.port.out;

import com.seritracker.domain.model.Notification;

import java.util.List;

public interface NotificationRepository {
    Notification save(Notification notification);
    List<Notification> findUnreadByUserId(Long userId);
    List<Long> findAllUserIds();
    boolean existsByUserIdAndTmdbIdAndEpisodeCode(Long userId, Integer tmdbId, String episodeCode);
    void markAsRead(Long id);
}