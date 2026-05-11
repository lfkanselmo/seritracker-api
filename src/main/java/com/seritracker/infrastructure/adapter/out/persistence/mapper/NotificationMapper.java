package com.seritracker.infrastructure.adapter.out.persistence.mapper;

import com.seritracker.domain.model.Notification;
import com.seritracker.infrastructure.adapter.out.persistence.entity.NotificationEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public Notification toDomain(NotificationEntity entity) {
        return Notification.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tmdbId(entity.getTmdbId())
                .seriesTitle(entity.getSeriesTitle())
                .episodeCode(entity.getEpisodeCode())
                .airDate(entity.getAirDate())
                .sentAt(entity.getSentAt())
                .read(entity.getRead())
                .build();
    }

    public NotificationEntity toEntity(Notification domain) {
        return NotificationEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .tmdbId(domain.getTmdbId())
                .seriesTitle(domain.getSeriesTitle())
                .episodeCode(domain.getEpisodeCode())
                .airDate(domain.getAirDate())
                .sentAt(domain.getSentAt())
                .read(domain.getRead())
                .build();
    }
}