package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class EpisodeWatch {
    Long id;
    Long userSeriesId;
    Integer seasonNumber;
    Integer episodeNumber;
    LocalDateTime watchedAt;
}
