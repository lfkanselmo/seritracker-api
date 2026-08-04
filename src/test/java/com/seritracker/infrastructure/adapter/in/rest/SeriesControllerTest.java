package com.seritracker.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seritracker.domain.exception.DuplicateSeriesException;
import com.seritracker.domain.exception.SeriesNotFoundException;
import com.seritracker.domain.model.EpisodeInfo;
import com.seritracker.domain.model.NextEpisode;
import com.seritracker.domain.model.PageRequest;
import com.seritracker.domain.model.PageResult;
import com.seritracker.domain.model.SeasonProgress;
import com.seritracker.domain.model.SeriesEpisodesSummary;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.SortDirection;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.port.in.CreateSeriesUseCase;
import com.seritracker.domain.port.in.DeleteSeriesUseCase;
import com.seritracker.domain.port.in.EpisodeTrackingUseCase;
import com.seritracker.domain.port.in.SearchSeriesUseCase;
import com.seritracker.domain.port.in.UpdateSeriesUseCase;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.CreateSeriesRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.MarkEpisodeRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.MarkSeasonRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.UpdateNotesRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.UpdateRatingRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.UpdateStatusRequest;
import com.seritracker.infrastructure.config.GlobalExceptionHandler;
import com.seritracker.infrastructure.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeriesController")
class SeriesControllerTest {

    @Mock private CreateSeriesUseCase createSeriesUseCase;
    @Mock private UpdateSeriesUseCase updateSeriesUseCase;
    @Mock private DeleteSeriesUseCase deleteSeriesUseCase;
    @Mock private SearchSeriesUseCase searchSeriesUseCase;
    @Mock private EpisodeTrackingUseCase episodeTrackingUseCase;

    @InjectMocks private SeriesController seriesController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        UserPrincipal principal = new UserPrincipal(1L, "test@test.com", "hashed_password", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        mockMvc = MockMvcBuilders
                .standaloneSetup(seriesController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

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

    @Nested
    @DisplayName("GET /api/v1/series")
    class ListAll {

        @Test
        @DisplayName("should return 200 with series list")
        void shouldReturn200_withSeriesList() throws Exception {
            List<UserSeries> content = List.of(buildUserSeries(1L, SeriesStatus.WATCHING));
            when(searchSeriesUseCase.listAllByUser(1L, PageRequest.of(0, 20)))
                    .thenReturn(new PageResult<>(content, 0, 20, 1, 1));

            mockMvc.perform(get("/api/v1/series"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].title").value("Breaking Bad"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("should filter by status when provided")
        void shouldFilterByStatus_whenProvided() throws Exception {
            List<UserSeries> content = List.of(buildUserSeries(1L, SeriesStatus.WATCHING));
            when(searchSeriesUseCase.listByStatus(1L, SeriesStatus.WATCHING, PageRequest.of(0, 20)))
                    .thenReturn(new PageResult<>(content, 0, 20, 1, 1));

            mockMvc.perform(get("/api/v1/series")
                            .param("status", "WATCHING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].status").value("WATCHING"));
        }

        @Test
        @DisplayName("should return 400 when status query param is invalid")
        void shouldReturn400_whenStatusQueryParamIsInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/series")
                            .param("status", "NOT_A_STATUS"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should pass search through when provided")
        void shouldPassSearchThrough_whenProvided() throws Exception {
            List<UserSeries> content = List.of(buildUserSeries(1L, SeriesStatus.WATCHING));
            when(searchSeriesUseCase.listAllByUser(1L, PageRequest.of(0, 20, "breaking", null, null)))
                    .thenReturn(new PageResult<>(content, 0, 20, 1, 1));

            mockMvc.perform(get("/api/v1/series")
                            .param("search", "breaking"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].title").value("Breaking Bad"));
        }

        @Test
        @DisplayName("should pass sortBy and sortDir through when provided")
        void shouldPassSort_whenProvided() throws Exception {
            List<UserSeries> content = List.of(buildUserSeries(1L, SeriesStatus.WATCHING));
            when(searchSeriesUseCase.listAllByUser(1L, PageRequest.of(0, 20, null, "title", SortDirection.ASC)))
                    .thenReturn(new PageResult<>(content, 0, 20, 1, 1));

            mockMvc.perform(get("/api/v1/series")
                            .param("sortBy", "TITLE")
                            .param("sortDir", "ASC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].title").value("Breaking Bad"));
        }

        @Test
        @DisplayName("should return 400 when sortBy query param is invalid")
        void shouldReturn400_whenSortByQueryParamIsInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/series")
                            .param("sortBy", "NOT_A_FIELD"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/series/{id}")
    class GetById {

        @Test
        @DisplayName("should return 200 with series")
        void shouldReturn200_withSeries() throws Exception {
            when(searchSeriesUseCase.getById(1L, 1L))
                    .thenReturn(buildUserSeries(1L, SeriesStatus.WATCHING));

            mockMvc.perform(get("/api/v1/series/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @DisplayName("should return 404 when series not found")
        void shouldReturn404_whenSeriesNotFound() throws Exception {
            when(searchSeriesUseCase.getById(1L, 99L))
                    .thenThrow(new SeriesNotFoundException(99L));

            mockMvc.perform(get("/api/v1/series/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/series")
    class Create {

        @Test
        @DisplayName("should return 201 when series created")
        void shouldReturn201_whenSeriesCreated() throws Exception {
            CreateSeriesRequest request = new CreateSeriesRequest(1396, SeriesStatus.WATCHING);
            when(createSeriesUseCase.createSeries(1L, 1396, SeriesStatus.WATCHING))
                    .thenReturn(buildUserSeries(1L, SeriesStatus.WATCHING));

            mockMvc.perform(post("/api/v1/series")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.tmdbId").value(1396));
        }

        @Test
        @DisplayName("should return 409 when series is duplicate")
        void shouldReturn409_whenSeriesIsDuplicate() throws Exception {
            CreateSeriesRequest request = new CreateSeriesRequest(1396, SeriesStatus.WATCHING);
            when(createSeriesUseCase.createSeries(any(), any(), any()))
                    .thenThrow(new DuplicateSeriesException(1396));

            mockMvc.perform(post("/api/v1/series")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return 400 when tmdbId is null")
        void shouldReturn400_whenTmdbIdIsNull() throws Exception {
            mockMvc.perform(post("/api/v1/series")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tmdbId\": null, \"status\": \"WATCHING\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return 400 when status is not a valid enum value")
        void shouldReturn400_whenStatusIsInvalid() throws Exception {
            mockMvc.perform(post("/api/v1/series")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tmdbId\": 1396, \"status\": \"NOT_A_STATUS\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/series/{id}/status")
    class UpdateStatus {

        @Test
        @DisplayName("should return 200 when status updated")
        void shouldReturn200_whenStatusUpdated() throws Exception {
            UpdateStatusRequest request = new UpdateStatusRequest(SeriesStatus.COMPLETED);
            when(updateSeriesUseCase.updateStatus(1L, 1L, SeriesStatus.COMPLETED))
                    .thenReturn(buildUserSeries(1L, SeriesStatus.COMPLETED));

            mockMvc.perform(patch("/api/v1/series/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/series/{id}/rating")
    class UpdateRating {

        @Test
        @DisplayName("should return 200 when rating updated")
        void shouldReturn200_whenRatingUpdated() throws Exception {
            UpdateRatingRequest request = new UpdateRatingRequest(9);
            UserSeries updated = buildUserSeries(1L, SeriesStatus.WATCHING);
            when(updateSeriesUseCase.updateRating(1L, 1L, 9)).thenReturn(updated);

            mockMvc.perform(patch("/api/v1/series/1/rating")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 when rating is out of range")
        void shouldReturn400_whenRatingIsOutOfRange() throws Exception {
            mockMvc.perform(patch("/api/v1/series/1/rating")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rating\": 11}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/series/{id}/seasons")
    class GetSeasonsSummary {

        @Test
        @DisplayName("should return 200 with seasons and next episode")
        void shouldReturn200_withSeasonsAndNextEpisode() throws Exception {
            SeriesEpisodesSummary summary = SeriesEpisodesSummary.builder()
                    .seasons(List.of(SeasonProgress.builder()
                            .seasonNumber(1).name("Season 1").episodeCount(7).watchedCount(3)
                            .build()))
                    .nextEpisode(NextEpisode.builder()
                            .seasonNumber(1).episodeNumber(4).title("Cancer Man").airDate(null)
                            .build())
                    .build();
            when(episodeTrackingUseCase.getSeasonsSummary(1L, 1L)).thenReturn(summary);

            mockMvc.perform(get("/api/v1/series/1/seasons"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.seasons[0].watchedCount").value(3))
                    .andExpect(jsonPath("$.data.nextEpisode.title").value("Cancer Man"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/series/{id}/seasons/{seasonNumber}/episodes")
    class GetSeasonEpisodes {

        @Test
        @DisplayName("should return 200 with episode list")
        void shouldReturn200_withEpisodeList() throws Exception {
            List<EpisodeInfo> episodes = List.of(EpisodeInfo.builder()
                    .seasonNumber(1).episodeNumber(1).title("Pilot").airDate(null).watched(true)
                    .build());
            when(episodeTrackingUseCase.getSeasonEpisodes(1L, 1L, 1)).thenReturn(episodes);

            mockMvc.perform(get("/api/v1/series/1/seasons/1/episodes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.episodes[0].title").value("Pilot"))
                    .andExpect(jsonPath("$.data.episodes[0].watched").value(true));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/series/{id}/seasons/{seasonNumber}/episodes/{episodeNumber}")
    class MarkEpisode {

        @Test
        @DisplayName("should return 200 when episode marked as watched")
        void shouldReturn200_whenEpisodeMarkedAsWatched() throws Exception {
            MarkEpisodeRequest request = new MarkEpisodeRequest(true);
            when(episodeTrackingUseCase.markEpisode(1L, 1L, 1, 1, true))
                    .thenReturn(buildUserSeries(1L, SeriesStatus.WATCHING));

            mockMvc.perform(patch("/api/v1/series/1/seasons/1/episodes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 when watched is missing")
        void shouldReturn400_whenWatchedIsMissing() throws Exception {
            mockMvc.perform(patch("/api/v1/series/1/seasons/1/episodes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/series/{id}/seasons/{seasonNumber}/watch-all")
    class MarkSeasonWatched {

        @Test
        @DisplayName("should return 200 when season marked as watched")
        void shouldReturn200_whenSeasonMarkedAsWatched() throws Exception {
            MarkSeasonRequest request = new MarkSeasonRequest(List.of(1, 2, 3));
            when(episodeTrackingUseCase.markEpisodesWatched(1L, 1L, 1, List.of(1, 2, 3)))
                    .thenReturn(buildUserSeries(1L, SeriesStatus.WATCHING));

            mockMvc.perform(patch("/api/v1/series/1/seasons/1/watch-all")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 when episodeNumbers is empty")
        void shouldReturn400_whenEpisodeNumbersIsEmpty() throws Exception {
            mockMvc.perform(patch("/api/v1/series/1/seasons/1/watch-all")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"episodeNumbers\": []}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/series/{id}/notes")
    class UpdateNotes {

        @Test
        @DisplayName("should return 200 when notes updated")
        void shouldReturn200_whenNotesUpdated() throws Exception {
            UpdateNotesRequest request = new UpdateNotesRequest("Great show so far");
            when(updateSeriesUseCase.updateNotes(1L, 1L, "Great show so far"))
                    .thenReturn(buildUserSeries(1L, SeriesStatus.WATCHING));

            mockMvc.perform(patch("/api/v1/series/1/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 when notes exceed the max length")
        void shouldReturn400_whenNotesExceedMaxLength() throws Exception {
            String tooLong = "a".repeat(2001);

            mockMvc.perform(patch("/api/v1/series/1/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateNotesRequest(tooLong))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/series/{id}")
    class Delete {

        @Test
        @DisplayName("should return 204 when series deleted")
        void shouldReturn204_whenSeriesDeleted() throws Exception {
            doNothing().when(deleteSeriesUseCase).deleteSeries(1L, 1L);

            mockMvc.perform(delete("/api/v1/series/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when series not found")
        void shouldReturn404_whenSeriesNotFound() throws Exception {
            doThrow(new SeriesNotFoundException(99L))
                    .when(deleteSeriesUseCase).deleteSeries(1L, 99L);

            mockMvc.perform(delete("/api/v1/series/99"))
                    .andExpect(status().isNotFound());
        }
    }
}