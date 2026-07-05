package com.seritracker.infrastructure.adapter.out.persistence.mapper;

import com.seritracker.domain.model.EpisodeWatch;
import com.seritracker.infrastructure.adapter.out.persistence.entity.EpisodeWatchEntity;
import org.springframework.stereotype.Component;

@Component
public class EpisodeWatchMapper {

    public EpisodeWatch toDomain(EpisodeWatchEntity entity) {
        return EpisodeWatch.builder()
                .id(entity.getId())
                .userSeriesId(entity.getUserSeriesId())
                .seasonNumber(entity.getSeasonNumber())
                .episodeNumber(entity.getEpisodeNumber())
                .watchedAt(entity.getWatchedAt())
                .build();
    }

    public EpisodeWatchEntity toEntity(EpisodeWatch domain) {
        return EpisodeWatchEntity.builder()
                .id(domain.getId())
                .userSeriesId(domain.getUserSeriesId())
                .seasonNumber(domain.getSeasonNumber())
                .episodeNumber(domain.getEpisodeNumber())
                .watchedAt(domain.getWatchedAt())
                .build();
    }
}
