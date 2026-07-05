package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class EpisodeInfo {
    Integer seasonNumber;
    Integer episodeNumber;
    String title;
    LocalDate airDate;
    boolean watched;
}
