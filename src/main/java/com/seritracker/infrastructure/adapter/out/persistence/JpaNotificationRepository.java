package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.infrastructure.adapter.out.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface JpaNotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Page<NotificationEntity> findByUserIdAndReadFalseOrderBySentAtDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndTmdbIdAndEpisodeCode(Long userId, Integer tmdbId, String episodeCode);

    @Modifying
    @Transactional
    @Query("UPDATE NotificationEntity n SET n.read = true WHERE n.id = :id")
    void markAsRead(Long id);
}