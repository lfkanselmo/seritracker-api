package com.seritracker.infrastructure.adapter.out.tmdb;

import com.seritracker.domain.model.Series;
import com.seritracker.domain.port.out.TmdbClient;
import com.seritracker.infrastructure.adapter.out.tmdb.dto.TmdbSearchResponse;
import com.seritracker.infrastructure.adapter.out.tmdb.dto.TmdbSeriesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TmdbClientAdapter implements TmdbClient {

    @Value("${tmdb.base-url}")
    private String baseUrl;

    @Value("${tmdb.token}")
    private String token;

    @Value("${tmdb.image-base-url}")
    private String imageBaseUrl;

    @Override
    public List<Series> searchSeries(String query) {
        TmdbSearchResponse response = buildClient()
                .get()
                .uri("/search/tv?query={q}&language=es-ES", query)
                .retrieve()
                .bodyToMono(TmdbSearchResponse.class)
                .block();

        if (response == null || response.getResults() == null) {
            return Collections.emptyList();
        }

        return response.getResults().stream()
                .map(r -> Series.builder()
                        .tmdbId(r.getId())
                        .title(r.getName())
                        .posterUrl(r.getPosterPath() != null
                                ? imageBaseUrl + r.getPosterPath()
                                : null)
                        .genres(Collections.emptyList())
                        .totalEpisodes(0)
                        .build())
                .toList();
    }

    @Override
    public Series getSeriesDetails(Integer tmdbId) {
        TmdbSeriesResponse response = buildClient()
                .get()
                .uri("/tv/{id}?language=es-ES", tmdbId)
                .retrieve()
                .bodyToMono(TmdbSeriesResponse.class)
                .block();

        String network = Optional.ofNullable(response.getNetworks())
                .filter(n -> !n.isEmpty())
                .map(n -> n.get(0).getName())
                .orElse(null);

        List<String> genres = Optional.ofNullable(response.getGenres())
                .map(g -> g.stream()
                        .map(TmdbSeriesResponse.TmdbGenre::getName)
                        .toList())
                .orElse(Collections.emptyList());

        return Series.builder()
                .tmdbId(response.getId())
                .title(response.getName())
                .posterUrl(response.getPosterPath() != null
                        ? imageBaseUrl + response.getPosterPath()
                        : null)
                .totalEpisodes(Optional.ofNullable(response.getNumberOfEpisodes()).orElse(0))
                .network(network)
                .genres(genres)
                .build();
    }

    private WebClient buildClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }
}