package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SeriesEpisodesSummary {
    List<SeasonProgress> seasons;
    NextEpisode nextEpisode;
}
