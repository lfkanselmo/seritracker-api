package com.seritracker.application.service;

import com.seritracker.domain.exception.DuplicateSeriesException;
import com.seritracker.domain.exception.SeriesNotFoundException;
import com.seritracker.domain.model.Series;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.port.out.TmdbClient;
import com.seritracker.domain.port.out.UserSeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeriesService")
class SeriesServiceTest {

    @Mock private UserSeriesRepository userSeriesRepository;
    @Mock private TmdbClient tmdbClient;

    @InjectMocks private SeriesService seriesService;

    // ── Factories ──────────────────────────────────────────────────────

    private UserSeries buildUserSeries(Long id, SeriesStatus status) {
        return UserSeries.builder()
                .id(id)
                .userId(1L)
                .tmdbId(1396)
                .title("Breaking Bad")
                .posterUrl("https://image.tmdb.org/t/p/w300/poster.jpg")
                .status(status)
                .rating(null)
                .watchedEpisodes(0)
                .totalEpisodes(62)
                .network("AMC")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Series buildTmdbSeries() {
        return Series.builder()
                .tmdbId(1396)
                .title("Breaking Bad")
                .posterUrl("https://image.tmdb.org/t/p/w300/poster.jpg")
                .totalEpisodes(62)
                .network("AMC")
                .genres(List.of("Drama", "Crimen"))
                .build();
    }

    // ── createSeries ───────────────────────────────────────────────────

    @Nested
    @DisplayName("createSeries")
    class CreateSeries {

        @Test
        @DisplayName("should create series when tmdbId is not duplicated")
        void shouldCreateSeries_whenTmdbIdIsNotDuplicated() {
            // Arrange
            Long userId = 1L;
            Integer tmdbId = 1396;
            UserSeries expected = buildUserSeries(1L, SeriesStatus.WATCHING);

            when(userSeriesRepository.existsByUserIdAndTmdbId(userId, tmdbId)).thenReturn(false);
            when(tmdbClient.getSeriesDetails(tmdbId)).thenReturn(buildTmdbSeries());
            when(userSeriesRepository.save(any())).thenReturn(expected);

            // Act
            UserSeries result = seriesService.createSeries(userId, tmdbId, "WATCHING");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTmdbId()).isEqualTo(tmdbId);
            assertThat(result.getStatus()).isEqualTo(SeriesStatus.WATCHING);
            verify(userSeriesRepository).save(any());
        }

        @Test
        @DisplayName("should throw DuplicateSeriesException when series already exists")
        void shouldThrowDuplicateSeriesException_whenSeriesAlreadyExists() {
            // Arrange
            Long userId = 1L;
            Integer tmdbId = 1396;
            when(userSeriesRepository.existsByUserIdAndTmdbId(userId, tmdbId)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> seriesService.createSeries(userId, tmdbId, "WATCHING"))
                    .isInstanceOf(DuplicateSeriesException.class);

            verify(userSeriesRepository, never()).save(any());
            verify(tmdbClient, never()).getSeriesDetails(any());
        }
    }

    // ── updateStatus ───────────────────────────────────────────────────

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("should update status when series exists")
        void shouldUpdateStatus_whenSeriesExists() {
            // Arrange
            Long userId = 1L;
            Long id = 1L;
            UserSeries existing = buildUserSeries(id, SeriesStatus.WATCHING);
            UserSeries updated  = buildUserSeries(id, SeriesStatus.COMPLETED);

            when(userSeriesRepository.findById(id)).thenReturn(Optional.of(existing));
            when(userSeriesRepository.save(any())).thenReturn(updated);

            // Act
            UserSeries result = seriesService.updateStatus(userId, id, SeriesStatus.COMPLETED);

            // Assert
            assertThat(result.getStatus()).isEqualTo(SeriesStatus.COMPLETED);
            verify(userSeriesRepository).save(any());
        }

        @Test
        @DisplayName("should throw SeriesNotFoundException when series does not exist")
        void shouldThrowSeriesNotFoundException_whenSeriesDoesNotExist() {
            // Arrange
            Long userId = 1L;
            Long id = 99L;
            when(userSeriesRepository.findById(id)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> seriesService.updateStatus(userId, id, SeriesStatus.COMPLETED))
                    .isInstanceOf(SeriesNotFoundException.class);

            verify(userSeriesRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw SeriesNotFoundException when series belongs to another user")
        void shouldThrowSeriesNotFoundException_whenSeriesBelongsToAnotherUser() {
            // Arrange
            Long id = 1L;
            UserSeries existing = buildUserSeries(id, SeriesStatus.WATCHING); // userId = 1L
            when(userSeriesRepository.findById(id)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> seriesService.updateStatus(2L, id, SeriesStatus.COMPLETED))
                    .isInstanceOf(SeriesNotFoundException.class);

            verify(userSeriesRepository, never()).save(any());
        }
    }

    // ── updateRating ───────────────────────────────────────────────────

    @Nested
    @DisplayName("updateRating")
    class UpdateRating {

        @Test
        @DisplayName("should update rating when series exists")
        void shouldUpdateRating_whenSeriesExists() {
            // Arrange
            Long userId = 1L;
            Long id = 1L;
            UserSeries existing = buildUserSeries(id, SeriesStatus.WATCHING);
            UserSeries updated  = existing.withRating(9);

            when(userSeriesRepository.findById(id)).thenReturn(Optional.of(existing));
            when(userSeriesRepository.save(any())).thenReturn(updated);

            // Act
            UserSeries result = seriesService.updateRating(userId, id, 9);

            // Assert
            assertThat(result.getRating()).isEqualTo(9);
            verify(userSeriesRepository).save(any());
        }

        @Test
        @DisplayName("should throw SeriesNotFoundException when series does not exist")
        void shouldThrowSeriesNotFoundException_whenSeriesDoesNotExist() {
            // Arrange
            Long userId = 1L;
            Long id = 99L;
            when(userSeriesRepository.findById(id)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> seriesService.updateRating(userId, id, 9))
                    .isInstanceOf(SeriesNotFoundException.class);
        }
    }

    // ── updateWatchedEpisodes ──────────────────────────────────────────

    @Nested
    @DisplayName("updateWatchedEpisodes")
    class UpdateWatchedEpisodes {

        @Test
        @DisplayName("should update watched episodes when series exists")
        void shouldUpdateWatchedEpisodes_whenSeriesExists() {
            // Arrange
            Long userId = 1L;
            Long id = 1L;
            UserSeries existing = buildUserSeries(id, SeriesStatus.WATCHING);
            UserSeries updated  = existing.withWatchedEpisodes(10);

            when(userSeriesRepository.findById(id)).thenReturn(Optional.of(existing));
            when(userSeriesRepository.save(any())).thenReturn(updated);

            // Act
            UserSeries result = seriesService.updateWatchedEpisodes(userId, id, 10);

            // Assert
            assertThat(result.getWatchedEpisodes()).isEqualTo(10);
            verify(userSeriesRepository).save(any());
        }
    }

    // ── deleteSeries ───────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteSeries")
    class DeleteSeries {

        @Test
        @DisplayName("should delete series when it exists")
        void shouldDeleteSeries_whenItExists() {
            // Arrange
            Long userId = 1L;
            Long id = 1L;
            UserSeries existing = buildUserSeries(id, SeriesStatus.WATCHING);
            when(userSeriesRepository.findById(id)).thenReturn(Optional.of(existing));

            // Act
            seriesService.deleteSeries(userId, id);

            // Assert
            verify(userSeriesRepository).deleteById(id);
        }

        @Test
        @DisplayName("should throw SeriesNotFoundException when series does not exist")
        void shouldThrowSeriesNotFoundException_whenSeriesDoesNotExist() {
            // Arrange
            Long userId = 1L;
            Long id = 99L;
            when(userSeriesRepository.findById(id)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> seriesService.deleteSeries(userId, id))
                    .isInstanceOf(SeriesNotFoundException.class);

            verify(userSeriesRepository, never()).deleteById(any());
        }
    }

    // ── listAllByUser ──────────────────────────────────────────────────

    @Nested
    @DisplayName("listAllByUser")
    class ListAllByUser {

        @Test
        @DisplayName("should return all series for user")
        void shouldReturnAllSeries_whenUserHasSeries() {
            // Arrange
            Long userId = 1L;
            List<UserSeries> expected = List.of(
                    buildUserSeries(1L, SeriesStatus.WATCHING),
                    buildUserSeries(2L, SeriesStatus.COMPLETED)
            );
            when(userSeriesRepository.findAllByUserId(userId)).thenReturn(expected);

            // Act
            List<UserSeries> result = seriesService.listAllByUser(userId);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(UserSeries::getStatus)
                    .containsExactly(SeriesStatus.WATCHING, SeriesStatus.COMPLETED);
        }

        @Test
        @DisplayName("should return empty list when user has no series")
        void shouldReturnEmptyList_whenUserHasNoSeries() {
            // Arrange
            Long userId = 1L;
            when(userSeriesRepository.findAllByUserId(userId)).thenReturn(List.of());

            // Act
            List<UserSeries> result = seriesService.listAllByUser(userId);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    // ── getById ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should return series when it exists")
        void shouldReturnSeries_whenItExists() {
            // Arrange
            Long userId = 1L;
            Long id = 1L;
            UserSeries expected = buildUserSeries(id, SeriesStatus.WATCHING);
            when(userSeriesRepository.findById(id)).thenReturn(Optional.of(expected));

            // Act
            UserSeries result = seriesService.getById(userId, id);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("should throw SeriesNotFoundException when series does not exist")
        void shouldThrowSeriesNotFoundException_whenSeriesDoesNotExist() {
            // Arrange
            Long userId = 1L;
            Long id = 99L;
            when(userSeriesRepository.findById(id)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> seriesService.getById(userId, id))
                    .isInstanceOf(SeriesNotFoundException.class);
        }

        @Test
        @DisplayName("should throw SeriesNotFoundException when series belongs to another user")
        void shouldThrowSeriesNotFoundException_whenSeriesBelongsToAnotherUser() {
            // Arrange
            Long id = 1L;
            UserSeries expected = buildUserSeries(id, SeriesStatus.WATCHING); // userId = 1L
            when(userSeriesRepository.findById(id)).thenReturn(Optional.of(expected));

            // Act & Assert
            assertThatThrownBy(() -> seriesService.getById(2L, id))
                    .isInstanceOf(SeriesNotFoundException.class);
        }
    }
}