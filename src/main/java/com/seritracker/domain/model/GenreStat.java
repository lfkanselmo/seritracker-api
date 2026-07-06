package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GenreStat {
    String genre;
    int episodeCount;
}
