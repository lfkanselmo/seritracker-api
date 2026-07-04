package com.seritracker.infrastructure.adapter.out.persistence.mapper;

import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.infrastructure.adapter.out.persistence.entity.UserSeriesEntity;
import org.springframework.stereotype.Component;

@Component
public class UserSeriesMapper {

    public UserSeries toDomain(UserSeriesEntity entity) {
        return UserSeries.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tmdbId(entity.getTmdbId())
                .title(entity.getTitle())
                .posterUrl(entity.getPosterUrl())
                .status(SeriesStatus.valueOf(entity.getStatus()))
                .rating(entity.getRating())
                .watchedEpisodes(entity.getWatchedEpisodes())
                .totalEpisodes(entity.getTotalEpisodes())
                .network(entity.getNetwork())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    public UserSeriesEntity toEntity(UserSeries domain) {
        return UserSeriesEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .tmdbId(domain.getTmdbId())
                .title(domain.getTitle())
                .posterUrl(domain.getPosterUrl())
                .status(domain.getStatus().name())
                .rating(domain.getRating())
                .watchedEpisodes(domain.getWatchedEpisodes())
                .totalEpisodes(domain.getTotalEpisodes())
                .network(domain.getNetwork())
                .notes(domain.getNotes())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                .build();
    }
}