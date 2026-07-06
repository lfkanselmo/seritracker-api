package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UserStats {
    int totalEpisodesWatched;
    long totalMinutesWatched;
    int totalSeriesTracked;
    int totalSeriesCompleted;
    int currentStreakDays;
    List<Badge> badges;
    YearSummary currentYear;
}
