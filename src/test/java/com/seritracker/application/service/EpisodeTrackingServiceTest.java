package com.seritracker.application.service;

import com.seritracker.domain.exception.SeriesNotFoundException;
import com.seritracker.domain.model.Episode;
import com.seritracker.domain.model.EpisodeInfo;
import com.seritracker.domain.model.EpisodeWatch;
import com.seritracker.domain.model.SeasonSummary;
import com.seritracker.domain.model.Series;
import com.seritracker.domain.model.SeriesEpisodesSummary;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.port.out.EpisodeWatchRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EpisodeTrackingService")
class EpisodeTrackingServiceTest {

    @Mock private UserSeriesRepository userSeriesRepository;
    @Mock private EpisodeWatchRepository episodeWatchRepository;
    @Mock private TmdbClient tmdbClient;

    @InjectMocks private EpisodeTrackingService episodeTrackingService;

    private UserSeries buildUserSeries(Integer watchedEpisodes) {
        return UserSeries.builder()
                .id(1L)
                .userId(1L)
                .tmdbId(1396)
                .title("Breaking Bad")
                .status(SeriesStatus.WATCHING)
                .watchedEpisodes(watchedEpisodes)
                .totalEpisodes(62)
                .build();
    }

    private Series buildTmdbSeries(List<SeasonSummary> seasons) {
        return Series.builder()
                .tmdbId(1396)
                .title("Breaking Bad")
                .totalEpisodes(62)
                .seasons(seasons)
                .build();
    }

    private EpisodeWatch buildWatch(Integer season, Integer episode) {
        return EpisodeWatch.builder()
                .userSeriesId(1L)
                .seasonNumber(season)
                .episodeNumber(episode)
                .watchedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getSeasonsSummary")
    class GetSeasonsSummary {

        @Test
        @DisplayName("should return seasons with watched counts and the first unwatched episode as next")
        void shouldReturnSeasonsAndNextEpisode() {
            UserSeries series = buildUserSeries(2);
            List<SeasonSummary> seasons = List.of(
                    SeasonSummary.builder().seasonNumber(1).name("Season 1").episodeCount(2).build(),
                    SeasonSummary.builder().seasonNumber(2).name("Season 2").episodeCount(3).build()
            );

            when(userSeriesRepository.findById(1L)).thenReturn(Optional.of(series));
            when(tmdbClient.getSeriesDetails(1396)).thenReturn(buildTmdbSeries(seasons));
            when(episodeWatchRepository.countByUserSeriesId(1L)).thenReturn(2L);
            when(episodeWatchRepository.findByUserSeriesId(1L)).thenReturn(List.of(
                    buildWatch(1, 1), buildWatch(1, 2)
            ));
            when(tmdbClient.getSeasonEpisodes(1396, 2)).thenReturn(List.of(
                    Episode.builder().seasonNumber(2).episodeNumber(1).title("Cat's in the Bag").airDate(null).build()
            ));

            SeriesEpisodesSummary result = episodeTrackingService.getSeasonsSummary(1L, 1L);

            assertThat(result.getSeasons()).hasSize(2);
            assertThat(result.getSeasons().get(0).getWatchedCount()).isEqualTo(2);
            assertThat(result.getSeasons().get(1).getWatchedCount()).isEqualTo(0);
            assertThat(result.getNextEpisode().getSeasonNumber()).isEqualTo(2);
            assertThat(result.getNextEpisode().getEpisodeNumber()).isEqualTo(1);
            assertThat(result.getNextEpisode().getTitle()).isEqualTo("Cat's in the Bag");
        }

        @Test
        @DisplayName("should return null next episode when everything is watched")
        void shouldReturnNullNextEpisode_whenEverythingIsWatched() {
            UserSeries series = buildUserSeries(2);
            List<SeasonSummary> seasons = List.of(
                    SeasonSummary.builder().seasonNumber(1).name("Season 1").episodeCount(2).build()
            );

            when(userSeriesRepository.findById(1L)).thenReturn(Optional.of(series));
            when(tmdbClient.getSeriesDetails(1396)).thenReturn(buildTmdbSeries(seasons));
            when(episodeWatchRepository.countByUserSeriesId(1L)).thenReturn(2L);
            when(episodeWatchRepository.findByUserSeriesId(1L)).thenReturn(List.of(
                    buildWatch(1, 1), buildWatch(1, 2)
            ));

            SeriesEpisodesSummary result = episodeTrackingService.getSeasonsSummary(1L, 1L);

            assertThat(result.getNextEpisode()).isNull();
            verify(tmdbClient, never()).getSeasonEpisodes(any(), any());
        }

        @Test
        @DisplayName("should backfill the legacy watched-episodes counter as the first N episodes in order")
        void shouldBackfillLegacyCounter() {
            UserSeries series = buildUserSeries(3);
            List<SeasonSummary> seasons = List.of(
                    SeasonSummary.builder().seasonNumber(1).name("Season 1").episodeCount(2).build(),
                    SeasonSummary.builder().seasonNumber(2).name("Season 2").episodeCount(3).build()
            );

            when(userSeriesRepository.findById(1L)).thenReturn(Optional.of(series));
            when(tmdbClient.getSeriesDetails(1396)).thenReturn(buildTmdbSeries(seasons));
            when(episodeWatchRepository.countByUserSeriesId(1L)).thenReturn(0L);
            when(episodeWatchRepository.findByUserSeriesId(1L)).thenReturn(List.of());

            episodeTrackingService.getSeasonsSummary(1L, 1L);

            ArgumentCaptor<List<EpisodeWatch>> captor = ArgumentCaptor.forClass(List.class);
            verify(episodeWatchRepository).saveAll(captor.capture());

            List<EpisodeWatch> inserted = captor.getValue();
            assertThat(inserted).hasSize(3);
            assertThat(inserted.get(0).getSeasonNumber()).isEqualTo(1);
            assertThat(inserted.get(0).getEpisodeNumber()).isEqualTo(1);
            assertThat(inserted.get(1).getSeasonNumber()).isEqualTo(1);
            assertThat(inserted.get(1).getEpisodeNumber()).isEqualTo(2);
            assertThat(inserted.get(2).getSeasonNumber()).isEqualTo(2);
            assertThat(inserted.get(2).getEpisodeNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not backfill when episode_watch already has rows")
        void shouldNotBackfill_whenEpisodeWatchAlreadyHasRows() {
            UserSeries series = buildUserSeries(3);
            List<SeasonSummary> seasons = List.of(
                    SeasonSummary.builder().seasonNumber(1).name("Season 1").episodeCount(5).build()
            );

            when(userSeriesRepository.findById(1L)).thenReturn(Optional.of(series));
            when(tmdbClient.getSeriesDetails(1396)).thenReturn(buildTmdbSeries(seasons));
            when(episodeWatchRepository.countByUserSeriesId(1L)).thenReturn(1L);
            when(episodeWatchRepository.findByUserSeriesId(1L)).thenReturn(List.of(buildWatch(1, 1)));
            when(tmdbClient.getSeasonEpisodes(1396, 1)).thenReturn(List.of(
                    Episode.builder().seasonNumber(1).episodeNumber(2).title("Ep 2").airDate(LocalDate.now()).build()
            ));

            episodeTrackingService.getSeasonsSummary(1L, 1L);

            verify(episodeWatchRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("should throw SeriesNotFoundException when series belongs to another user")
        void shouldThrowSeriesNotFoundException_whenSeriesBelongsToAnotherUser() {
            when(userSeriesRepository.findById(1L)).thenReturn(Optional.of(buildUserSeries(0)));

            assertThatThrownBy(() -> episodeTrackingService.getSeasonsSummary(2L, 1L))
                    .isInstanceOf(SeriesNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getSeasonEpisodes")
    class GetSeasonEpisodes {

        @Test
        @DisplayName("should mark episodes as watched based on the stored set")
        void shouldMarkEpisodesAsWatched() {
            when(userSeriesRepository.findById(1L)).thenReturn(Optional.of(buildUserSeries(1)));
            when(tmdbClient.getSeasonEpisodes(1396, 1)).thenReturn(List.of(
                    Episode.builder().seasonNumber(1).episodeNumber(1).title("Pilot").airDate(null).build(),
                    Episode.builder().seasonNumber(1).episodeNumber(2).title("Ep 2").airDate(null).build()
            ));
            when(episodeWatchRepository.findByUserSeriesId(1L)).thenReturn(List.of(buildWatch(1, 1)));

            List<EpisodeInfo> result = episodeTrackingService.getSeasonEpisodes(1L, 1L, 1);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).isWatched()).isTrue();
            assertThat(result.get(1).isWatched()).isFalse();
        }
    }

    @Nested
    @DisplayName("markEpisode")
    class MarkEpisode {

        @Test
        @DisplayName("should insert a watch row and refresh the watched count when marking watched")
        void shouldInsertAndRefreshCount_whenMarkingWatched() {
            UserSeries series = buildUserSeries(0);
            when(userSeriesRepository.findById(1L)).thenReturn(Optional.of(series));
            when(episodeWatchRepository.findByUserSeriesId(1L)).thenReturn(List.of());
            when(episodeWatchRepository.countByUserSeriesId(1L)).thenReturn(1L);
            when(userSeriesRepository.save(any())).thenReturn(series.withWatchedEpisodes(1));

            UserSeries result = episodeTrackingService.markEpisode(1L, 1L, 1, 1, true);

            verify(episodeWatchRepository).save(any());
            assertThat(result.getWatchedEpisodes()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not insert a duplicate watch row when already watched")
        void shouldNotInsertDuplicate_whenAlreadyWatched() {
            UserSeries series = buildUserSeries(1);
            when(userSeriesRepository.findById(1L)).thenReturn(Optional.of(series));
            when(episodeWatchRepository.findByUserSeriesId(1L)).thenReturn(List.of(buildWatch(1, 1)));
            when(episodeWatchRepository.countByUserSeriesId(1L)).thenReturn(1L);
            when(userSeriesRepository.save(any())).thenReturn(series);

            episodeTrackingService.markEpisode(1L, 1L, 1, 1, true);

            verify(episodeWatchRepository, never()).save(any());
        }

        @Test
        @DisplayName("should delete the watch row and refresh the watched count when marking unwatched")
        void shouldDeleteAndRefreshCount_whenMarkingUnwatched() {
            UserSeries series = buildUserSeries(1);
            when(userSeriesRepository.findById(1L)).thenReturn(Optional.of(series));
            when(episodeWatchRepository.countByUserSeriesId(1L)).thenReturn(0L);
            when(userSeriesRepository.save(any())).thenReturn(series.withWatchedEpisodes(0));

            UserSeries result = episodeTrackingService.markEpisode(1L, 1L, 1, 1, false);

            verify(episodeWatchRepository).deleteByUserSeriesIdAndSeasonNumberAndEpisodeNumber(1L, 1, 1);
            assertThat(result.getWatchedEpisodes()).isEqualTo(0);
        }

        @Test
        @DisplayName("should throw SeriesNotFoundException when series does not exist")
        void shouldThrowSeriesNotFoundException_whenSeriesDoesNotExist() {
            when(userSeriesRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> episodeTrackingService.markEpisode(1L, 99L, 1, 1, true))
                    .isInstanceOf(SeriesNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("markEpisodesWatched")
    class MarkEpisodesWatched {

        @Test
        @DisplayName("should only insert episodes that are not already watched")
        void shouldOnlyInsertEpisodesNotAlreadyWatched() {
            UserSeries series = buildUserSeries(1);
            when(userSeriesRepository.findById(1L)).thenReturn(Optional.of(series));
            when(episodeWatchRepository.findByUserSeriesId(1L)).thenReturn(List.of(buildWatch(1, 1)));
            when(episodeWatchRepository.countByUserSeriesId(1L)).thenReturn(3L);
            when(userSeriesRepository.save(any())).thenReturn(series.withWatchedEpisodes(3));

            UserSeries result = episodeTrackingService.markEpisodesWatched(1L, 1L, 1, List.of(1, 2, 3));

            ArgumentCaptor<List<EpisodeWatch>> captor = ArgumentCaptor.forClass(List.class);
            verify(episodeWatchRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).extracting(EpisodeWatch::getEpisodeNumber).containsExactly(2, 3);
            assertThat(result.getWatchedEpisodes()).isEqualTo(3);
        }
    }
}
