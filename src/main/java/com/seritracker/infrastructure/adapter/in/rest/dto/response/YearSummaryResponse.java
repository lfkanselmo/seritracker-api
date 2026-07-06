package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import com.seritracker.domain.model.YearSummary;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class YearSummaryResponse {
    int year;
    int episodesWatched;
    List<GenreStatResponse> topGenres;
    String mostWatchedSeriesTitle;
    Integer mostWatchedSeriesEpisodeCount;
    int longestStreakDays;

    public static YearSummaryResponse from(YearSummary domain) {
        return YearSummaryResponse.builder()
                .year(domain.getYear())
                .episodesWatched(domain.getEpisodesWatched())
                .topGenres(domain.getTopGenres().stream().map(GenreStatResponse::from).toList())
                .mostWatchedSeriesTitle(domain.getMostWatchedSeriesTitle())
                .mostWatchedSeriesEpisodeCount(domain.getMostWatchedSeriesEpisodeCount())
                .longestStreakDays(domain.getLongestStreakDays())
                .build();
    }
}
