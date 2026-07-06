package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import com.seritracker.domain.model.UserStats;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UserStatsResponse {
    int totalEpisodesWatched;
    long totalMinutesWatched;
    int totalSeriesTracked;
    int totalSeriesCompleted;
    int currentStreakDays;
    List<BadgeResponse> badges;
    YearSummaryResponse currentYear;

    public static UserStatsResponse from(UserStats domain) {
        return UserStatsResponse.builder()
                .totalEpisodesWatched(domain.getTotalEpisodesWatched())
                .totalMinutesWatched(domain.getTotalMinutesWatched())
                .totalSeriesTracked(domain.getTotalSeriesTracked())
                .totalSeriesCompleted(domain.getTotalSeriesCompleted())
                .currentStreakDays(domain.getCurrentStreakDays())
                .badges(domain.getBadges().stream().map(BadgeResponse::from).toList())
                .currentYear(YearSummaryResponse.from(domain.getCurrentYear()))
                .build();
    }
}
