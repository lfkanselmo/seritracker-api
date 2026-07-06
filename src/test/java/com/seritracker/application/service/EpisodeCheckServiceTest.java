package com.seritracker.application.service;

import com.seritracker.domain.model.Notification;
import com.seritracker.domain.model.Series;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.port.out.NotificationRepository;
import com.seritracker.domain.port.out.TmdbClient;
import com.seritracker.domain.port.out.UserSeriesRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EpisodeCheckService")
class EpisodeCheckServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserSeriesRepository userSeriesRepository;
    @Mock private TmdbClient tmdbClient;

    @InjectMocks private EpisodeCheckService episodeCheckService;

    private UserSeries buildWatchingSeries() {
        return UserSeries.builder()
                .id(1L)
                .userId(1L)
                .tmdbId(1396)
                .title("Breaking Bad")
                .status(SeriesStatus.WATCHING)
                .build();
    }

    private Series buildTmdbSeries(LocalDate nextAirDate, Integer season, Integer episode) {
        return Series.builder()
                .tmdbId(1396)
                .title("Breaking Bad")
                .nextAirDate(nextAirDate)
                .nextEpisodeSeasonNumber(season)
                .nextEpisodeNumber(episode)
                .build();
    }

    @Nested
    @DisplayName("checkUpcomingEpisodes")
    class CheckUpcomingEpisodes {

        @Test
        @DisplayName("should create a notification when the next episode airs today")
        void shouldCreateNotification_whenNextEpisodeAirsToday() {
            when(userSeriesRepository.findAllUserIds()).thenReturn(List.of(1L));
            when(userSeriesRepository.findAllByUserIdAndStatus(1L, SeriesStatus.WATCHING))
                    .thenReturn(List.of(buildWatchingSeries()));
            when(tmdbClient.getSeriesDetails(1396))
                    .thenReturn(buildTmdbSeries(LocalDate.now(), 6, 1));
            when(notificationRepository.existsByUserIdAndTmdbIdAndEpisodeCode(anyLong(), anyInt(), anyString()))
                    .thenReturn(false);

            episodeCheckService.checkUpcomingEpisodes();

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getEpisodeCode()).isEqualTo("S06E01");
            assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should create a notification when the next episode airs tomorrow")
        void shouldCreateNotification_whenNextEpisodeAirsTomorrow() {
            when(userSeriesRepository.findAllUserIds()).thenReturn(List.of(1L));
            when(userSeriesRepository.findAllByUserIdAndStatus(1L, SeriesStatus.WATCHING))
                    .thenReturn(List.of(buildWatchingSeries()));
            when(tmdbClient.getSeriesDetails(1396))
                    .thenReturn(buildTmdbSeries(LocalDate.now().plusDays(1), 6, 1));
            when(notificationRepository.existsByUserIdAndTmdbIdAndEpisodeCode(anyLong(), anyInt(), anyString()))
                    .thenReturn(false);

            episodeCheckService.checkUpcomingEpisodes();

            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("should not create a notification when the next episode is outside the +-1 day window")
        void shouldNotCreateNotification_whenOutsideWindow() {
            when(userSeriesRepository.findAllUserIds()).thenReturn(List.of(1L));
            when(userSeriesRepository.findAllByUserIdAndStatus(1L, SeriesStatus.WATCHING))
                    .thenReturn(List.of(buildWatchingSeries()));
            when(tmdbClient.getSeriesDetails(1396))
                    .thenReturn(buildTmdbSeries(LocalDate.now().plusDays(5), 6, 1));

            episodeCheckService.checkUpcomingEpisodes();

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not create a notification when TMDB has no next episode data")
        void shouldNotCreateNotification_whenNoNextEpisodeData() {
            when(userSeriesRepository.findAllUserIds()).thenReturn(List.of(1L));
            when(userSeriesRepository.findAllByUserIdAndStatus(1L, SeriesStatus.WATCHING))
                    .thenReturn(List.of(buildWatchingSeries()));
            when(tmdbClient.getSeriesDetails(1396))
                    .thenReturn(buildTmdbSeries(null, null, null));

            episodeCheckService.checkUpcomingEpisodes();

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not create a duplicate notification for the same episode")
        void shouldNotCreateDuplicateNotification_forSameEpisode() {
            when(userSeriesRepository.findAllUserIds()).thenReturn(List.of(1L));
            when(userSeriesRepository.findAllByUserIdAndStatus(1L, SeriesStatus.WATCHING))
                    .thenReturn(List.of(buildWatchingSeries()));
            when(tmdbClient.getSeriesDetails(1396))
                    .thenReturn(buildTmdbSeries(LocalDate.now(), 6, 1));
            when(notificationRepository.existsByUserIdAndTmdbIdAndEpisodeCode(1L, 1396, "S06E01"))
                    .thenReturn(true);

            episodeCheckService.checkUpcomingEpisodes();

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should continue checking other users when one lookup fails")
        void shouldContinueCheckingOtherUsers_whenOneLookupFails() {
            UserSeries otherSeries = buildWatchingSeries().withStatus(SeriesStatus.WATCHING);

            when(userSeriesRepository.findAllUserIds()).thenReturn(List.of(1L, 2L));
            when(userSeriesRepository.findAllByUserIdAndStatus(eq(1L), eq(SeriesStatus.WATCHING)))
                    .thenReturn(List.of(buildWatchingSeries()));
            when(userSeriesRepository.findAllByUserIdAndStatus(eq(2L), eq(SeriesStatus.WATCHING)))
                    .thenReturn(List.of(otherSeries));
            when(tmdbClient.getSeriesDetails(1396))
                    .thenThrow(new RuntimeException("TMDB unavailable"))
                    .thenReturn(buildTmdbSeries(LocalDate.now(), 6, 1));
            when(notificationRepository.existsByUserIdAndTmdbIdAndEpisodeCode(anyLong(), anyInt(), anyString()))
                    .thenReturn(false);

            episodeCheckService.checkUpcomingEpisodes();

            verify(notificationRepository).save(any(Notification.class));
        }
    }
}
