package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class YearSummary {
    int year;
    int episodesWatched;
    List<GenreStat> topGenres;
    String mostWatchedSeriesTitle;
    Integer mostWatchedSeriesEpisodeCount;
    int longestStreakDays;
}
