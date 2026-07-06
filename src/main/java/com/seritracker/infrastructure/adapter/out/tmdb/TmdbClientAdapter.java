package com.seritracker.infrastructure.adapter.out.tmdb;

import com.seritracker.domain.exception.SeriesNotFoundException;
import com.seritracker.domain.model.Episode;
import com.seritracker.domain.model.SeasonSummary;
import com.seritracker.domain.model.Series;
import com.seritracker.domain.port.out.TmdbClient;
import com.seritracker.infrastructure.adapter.out.tmdb.dto.TmdbSearchResponse;
import com.seritracker.infrastructure.adapter.out.tmdb.dto.TmdbSeasonResponse;
import com.seritracker.infrastructure.adapter.out.tmdb.dto.TmdbSeriesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbClientAdapter implements TmdbClient {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int RESPONSE_TIMEOUT_MS = 5000;

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
                .body(TmdbSearchResponse.class);

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
                    .body(TmdbSeriesResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
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

        List<SeasonSummary> seasons = Optional.ofNullable(response.getSeasons())
                .map(s -> s.stream()
                        .filter(season -> season.getSeasonNumber() != null && season.getSeasonNumber() >= 1)
                        .map(season -> SeasonSummary.builder()
                                .seasonNumber(season.getSeasonNumber())
                                .name(season.getName())
                                .episodeCount(Optional.ofNullable(season.getEpisodeCount()).orElse(0))
                                .build())
                        .toList())
                .orElse(Collections.emptyList());

        TmdbSeriesResponse.TmdbNextEpisodeToAir nextEpisode = response.getNextEpisodeToAir();

        return Series.builder()
                .tmdbId(response.getId())
                .title(response.getName())
                .posterUrl(response.getPosterPath() != null
                        ? imageBaseUrl + response.getPosterPath()
                        : null)
                .totalEpisodes(Optional.ofNullable(response.getNumberOfEpisodes()).orElse(0))
                .episodeRuntimeMinutes(resolveEpisodeRuntime(response))
                .network(network)
                .genres(genres)
                .seasons(seasons)
                .nextAirDate(nextEpisode != null ? parseAirDate(nextEpisode.getAirDate()) : null)
                .nextEpisodeSeasonNumber(nextEpisode != null ? nextEpisode.getSeasonNumber() : null)
                .nextEpisodeNumber(nextEpisode != null ? nextEpisode.getEpisodeNumber() : null)
                .nextEpisodeTitle(nextEpisode != null ? nextEpisode.getName() : null)
                .build();
    }

    private Integer resolveEpisodeRuntime(TmdbSeriesResponse response) {
        List<Integer> episodeRunTime = response.getEpisodeRunTime();
        if (episodeRunTime != null && !episodeRunTime.isEmpty()) {
            return episodeRunTime.get(0);
        }

        return Optional.ofNullable(response.getLastEpisodeToAir())
                .map(TmdbSeriesResponse.TmdbLastEpisodeToAir::getRuntime)
                .orElse(null);
    }

    @Override
    public List<Episode> getSeasonEpisodes(Integer tmdbId, Integer seasonNumber) {
        log.info("Fetching TMDB season {} episodes for tmdbId={}", seasonNumber, tmdbId);
        TmdbSeasonResponse response;
        try {
            response = buildClient()
                    .get()
                    .uri("/tv/{id}/season/{season}?language=es-ES", tmdbId, seasonNumber)
                    .retrieve()
                    .body(TmdbSeasonResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new SeriesNotFoundException(tmdbId);
        }

        if (response == null || response.getEpisodes() == null) {
            return Collections.emptyList();
        }

        return response.getEpisodes().stream()
                .map(e -> Episode.builder()
                        .seasonNumber(seasonNumber)
                        .episodeNumber(e.getEpisodeNumber())
                        .title(e.getName())
                        .airDate(parseAirDate(e.getAirDate()))
                        .build())
                .toList();
    }

    private LocalDate parseAirDate(String airDate) {
        if (airDate == null || airDate.isBlank()) return null;
        try {
            return LocalDate.parse(airDate);
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    private RestClient buildClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(RESPONSE_TIMEOUT_MS);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .requestFactory(requestFactory)
                .build();
    }
}
