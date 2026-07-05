package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.domain.model.Notification;
import com.seritracker.domain.model.PageRequest;
import com.seritracker.domain.model.PageResult;
import com.seritracker.domain.port.out.NotificationRepository;
import com.seritracker.infrastructure.adapter.out.persistence.entity.NotificationEntity;
import com.seritracker.infrastructure.adapter.out.persistence.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

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
    public PageResult<Notification> findUnreadByUserId(Long userId, PageRequest pageRequest) {
        Page<NotificationEntity> page = jpaRepository.findByUserIdAndReadFalseOrderBySentAtDesc(
                userId,
                org.springframework.data.domain.PageRequest.of(pageRequest.getPage(), pageRequest.getSize())
        );

        return PageResultMapper.from(page, mapper::toDomain);
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
