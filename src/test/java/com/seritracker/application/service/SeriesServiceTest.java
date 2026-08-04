package com.seritracker.application.service;

import com.seritracker.domain.exception.DuplicateSeriesException;
import com.seritracker.domain.exception.SeriesNotFoundException;
import com.seritracker.domain.model.PageRequest;
import com.seritracker.domain.model.PageResult;
import com.seritracker.domain.model.Series;
import com.seritracker.domain.model.SeriesStatus;
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

    @Nested
    @DisplayName("createSeries")
    class CreateSeries {

        @Test
        @DisplayName("should create series when tmdbId is not duplicated")
        void shouldCreateSeries_whenTmdbIdIsNotDuplicated() {
            Long userId = 1L;
            Integer tmdbId = 1396;
            UserSeries expected = buildUserSeries(1L, SeriesStatus.WATCHING);

            when(userSeriesRepository.existsByUserIdAndTmdbId(userId, tmdbId)).thenReturn(false);
            when(tmdbClient.getSeriesDetails(tmdbId)).thenReturn(buildTmdbSeries());
            when(userSeriesRepository.save(any())).thenReturn(expected);

            UserSeries result = seriesService.createSeries(userId, tmdbId, SeriesStatus.WATCHING);

            assertThat(result).isNotNull();
            assertThat(result.getTmdbId()).isEqualTo(tmdbId);
            assertThat(result.getStatus()).isEqualTo(SeriesStatus.WATCHING);
            verify(userSeriesRepository).save(any());
        }

        @Test
        @DisplayName("should throw DuplicateSeriesException when series already exists")
        void shouldThrowDuplicateSeriesException_whenSeriesAlreadyExists() {
            Long userId = 1L;
            Integer tmdbId = 1396;
            when(userSeriesRepository.existsByUserIdAndTmdbId(userId, tmdbId)).thenReturn(true);

            assertThatThrownBy(() -> seriesService.createSeries(userId, tmdbId, SeriesStatus.WATCHING))
                    .isInstanceOf(DuplicateSeriesException.class);

            verify(userSeriesRepository, never()).save(any());
            verify(tmdbClient, never()).getSeriesDetails(any());
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("should update status when series exists")
        void shouldUpdateStatus_whenSeriesExists() {
            Long userId = 1L;
            Long id = 1L;
            UserSeries existing = buildUserSeries(id, SeriesStatus.WATCHING);
            UserSeries updated  = buildUserSeries(id, SeriesStatus.COMPLETED);

            when(userSeriesRepository.findById(id)).thenReturn(Optional.of(existing));
            when(userSeriesRepository.save(any())).thenReturn(updated);

            UserSeries result = seriesService.updateStatus(userId, id, SeriesStatus.COMPLETED);

            assertThat(result.getStatus()).isEqualTo(SeriesStatus.COMPLETED);
            verify(userSeriesRepository).save(any());
        }

        @Test
        @DisplayName("should throw SeriesNotFoundException when series does not exist")
        void shouldThrowSeriesNotFoundException_whenSeriesDoesNotExist() {
            Long userId = 1L;
            Long id = 99L;
            when(userSeriesRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> seriesService.updateStatus(userId, id, SeriesStatus.COMPLETED))
                    .isInstanceOf(SeriesNotFoundException.class);

            verify(userSeriesRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw SeriesNotFoundException when series belongs to another user")
        void shouldThrowSeriesNotFoundException_whenSeriesBelongsToAnotherUser() {
            Long id = 1L;
            UserSeries existing = buildUserSeries(id, SeriesStatus.WATCHING); // userId = 1L
            when(userSeriesRepository.findById(id)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> seriesService.updateStatus(2L, id, SeriesStatus.COMPLETED))
                    .isInstanceOf(SeriesNotFoundException.class);

            verify(userSeriesRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateRating")
    class UpdateRating {

        @Test
        @DisplayName("should update rating when series exists")
        void shouldUpdateRating_whenSeriesExists() {
            Long userId = 1L;
            Long id = 1L;
            UserSeries existing = buildUserSeries(id, SeriesStatus.WATCHING);
            UserSeries updated  = existing.withRating(9);

            when(userSeriesRepository.findById(id)).thenReturn(Optional.of(existing));
            when(userSeriesRepository.save(any())).thenReturn(updated);

            UserSeries result = seriesService.updateRating(userId, id, 9);

            assertThat(result.getRating()).isEqualTo(9);
            verify(userSeriesRepository).save(any());
        }

        @Test
        @DisplayName("should throw SeriesNotFoundException when series does not exist")
        void shouldThrowSeriesNotFoundException_whenSeriesDoesNotExist() {
            Long userId = 1L;
            Long id = 99L;
            when(userSeriesRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> seriesService.updateRating(userId, id, 9))
                    .isInstanceOf(SeriesNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateNotes")
    class UpdateNotes {

        @Test
        @DisplayName("should update notes when series exists")
        void shouldUpdateNotes_whenSeriesExists() {
            Long userId = 1L;
            Long id = 1L;
            UserSeries existing = buildUserSeries(id, SeriesStatus.WATCHING);
            UserSeries updated  = existing.withNotes("Great show so far");

            when(userSeriesRepository.findById(id)).thenReturn(Optional.of(existing));
            when(userSeriesRepository.save(any())).thenReturn(updated);

            UserSeries result = seriesService.updateNotes(userId, id, "Great show so far");

            assertThat(result.getNotes()).isEqualTo("Great show so far");
            verify(userSeriesRepository).save(any());
        }

        @Test
        @DisplayName("should throw SeriesNotFoundException when series does not exist")
        void shouldThrowSeriesNotFoundException_whenSeriesDoesNotExist() {
            Long userId = 1L;
            Long id = 99L;
            when(userSeriesRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> seriesService.updateNotes(userId, id, "some notes"))
                    .isInstanceOf(SeriesNotFoundException.class);

            verify(userSeriesRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteSeries")
    class DeleteSeries {

        @Test
        @DisplayName("should delete series when it exists")
        void shouldDeleteSeries_whenItExists() {
            Long userId = 1L;
            Long id = 1L;
            UserSeries existing = buildUserSeries(id, SeriesStatus.WATCHING);
            when(userSeriesRepository.findById(id)).thenReturn(Optional.of(existing));

            seriesService.deleteSeries(userId, id);

            verify(userSeriesRepository).deleteById(id);
        }

        @Test
        @DisplayName("should throw SeriesNotFoundException when series does not exist")
        void shouldThrowSeriesNotFoundException_whenSeriesDoesNotExist() {
            Long userId = 1L;
            Long id = 99L;
            when(userSeriesRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> seriesService.deleteSeries(userId, id))
                    .isInstanceOf(SeriesNotFoundException.class);

            verify(userSeriesRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("listAllByUser")
    class ListAllByUser {

        @Test
        @DisplayName("should return all series for user")
        void shouldReturnAllSeries_whenUserHasSeries() {
            Long userId = 1L;
            PageRequest pageRequest = PageRequest.of(0, 20);
            List<UserSeries> content = List.of(
                    buildUserSeries(1L, SeriesStatus.WATCHING),
                    buildUserSeries(2L, SeriesStatus.COMPLETED)
            );
            PageResult<UserSeries> expected = new PageResult<>(content, 0, 20, 2, 1);
            when(userSeriesRepository.findAllByUserId(userId, pageRequest)).thenReturn(expected);

            PageResult<UserSeries> result = seriesService.listAllByUser(userId, pageRequest);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).extracting(UserSeries::getStatus)
                    .containsExactly(SeriesStatus.WATCHING, SeriesStatus.COMPLETED);
        }

        @Test
        @DisplayName("should return empty page when user has no series")
        void shouldReturnEmptyList_whenUserHasNoSeries() {
            Long userId = 1L;
            PageRequest pageRequest = PageRequest.of(0, 20);
            PageResult<UserSeries> expected = new PageResult<>(List.of(), 0, 20, 0, 0);
            when(userSeriesRepository.findAllByUserId(userId, pageRequest)).thenReturn(expected);

            PageResult<UserSeries> result = seriesService.listAllByUser(userId, pageRequest);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should return series when it exists")
        void shouldReturnSeries_whenItExists() {
            Long userId = 1L;
            Long id = 1L;
            UserSeries expected = buildUserSeries(id, SeriesStatus.WATCHING);
            when(userSeriesRepository.findById(id)).thenReturn(Optional.of(expected));

            UserSeries result = seriesService.getById(userId, id);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("should throw SeriesNotFoundException when series does not exist")
        void shouldThrowSeriesNotFoundException_whenSeriesDoesNotExist() {
            Long userId = 1L;
            Long id = 99L;
            when(userSeriesRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> seriesService.getById(userId, id))
                    .isInstanceOf(SeriesNotFoundException.class);
        }

        @Test
        @DisplayName("should throw SeriesNotFoundException when series belongs to another user")
        void shouldThrowSeriesNotFoundException_whenSeriesBelongsToAnotherUser() {
            Long id = 1L;
            UserSeries expected = buildUserSeries(id, SeriesStatus.WATCHING); // userId = 1L
            when(userSeriesRepository.findById(id)).thenReturn(Optional.of(expected));

            assertThatThrownBy(() -> seriesService.getById(2L, id))
                    .isInstanceOf(SeriesNotFoundException.class);
        }
    }
}