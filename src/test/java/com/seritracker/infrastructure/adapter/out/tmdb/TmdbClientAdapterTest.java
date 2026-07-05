package com.seritracker.infrastructure.adapter.out.tmdb;

import com.seritracker.domain.exception.SeriesNotFoundException;
import com.seritracker.domain.model.Episode;
import com.seritracker.domain.model.Series;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TmdbClientAdapter")
class TmdbClientAdapterTest {

    private MockWebServer mockWebServer;
    private TmdbClientAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        adapter = new TmdbClientAdapter();
        ReflectionTestUtils.setField(adapter, "baseUrl",      mockWebServer.url("/").toString().replaceAll("/$", ""));
        ReflectionTestUtils.setField(adapter, "token",        "test-token");
        ReflectionTestUtils.setField(adapter, "imageBaseUrl", "https://image.tmdb.org/t/p/w300");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("searchSeries should return mapped results")
    void searchSeries_shouldReturnMappedResults() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                    {
                      "results": [
                        {
                          "id": 1396,
                          "name": "Breaking Bad",
                          "poster_path": "/poster.jpg",
                          "first_air_date": "2008-01-20"
                        }
                      ]
                    }
                    """)
                .addHeader("Content-Type", "application/json"));

        List<Series> results = adapter.searchSeries("Breaking Bad");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Breaking Bad");
        assertThat(results.get(0).getTmdbId()).isEqualTo(1396);
        assertThat(results.get(0).getPosterUrl()).contains("/poster.jpg");
    }

    @Test
    @DisplayName("searchSeries should return empty list when no results")
    void searchSeries_shouldReturnEmptyList_whenNoResults() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"results\": []}")
                .addHeader("Content-Type", "application/json"));

        List<Series> results = adapter.searchSeries("unknown");

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("searchSeries should handle null poster path")
    void searchSeries_shouldHandleNullPosterPath() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                    {
                      "results": [
                        {
                          "id": 1396,
                          "name": "Breaking Bad",
                          "poster_path": null
                        }
                      ]
                    }
                    """)
                .addHeader("Content-Type", "application/json"));

        List<Series> results = adapter.searchSeries("Breaking Bad");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPosterUrl()).isNull();
    }

    @Test
    @DisplayName("getSeriesDetails should return mapped series")
    void getSeriesDetails_shouldReturnMappedSeries() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                    {
                      "id": 1396,
                      "name": "Breaking Bad",
                      "poster_path": "/poster.jpg",
                      "number_of_episodes": 62,
                      "networks": [{"name": "AMC"}],
                      "genres": [{"name": "Drama"}, {"name": "Crimen"}]
                    }
                    """)
                .addHeader("Content-Type", "application/json"));

        Series result = adapter.getSeriesDetails(1396);

        assertThat(result.getTmdbId()).isEqualTo(1396);
        assertThat(result.getTitle()).isEqualTo("Breaking Bad");
        assertThat(result.getTotalEpisodes()).isEqualTo(62);
        assertThat(result.getNetwork()).isEqualTo("AMC");
        assertThat(result.getGenres()).containsExactly("Drama", "Crimen");
    }

    @Test
    @DisplayName("getSeriesDetails should handle missing network")
    void getSeriesDetails_shouldHandleMissingNetwork() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                    {
                      "id": 1396,
                      "name": "Breaking Bad",
                      "poster_path": null,
                      "number_of_episodes": 62,
                      "networks": [],
                      "genres": []
                    }
                    """)
                .addHeader("Content-Type", "application/json"));

        Series result = adapter.getSeriesDetails(1396);

        assertThat(result.getNetwork()).isNull();
        assertThat(result.getGenres()).isEmpty();
    }

    @Test
    @DisplayName("getSeriesDetails should throw SeriesNotFoundException when TMDB returns 404")
    void getSeriesDetails_shouldThrowSeriesNotFoundException_whenTmdbReturns404() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        assertThatThrownBy(() -> adapter.getSeriesDetails(999999))
                .isInstanceOf(SeriesNotFoundException.class);
    }

    @Test
    @DisplayName("getSeriesDetails should throw SeriesNotFoundException when TMDB returns an empty body")
    void getSeriesDetails_shouldThrowSeriesNotFoundException_whenBodyIsEmpty() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{}")
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> adapter.getSeriesDetails(1396))
                .isInstanceOf(SeriesNotFoundException.class);
    }

    @Test
    @DisplayName("getSeriesDetails should map seasons and filter out specials (season 0)")
    void getSeriesDetails_shouldMapSeasons_andFilterOutSpecials() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                    {
                      "id": 1396,
                      "name": "Breaking Bad",
                      "number_of_episodes": 62,
                      "number_of_seasons": 5,
                      "networks": [],
                      "genres": [],
                      "seasons": [
                        {"season_number": 0, "name": "Specials", "episode_count": 3},
                        {"season_number": 1, "name": "Season 1", "episode_count": 7}
                      ]
                    }
                    """)
                .addHeader("Content-Type", "application/json"));

        Series result = adapter.getSeriesDetails(1396);

        assertThat(result.getSeasons()).hasSize(1);
        assertThat(result.getSeasons().get(0).getSeasonNumber()).isEqualTo(1);
        assertThat(result.getSeasons().get(0).getEpisodeCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("getSeasonEpisodes should return mapped episodes")
    void getSeasonEpisodes_shouldReturnMappedEpisodes() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                    {
                      "season_number": 1,
                      "name": "Season 1",
                      "episodes": [
                        {"episode_number": 1, "name": "Pilot", "air_date": "2008-01-20"},
                        {"episode_number": 2, "name": "Cat's in the Bag...", "air_date": null}
                      ]
                    }
                    """)
                .addHeader("Content-Type", "application/json"));

        List<Episode> episodes = adapter.getSeasonEpisodes(1396, 1);

        assertThat(episodes).hasSize(2);
        assertThat(episodes.get(0).getSeasonNumber()).isEqualTo(1);
        assertThat(episodes.get(0).getTitle()).isEqualTo("Pilot");
        assertThat(episodes.get(0).getAirDate()).isEqualTo(java.time.LocalDate.of(2008, 1, 20));
        assertThat(episodes.get(1).getAirDate()).isNull();
    }

    @Test
    @DisplayName("getSeasonEpisodes should throw SeriesNotFoundException when TMDB returns 404")
    void getSeasonEpisodes_shouldThrowSeriesNotFoundException_whenTmdbReturns404() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        assertThatThrownBy(() -> adapter.getSeasonEpisodes(999999, 1))
                .isInstanceOf(SeriesNotFoundException.class);
    }
}