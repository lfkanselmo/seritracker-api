package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import com.seritracker.domain.model.EpisodeInfo;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class EpisodeResponse {

    Integer seasonNumber;
    Integer episodeNumber;
    String title;
    LocalDate airDate;
    boolean watched;

    public static EpisodeResponse from(EpisodeInfo domain) {
        return EpisodeResponse.builder()
                .seasonNumber(domain.getSeasonNumber())
                .episodeNumber(domain.getEpisodeNumber())
                .title(domain.getTitle())
                .airDate(domain.getAirDate())
                .watched(domain.isWatched())
                .build();
    }
}
