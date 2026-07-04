package com.seritracker.domain.port.out;

import com.seritracker.domain.model.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(Long id);
    List<Notification> findUnreadByUserId(Long userId);
    List<Long> findAllUserIds();
    boolean existsByUserIdAndTmdbIdAndEpisodeCode(Long userId, Integer tmdbId, String episodeCode);
    void markAsRead(Long id);
}