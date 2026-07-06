package com.seritracker.application.service;

import com.seritracker.domain.model.Series;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UpcomingEpisode;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.port.in.CalendarUseCase;
import com.seritracker.domain.port.out.TmdbClient;
import com.seritracker.domain.port.out.UserSeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService implements CalendarUseCase {

    private final UserSeriesRepository userSeriesRepository;
    private final TmdbClient tmdbClient;

    @Override
    public List<UpcomingEpisode> getUpcomingEpisodes(Long userId) {
        List<UserSeries> watchingSeries = userSeriesRepository.findAllByUserIdAndStatus(userId, SeriesStatus.WATCHING);

        return watchingSeries.stream()
                .map(this::toUpcomingEpisode)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(UpcomingEpisode::getAirDate))
                .toList();
    }

    private Optional<UpcomingEpisode> toUpcomingEpisode(UserSeries series) {
        try {
            Series tmdbData = tmdbClient.getSeriesDetails(series.getTmdbId());
            LocalDate nextAirDate = tmdbData.getNextAirDate();

            if (nextAirDate == null || nextAirDate.isBefore(LocalDate.now())) {
                return Optional.empty();
            }

            return Optional.of(UpcomingEpisode.builder()
                    .userSeriesId(series.getId())
                    .tmdbId(series.getTmdbId())
                    .seriesTitle(series.getTitle())
                    .posterUrl(series.getPosterUrl())
                    .seasonNumber(tmdbData.getNextEpisodeSeasonNumber())
                    .episodeNumber(tmdbData.getNextEpisodeNumber())
                    .episodeTitle(tmdbData.getNextEpisodeTitle())
                    .airDate(nextAirDate)
                    .build());

        } catch (Exception e) {
            log.error("Failed to fetch upcoming episode for tmdbId={}", series.getTmdbId(), e);
            return Optional.empty();
        }
    }
}
