package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.domain.model.Notification;
import com.seritracker.domain.port.out.NotificationRepository;
import com.seritracker.infrastructure.adapter.out.persistence.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final JpaNotificationRepository jpaRepository;
    private final NotificationMapper        mapper;

    @Override
    public Notification save(Notification notification) {
        return mapper.toDomain(
                jpaRepository.save(mapper.toEntity(notification))
        );
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Notification> findUnreadByUserId(Long userId) {
        return jpaRepository
                .findByUserIdAndReadFalseOrderBySentAtDesc(userId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Long> findAllUserIds() {
        return jpaRepository.findAllDistinctUserIds();
    }

    @Override
    public boolean existsByUserIdAndTmdbIdAndEpisodeCode(Long userId, Integer tmdbId, String episodeCode) {
        return jpaRepository.existsByUserIdAndTmdbIdAndEpisodeCode(userId, tmdbId, episodeCode);
    }

    @Override
    public void markAsRead(Long id) {
        jpaRepository.markAsRead(id);
    }
}