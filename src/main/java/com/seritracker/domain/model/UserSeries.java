package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

@Value
@Builder
public class UserSeries {
    Long id;
    Long userId;
    Integer tmdbId;
    String title;
    String posterUrl;

    @With SeriesStatus status;
    @With Integer rating;
    @With Integer watchedEpisodes;

    Integer totalEpisodes;
    String network;
    @With String notes;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    Long version;
}