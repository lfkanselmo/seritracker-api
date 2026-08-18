package com.seritracker.application.service;

import com.seritracker.domain.model.Episode;
import com.seritracker.domain.model.EpisodeInfo;
import com.seritracker.domain.model.EpisodeWatch;
import com.seritracker.domain.model.NextEpisode;
import com.seritracker.domain.model.SeasonProgress;
import com.seritracker.domain.model.SeasonSummary;
import com.seritracker.domain.model.Series;
import com.seritracker.domain.model.SeriesEpisodesSummary;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.port.in.EpisodeTrackingUseCase;
import com.seritracker.domain.port.out.EpisodeWatchRepository;
import com.seritracker.domain.port.out.TmdbClient;
import com.seritracker.domain.port.out.UserSeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeTrackingService implements EpisodeTrackingUseCase {

    private final UserSeriesRepository userSeriesRepository;
    private final EpisodeWatchRepository episodeWatchRepository;
    private final TmdbClient tmdbClient;

    @Override
    public SeriesEpisodesSummary getSeasonsSummary(Long userId, Long seriesId) {
        UserSeries series = findOrThrow(userId, seriesId);
        List<SeasonSummary> seasons = fetchSeasons(series.getTmdbId());

        backfillIfNeeded(series, seasons);

        List<EpisodeWatch> watched = episodeWatchRepository.findByUserSeriesId(seriesId);
        List<SeasonProgress> seasonProgress = buildSeasonProgress(seasons, watched);
        NextEpisode nextEpisode = computeNextEpisode(series.getTmdbId(), seasons, toKeys(watched));

        return SeriesEpisodesSummary.builder()
                .seasons(seasonProgress)
                .nextEpisode(nextEpisode)
                .build();
    }

    private List<SeasonProgress> buildSeasonProgress(List<SeasonSummary> seasons, List<EpisodeWatch> watched) {
        Map<Integer, Long> watchedCountBySeason = watched.stream()
                .collect(Collectors.groupingBy(EpisodeWatch::getSeasonNumber, Collectors.counting()));

        return seasons.stream()
                .map(s -> SeasonProgress.builder()
                        .seasonNumber(s.getSeasonNumber())
                        .name(s.getName())
                        .episodeCount(s.getEpisodeCount())
                        .watchedCount(watchedCountBySeason.getOrDefault(s.getSeasonNumber(), 0L).intValue())
                        .build())
                .toList();
    }

    @Override
    public List<EpisodeInfo> getSeasonEpisodes(Long userId, Long seriesId, Integer seasonNumber) {
        UserSeries series = findOrThrow(userId, seriesId);
        List<Episode> episodes = tmdbClient.getSeasonEpisodes(series.getTmdbId(), seasonNumber);

        Set<Integer> watchedEpisodeNumbers = episodeWatchRepository.findByUserSeriesId(seriesId).stream()
                .filter(w -> w.getSeasonNumber().equals(seasonNumber))
                .map(EpisodeWatch::getEpisodeNumber)
                .collect(Collectors.toSet());

        return episodes.stream()
                .map(e -> EpisodeInfo.builder()
                        .seasonNumber(e.getSeasonNumber())
                        .episodeNumber(e.getEpisodeNumber())
                        .title(e.getTitle())
                        .airDate(e.getAirDate())
                        .watched(watchedEpisodeNumbers.contains(e.getEpisodeNumber()))
                        .build())
                .toList();
    }

    @Override
    public UserSeries markEpisode(Long userId, Long seriesId, Integer seasonNumber, Integer episodeNumber, boolean watched) {
        UserSeries series = findOrThrow(userId, seriesId);
        log.info("Marking series id={} season={} episode={} watched={}", seriesId, seasonNumber, episodeNumber, watched);

        if (watched) {
            boolean alreadyWatched = episodeWatchRepository.findByUserSeriesId(seriesId).stream()
                    .anyMatch(w -> w.getSeasonNumber().equals(seasonNumber) && w.getEpisodeNumber().equals(episodeNumber));
            if (!alreadyWatched) {
                episodeWatchRepository.save(EpisodeWatch.builder()
                        .userSeriesId(seriesId)
                        .seasonNumber(seasonNumber)
                        .episodeNumber(episodeNumber)
                        .build());
            }
        } else {
            episodeWatchRepository.deleteByUserSeriesIdAndSeasonNumberAndEpisodeNumber(seriesId, seasonNumber, episodeNumber);
        }

        return refreshWatchedCount(series);
    }

    @Override
    public UserSeries markEpisodesWatched(Long userId, Long seriesId, Integer seasonNumber, List<Integer> episodeNumbers) {
        UserSeries series = findOrThrow(userId, seriesId);
        log.info("Marking series id={} season={} episodes={} as watched", seriesId, seasonNumber, episodeNumbers);

        Set<Integer> alreadyWatched = episodeWatchRepository.findByUserSeriesId(seriesId).stream()
                .filter(w -> w.getSeasonNumber().equals(seasonNumber))
                .map(EpisodeWatch::getEpisodeNumber)
                .collect(Collectors.toSet());

        List<EpisodeWatch> toInsert = episodeNumbers.stream()
                .filter(ep -> !alreadyWatched.contains(ep))
                .map(ep -> EpisodeWatch.builder()
                        .userSeriesId(seriesId)
                        .seasonNumber(seasonNumber)
                        .episodeNumber(ep)
                        .build())
                .toList();

        if (!toInsert.isEmpty()) {
            episodeWatchRepository.saveAll(toInsert);
        }

        return refreshWatchedCount(series);
    }

    private List<SeasonSummary> fetchSeasons(Integer tmdbId) {
        Series tmdbData = tmdbClient.getSeriesDetails(tmdbId);
        return tmdbData.getSeasons() != null ? tmdbData.getSeasons() : Collections.emptyList();
    }

    /**
     * Backfillea el contador plano "watchedEpisodes" heredado (de cuando
     * sólo existía el stepper +/-) como los primeros N episodios en orden
     * — es la única interpretación razonable, ya que ese stepper nunca
     * permitía marcar episodios salteados.
     */
    private void backfillIfNeeded(UserSeries series, List<SeasonSummary> seasons) {
        if (episodeWatchRepository.countByUserSeriesId(series.getId()) > 0) return;

        int watchedCount = Optional.ofNullable(series.getWatchedEpisodes()).orElse(0);
        if (watchedCount <= 0) return;

        log.info("Backfilling {} watched episodes for seriesId={} from legacy counter", watchedCount, series.getId());

        List<EpisodeWatch> toInsert = buildBackfillWatches(series.getId(), seasons, watchedCount);
        if (!toInsert.isEmpty()) {
            episodeWatchRepository.saveAll(toInsert);
        }
    }

    private List<EpisodeWatch> buildBackfillWatches(Long seriesId, List<SeasonSummary> seasons, int watchedCount) {
        List<EpisodeWatch> toInsert = new ArrayList<>();
        int remaining = watchedCount;
        for (SeasonSummary season : seasons) {
            if (remaining <= 0) break;
            int episodesInSeason = Math.min(remaining, season.getEpisodeCount());
            for (int ep = 1; ep <= episodesInSeason; ep++) {
                toInsert.add(EpisodeWatch.builder()
                        .userSeriesId(seriesId)
                        .seasonNumber(season.getSeasonNumber())
                        .episodeNumber(ep)
                        .build());
            }
            remaining -= episodesInSeason;
        }
        return toInsert;
    }

    private NextEpisode computeNextEpisode(Integer tmdbId, List<SeasonSummary> seasons, Set<String> watchedKeys) {
        UnwatchedSlot slot = findFirstUnwatched(seasons, watchedKeys);
        if (slot == null) return null;

        Episode episode = fetchEpisode(tmdbId, slot.seasonNumber(), slot.episodeNumber());
        if (episode == null) return null;

        return NextEpisode.builder()
                .seasonNumber(slot.seasonNumber())
                .episodeNumber(slot.episodeNumber())
                .title(episode.getTitle())
                .airDate(episode.getAirDate())
                .build();
    }

    private record UnwatchedSlot(Integer seasonNumber, Integer episodeNumber) {}

    private UnwatchedSlot findFirstUnwatched(List<SeasonSummary> seasons, Set<String> watchedKeys) {
        for (SeasonSummary season : seasons) {
            for (int ep = 1; ep <= season.getEpisodeCount(); ep++) {
                if (!watchedKeys.contains(key(season.getSeasonNumber(), ep))) {
                    return new UnwatchedSlot(season.getSeasonNumber(), ep);
                }
            }
        }
        return null;
    }

    private Episode fetchEpisode(Integer tmdbId, Integer seasonNumber, Integer episodeNumber) {
        return tmdbClient.getSeasonEpisodes(tmdbId, seasonNumber).stream()
                .filter(e -> e.getEpisodeNumber().equals(episodeNumber))
                .findFirst()
                .orElse(null);
    }

    private UserSeries refreshWatchedCount(UserSeries series) {
        int count = (int) episodeWatchRepository.countByUserSeriesId(series.getId());
        return userSeriesRepository.save(series.withWatchedEpisodes(count));
    }

    private Set<String> toKeys(List<EpisodeWatch> watched) {
        return watched.stream()
                .map(w -> key(w.getSeasonNumber(), w.getEpisodeNumber()))
                .collect(Collectors.toSet());
    }

    private String key(Integer season, Integer episode) {
        return season + ":" + episode;
    }

    private UserSeries findOrThrow(Long userId, Long id) {
        return SeriesLookup.findOrThrow(userSeriesRepository, userId, id);
    }
}
