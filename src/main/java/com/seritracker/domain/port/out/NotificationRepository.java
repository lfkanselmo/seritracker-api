package com.seritracker.domain.port.out;

import com.seritracker.domain.model.Notification;
import com.seritracker.domain.model.PageRequest;
import com.seritracker.domain.model.PageResult;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(Long id);
    PageResult<Notification> findUnreadByUserId(Long userId, PageRequest pageRequest);
    List<Long> findAllUserIds();
    boolean existsByUserIdAndTmdbIdAndEpisodeCode(Long userId, Integer tmdbId, String episodeCode);
    void markAsRead(Long id);
}
