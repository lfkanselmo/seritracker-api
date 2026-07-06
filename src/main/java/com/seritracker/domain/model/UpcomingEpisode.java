package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class UpcomingEpisode {
    Long userSeriesId;
    Integer tmdbId;
    String seriesTitle;
    String posterUrl;
    Integer seasonNumber;
    Integer episodeNumber;
    String episodeTitle;
    LocalDate airDate;
}
