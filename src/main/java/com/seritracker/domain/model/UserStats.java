package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserStats {
    int totalEpisodesWatched;
    long totalMinutesWatched;
    int totalSeriesTracked;
    int totalSeriesCompleted;
    YearSummary currentYear;
}
