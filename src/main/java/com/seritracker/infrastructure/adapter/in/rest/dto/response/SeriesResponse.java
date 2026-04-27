package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import com.seritracker.domain.model.UserSeries;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class SeriesResponse {

    Long id;
    Integer tmdbId;
    String title;
    String posterUrl;
    String status;
    Integer rating;
    Integer watchedEpisodes;
    Integer totalEpisodes;
    String network;
    String notes;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static SeriesResponse from(UserSeries domain) {
        return SeriesResponse.builder()
                .id(domain.getId())
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
                .build();
    }
}