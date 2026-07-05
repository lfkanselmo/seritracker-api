package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import com.seritracker.domain.model.NextEpisode;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class NextEpisodeResponse {

    Integer seasonNumber;
    Integer episodeNumber;
    String title;
    LocalDate airDate;

    public static NextEpisodeResponse from(NextEpisode domain) {
        if (domain == null) return null;

        return NextEpisodeResponse.builder()
                .seasonNumber(domain.getSeasonNumber())
                .episodeNumber(domain.getEpisodeNumber())
                .title(domain.getTitle())
                .airDate(domain.getAirDate())
                .build();
    }
}
