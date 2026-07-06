package com.seritracker.application.service;

import com.seritracker.domain.model.EpisodeWatch;
import com.seritracker.domain.model.Series;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.model.UserStats;
import com.seritracker.domain.port.out.EpisodeWatchRepository;
import com.seritracker.domain.port.out.TmdbClient;
import com.seritracker.domain.port.out.UserSeriesRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatsService")
class StatsServiceTest {

    @Mock private UserSeriesRepository userSeriesRepository;
    @Mock private EpisodeWatchRepository episodeWatchRepository;
    @Mock private TmdbClient tmdbClient;

    @InjectMocks private StatsService statsService;

    private UserSeries buildSeries(Long id, Integer tmdbId, String title, SeriesStatus status) {
        return UserSeries.builder()
                .id(id)
                .userId(1L)
                .tmdbId(tmdbId)
                .title(title)
                .status(status)
                .build();
    }

    private EpisodeWatch buildWatch(Long userSeriesId, LocalDateTime watchedAt) {
        return EpisodeWatch.builder()
                .userSeriesId(userSeriesId)
                .seasonNumber(1)
                .episodeNumber(1)
                .watchedAt(watchedAt)
                .build();
    }

    private Series buildTmdbSeries(Integer runtimeMinutes, List<String> genres) {
        return Series.builder()
                .episodeRuntimeMinutes(runtimeMinutes)
                .genres(genres)
                .build();
    }

    @Nested
    @DisplayName("getStats")
    class GetStats {

        @Test
        @DisplayName("should return zeroed stats when the user has no series")
        void shouldReturnZeroedStats_whenNoSeries() {
            when(userSeriesRepository.findAllForUser(1L)).thenReturn(List.of());

            UserStats result = statsService.getStats(1L);

            assertThat(result.getTotalEpisodesWatched()).isZero();
            assertThat(result.getTotalMinutesWatched()).isZero();
            assertThat(result.getTotalSeriesTracked()).isZero();
            assertThat(result.getTotalSeriesCompleted()).isZero();
            assertThat(result.getCurrentYear().getEpisodesWatched()).isZero();
            assertThat(result.getCurrentYear().getLongestStreakDays()).isZero();
            assertThat(result.getCurrentYear().getMostWatchedSeriesTitle()).isNull();
        }

        @Test
        @DisplayName("should count total series tracked and completed regardless of watch history")
        void shouldCountSeriesTrackedAndCompleted() {
            UserSeries watching = buildSeries(1L, 1396, "Breaking Bad", SeriesStatus.WATCHING);
            UserSeries completed = buildSeries(2L, 1399, "Chernobyl", SeriesStatus.COMPLETED);
            UserSeries wantToWatch = buildSeries(3L, 1400, "Dark", SeriesStatus.WANT_TO_WATCH);

            when(userSeriesRepository.findAllForUser(1L)).thenReturn(List.of(watching, completed, wantToWatch));
            when(episodeWatchRepository.findByUserSeriesId(1L)).thenReturn(List.of());
            when(episodeWatchRepository.findByUserSeriesId(2L)).thenReturn(List.of());
            when(episodeWatchRepository.findByUserSeriesId(3L)).thenReturn(List.of());

            UserStats result = statsService.getStats(1L);

            assertThat(result.getTotalSeriesTracked()).isEqualTo(3);
            assertThat(result.getTotalSeriesCompleted()).isEqualTo(1);
        }

        @Test
        @DisplayName("should sum watched episodes and estimate total minutes from TMDB runtime")
        void shouldSumEpisodesAndEstimateMinutes() {
            UserSeries series = buildSeries(1L, 1396, "Breaking Bad", SeriesStatus.WATCHING);

            when(userSeriesRepository.findAllForUser(1L)).thenReturn(List.of(series));
            when(episodeWatchRepository.findByUserSeriesId(1L)).thenReturn(List.of(
                    buildWatch(1L, LocalDateTime.now()),
                    buildWatch(1L, LocalDateTime.now())
            ));
            when(tmdbClient.getSeriesDetails(1396)).thenReturn(buildTmdbSeries(45, List.of("Drama")));

            UserStats result = statsService.getStats(1L);

            assertThat(result.getTotalEpisodesWatched()).isEqualTo(2);
            assertThat(result.getTotalMinutesWatched()).isEqualTo(90);
        }

        @Test
        @DisplayName("should still count episodes when the TMDB lookup fails, without estimating minutes")
        void shouldCountEpisodes_whenTmdbLookupFails() {
            UserSeries series = buildSeries(1L, 1396, "Breaking Bad", SeriesStatus.WATCHING);

            when(userSeriesRepository.findAllForUser(1L)).thenReturn(List.of(series));
            when(episodeWatchRepository.findByUserSeriesId(1L)).thenReturn(List.of(buildWatch(1L, LocalDateTime.now())));
            when(tmdbClient.getSeriesDetails(1396)).thenThrow(new RuntimeException("TMDB unavailable"));

            UserStats result = statsService.getStats(1L);

            assertThat(result.getTotalEpisodesWatched()).isEqualTo(1);
            assertThat(result.getTotalMinutesWatched()).isZero();
        }

        @Test
        @DisplayName("should exclude watches from previous years from the current-year summary")
        void shouldExcludePreviousYearWatches_fromYearSummary() {
            UserSeries series = buildSeries(1L, 1396, "Breaking Bad", SeriesStatus.WATCHING);

            when(userSeriesRepository.findAllForUser(1L)).thenReturn(List.of(series));
            when(episodeWatchRepository.findByUserSeriesId(1L)).thenReturn(List.of(
                    buildWatch(1L, LocalDateTime.now().minusYears(1)),
                    buildWatch(1L, LocalDateTime.now())
            ));
            when(tmdbClient.getSeriesDetails(1396)).thenReturn(buildTmdbSeries(45, List.of("Drama")));

            UserStats result = statsService.getStats(1L);

            assertThat(result.getTotalEpisodesWatched()).isEqualTo(2);
            assertThat(result.getCurrentYear().getEpisodesWatched()).isEqualTo(1);
        }

        @Test
        @DisplayName("should pick the series with the most episodes watched this year")
        void shouldPickMostWatchedSeries_thisYear() {
            UserSeries seriesA = buildSeries(1L, 1396, "Breaking Bad", SeriesStatus.WATCHING);
            UserSeries seriesB = buildSeries(2L, 1399, "Chernobyl", SeriesStatus.COMPLETED);

            when(userSeriesRepository.findAllForUser(1L)).thenReturn(List.of(seriesA, seriesB));
            when(episodeWatchRepository.findByUserSeriesId(1L)).thenReturn(List.of(buildWatch(1L, LocalDateTime.now())));
            when(episodeWatchRepository.findByUserSeriesId(2L)).thenReturn(List.of(
                    buildWatch(2L, LocalDateTime.now()),
                    buildWatch(2L, LocalDateTime.now())
            ));
            when(tmdbClient.getSeriesDetails(1396)).thenReturn(buildTmdbSeries(45, List.of("Drama")));
            when(tmdbClient.getSeriesDetails(1399)).thenReturn(buildTmdbSeries(60, List.of("Drama", "History")));

            UserStats result = statsService.getStats(1L);

            assertThat(result.getCurrentYear().getMostWatchedSeriesTitle()).isEqualTo("Chernobyl");
            assertThat(result.getCurrentYear().getMostWatchedSeriesEpisodeCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should aggregate genre counts across series and cap at the top 5")
        void shouldAggregateGenres_cappedAtTop5() {
            UserSeries series = buildSeries(1L, 1396, "Breaking Bad", SeriesStatus.WATCHING);

            when(userSeriesRepository.findAllForUser(1L)).thenReturn(List.of(series));
            when(episodeWatchRepository.findByUserSeriesId(1L)).thenReturn(List.of(buildWatch(1L, LocalDateTime.now())));
            when(tmdbClient.getSeriesDetails(1396)).thenReturn(buildTmdbSeries(45,
                    List.of("Drama", "Crime", "Thriller", "Action", "Comedy", "Horror")));

            UserStats result = statsService.getStats(1L);

            assertThat(result.getCurrentYear().getTopGenres()).hasSize(5);
        }

        @Test
        @DisplayName("should compute the longest consecutive-day streak, ignoring gaps")
        void shouldComputeLongestStreak_ignoringGaps() {
            UserSeries series = buildSeries(1L, 1396, "Breaking Bad", SeriesStatus.WATCHING);
            LocalDateTime today = LocalDateTime.now().withHour(12);

            when(userSeriesRepository.findAllForUser(1L)).thenReturn(List.of(series));
            when(episodeWatchRepository.findByUserSeriesId(1L)).thenReturn(List.of(
                    buildWatch(1L, today.minusDays(10)),
                    buildWatch(1L, today.minusDays(2)),
                    buildWatch(1L, today.minusDays(1)),
                    buildWatch(1L, today)
            ));
            when(tmdbClient.getSeriesDetails(1396)).thenReturn(buildTmdbSeries(45, List.of("Drama")));

            UserStats result = statsService.getStats(1L);

            assertThat(result.getCurrentYear().getLongestStreakDays()).isEqualTo(3);
        }
    }
}
