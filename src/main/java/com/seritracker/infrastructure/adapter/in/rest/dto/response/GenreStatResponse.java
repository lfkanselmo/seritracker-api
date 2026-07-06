package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import com.seritracker.domain.model.GenreStat;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GenreStatResponse {
    String genre;
    int episodeCount;

    public static GenreStatResponse from(GenreStat domain) {
        return GenreStatResponse.builder()
                .genre(domain.getGenre())
                .episodeCount(domain.getEpisodeCount())
                .build();
    }
}
