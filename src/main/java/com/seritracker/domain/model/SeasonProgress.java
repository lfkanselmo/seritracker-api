package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SeasonProgress {
    Integer seasonNumber;
    String name;
    Integer episodeCount;
    Integer watchedCount;
}
