package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import com.seritracker.domain.model.SeriesEpisodesSummary;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SeasonsSummaryResponse {

    List<SeasonProgressResponse> seasons;
    NextEpisodeResponse nextEpisode;

    public static SeasonsSummaryResponse from(SeriesEpisodesSummary domain) {
        return SeasonsSummaryResponse.builder()
                .seasons(domain.getSeasons().stream().map(SeasonProgressResponse::from).toList())
                .nextEpisode(NextEpisodeResponse.from(domain.getNextEpisode()))
                .build();
    }
}
