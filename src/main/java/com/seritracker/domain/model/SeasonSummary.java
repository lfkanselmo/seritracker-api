package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SeasonSummary {
    Integer seasonNumber;
    String name;
    Integer episodeCount;
}
