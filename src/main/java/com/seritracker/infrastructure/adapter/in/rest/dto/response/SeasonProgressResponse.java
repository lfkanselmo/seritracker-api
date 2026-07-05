package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import com.seritracker.domain.model.SeasonProgress;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SeasonProgressResponse {

    Integer seasonNumber;
    String name;
    Integer episodeCount;
    Integer watchedCount;

    public static SeasonProgressResponse from(SeasonProgress domain) {
        return SeasonProgressResponse.builder()
                .seasonNumber(domain.getSeasonNumber())
                .name(domain.getName())
                .episodeCount(domain.getEpisodeCount())
                .watchedCount(domain.getWatchedCount())
                .build();
    }
}
