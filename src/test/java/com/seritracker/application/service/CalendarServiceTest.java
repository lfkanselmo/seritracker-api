package com.seritracker.application.service;

import com.seritracker.domain.model.Series;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UpcomingEpisode;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.port.out.TmdbClient;
import com.seritracker.domain.port.out.UserSeriesRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarService")
class CalendarServiceTest {

    @Mock private UserSeriesRepository userSeriesRepository;
    @Mock private TmdbClient tmdbClient;

    @InjectMocks private CalendarService calendarService;

    private UserSeries buildWatchingSeries(Long id, Integer tmdbId, String title) {
        return UserSeries.builder()
                .id(id)
                .userId(1L)
                .tmdbId(tmdbId)
                .title(title)
                .posterUrl("https://image.tmdb.org/t/p/w300/" + tmdbId + ".jpg")
                .status(SeriesStatus.WATCHING)
                .build();
    }

    private Series buildTmdbSeries(LocalDate nextAirDate, Integer season, Integer episode, String episodeTitle) {
        return Series.builder()
                .nextAirDate(nextAirDate)
                .nextEpisodeSeasonNumber(season)
                .nextEpisodeNumber(episode)
                .nextEpisodeTitle(episodeTitle)
                .build();
    }

    @Nested
    @DisplayName("getUpcomingEpisodes")
    class GetUpcomingEpisodes {

        @Test
        @DisplayName("should return upcoming episodes sorted by air date")
        void shouldReturnUpcomingEpisodes_sortedByAirDate() {
            UserSeries seriesA = buildWatchingSeries(1L, 1396, "Breaking Bad");
            UserSeries seriesB = buildWatchingSeries(2L, 1399, "Game of Thrones");

            when(userSeriesRepository.findAllByUserIdAndStatus(1L, SeriesStatus.WATCHING))
                    .thenReturn(List.of(seriesA, seriesB));
            when(tmdbClient.getSeriesDetails(1396))
                    .thenReturn(buildTmdbSeries(LocalDate.now().plusDays(5), 6, 1, "Felina"));
            when(tmdbClient.getSeriesDetails(1399))
                    .thenReturn(buildTmdbSeries(LocalDate.now().plusDays(1), 8, 6, "The Iron Throne"));

            List<UpcomingEpisode> result = calendarService.getUpcomingEpisodes(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getSeriesTitle()).isEqualTo("Game of Thrones");
            assertThat(result.get(1).getSeriesTitle()).isEqualTo("Breaking Bad");
        }

        @Test
        @DisplayName("should skip series with no next air date")
        void shouldSkipSeries_withNoNextAirDate() {
            UserSeries series = buildWatchingSeries(1L, 1396, "Breaking Bad");

            when(userSeriesRepository.findAllByUserIdAndStatus(1L, SeriesStatus.WATCHING))
                    .thenReturn(List.of(series));
            when(tmdbClient.getSeriesDetails(1396))
                    .thenReturn(buildTmdbSeries(null, null, null, null));

            List<UpcomingEpisode> result = calendarService.getUpcomingEpisodes(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should skip series whose next air date is in the past")
        void shouldSkipSeries_whenAirDateIsInThePast() {
            UserSeries series = buildWatchingSeries(1L, 1396, "Breaking Bad");

            when(userSeriesRepository.findAllByUserIdAndStatus(1L, SeriesStatus.WATCHING))
                    .thenReturn(List.of(series));
            when(tmdbClient.getSeriesDetails(1396))
                    .thenReturn(buildTmdbSeries(LocalDate.now().minusDays(2), 6, 1, "Felina"));

            List<UpcomingEpisode> result = calendarService.getUpcomingEpisodes(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should skip a series when TMDB lookup fails, without affecting the others")
        void shouldSkipSeries_whenTmdbLookupFails() {
            UserSeries failing = buildWatchingSeries(1L, 1396, "Breaking Bad");
            UserSeries ok = buildWatchingSeries(2L, 1399, "Game of Thrones");

            when(userSeriesRepository.findAllByUserIdAndStatus(1L, SeriesStatus.WATCHING))
                    .thenReturn(List.of(failing, ok));
            when(tmdbClient.getSeriesDetails(1396)).thenThrow(new RuntimeException("TMDB unavailable"));
            when(tmdbClient.getSeriesDetails(1399))
                    .thenReturn(buildTmdbSeries(LocalDate.now().plusDays(1), 8, 6, "The Iron Throne"));

            List<UpcomingEpisode> result = calendarService.getUpcomingEpisodes(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSeriesTitle()).isEqualTo("Game of Thrones");
        }

        @Test
        @DisplayName("should return an empty list when there are no watching series")
        void shouldReturnEmptyList_whenNoWatchingSeries() {
            when(userSeriesRepository.findAllByUserIdAndStatus(1L, SeriesStatus.WATCHING))
                    .thenReturn(List.of());

            List<UpcomingEpisode> result = calendarService.getUpcomingEpisodes(1L);

            assertThat(result).isEmpty();
        }
    }
}
