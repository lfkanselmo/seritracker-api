package com.seritracker.infrastructure.adapter.out.tmdb;

import com.seritracker.domain.exception.SeriesNotFoundException;
import com.seritracker.domain.model.Series;
import com.seritracker.domain.port.out.TmdbClient;
import com.seritracker.infrastructure.adapter.out.tmdb.dto.TmdbSearchResponse;
import com.seritracker.infrastructure.adapter.out.tmdb.dto.TmdbSeriesResponse;
import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbClientAdapter implements TmdbClient {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(5);

    @Value("${tmdb.base-url}")
    private String baseUrl;

    @Value("${tmdb.token}")
    private String token;

    @Value("${tmdb.image-base-url}")
    private String imageBaseUrl;

    @Override
    public List<Series> searchSeries(String query) {
        log.info("Searching TMDB for query='{}'", query);
        TmdbSearchResponse response = buildClient()
                .get()
                .uri("/search/tv?query={q}&language=es-ES", query)
                .retrieve()
                .bodyToMono(TmdbSearchResponse.class)
                .block();

        if (response == null || response.getResults() == null) {
            return Collections.emptyList();
        }

        log.debug("TMDB search returned {} results for query='{}'", response.getResults().size(), query);
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
        log.info("Fetching TMDB details for tmdbId={}", tmdbId);
        TmdbSeriesResponse response;
        try {
            response = buildClient()
                    .get()
                    .uri("/tv/{id}?language=es-ES", tmdbId)
                    .retrieve()
                    .bodyToMono(TmdbSeriesResponse.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            throw new SeriesNotFoundException(tmdbId);
        }

        if (response == null || response.getId() == null) {
            log.warn("TMDB returned an empty response for tmdbId={}", tmdbId);
            throw new SeriesNotFoundException(tmdbId);
        }

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
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(RESPONSE_TIMEOUT);

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}