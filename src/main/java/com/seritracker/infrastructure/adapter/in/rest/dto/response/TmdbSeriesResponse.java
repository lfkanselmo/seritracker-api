package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import com.seritracker.domain.model.Series;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TmdbSeriesResponse {

    Integer tmdbId;
    String title;
    String posterUrl;
    List<String> genres;
    String network;
    Integer totalEpisodes;

    public static TmdbSeriesResponse from(Series domain) {
        return TmdbSeriesResponse.builder()
                .tmdbId(domain.getTmdbId())
                .title(domain.getTitle())
                .posterUrl(domain.getPosterUrl())
                .genres(domain.getGenres())
                .network(domain.getNetwork())
                .totalEpisodes(domain.getTotalEpisodes())
                .build();
    }
}