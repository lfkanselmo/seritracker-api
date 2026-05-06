package com.seritracker.infrastructure.adapter.in.rest;

import com.seritracker.domain.model.Series;
import com.seritracker.domain.port.out.TmdbClient;
import com.seritracker.infrastructure.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TmdbController")
class TmdbControllerTest {

    @Mock private TmdbClient tmdbClient;

    @InjectMocks private TmdbController tmdbController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(tmdbController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Series buildSeries() {
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
    @DisplayName("GET /api/v1/tmdb/search")
    class Search {

        @Test
        @DisplayName("should return 200 with results")
        void shouldReturn200_withResults() throws Exception {
            when(tmdbClient.searchSeries("Breaking Bad"))
                    .thenReturn(List.of(buildSeries()));

            mockMvc.perform(get("/api/v1/tmdb/search").param("q", "Breaking Bad"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].title").value("Breaking Bad"))
                    .andExpect(jsonPath("$.data[0].tmdbId").value(1396));
        }

        @Test
        @DisplayName("should return empty list when no results")
        void shouldReturnEmptyList_whenNoResults() throws Exception {
            when(tmdbClient.searchSeries("unknown")).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/tmdb/search").param("q", "unknown"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tmdb/series/{tmdbId}")
    class GetDetails {

        @Test
        @DisplayName("should return 200 with series details")
        void shouldReturn200_withSeriesDetails() throws Exception {
            when(tmdbClient.getSeriesDetails(1396)).thenReturn(buildSeries());

            mockMvc.perform(get("/api/v1/tmdb/series/1396"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.tmdbId").value(1396))
                    .andExpect(jsonPath("$.data.network").value("AMC"));
        }
    }
}