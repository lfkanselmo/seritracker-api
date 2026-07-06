package com.seritracker.infrastructure.adapter.out.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TmdbSeriesResponse {

    private Integer id;
    private String name;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("number_of_episodes")
    private Integer numberOfEpisodes;

    @JsonProperty("networks")
    private List<TmdbNetwork> networks;

    @JsonProperty("genres")
    private List<TmdbGenre> genres;

    @JsonProperty("seasons")
    private List<TmdbSeasonSummary> seasons;

    @JsonProperty("next_episode_to_air")
    private TmdbNextEpisodeToAir nextEpisodeToAir;

    @Data
    public static class TmdbNetwork {
        private String name;
    }

    @Data
    public static class TmdbGenre {
        private String name;
    }

    @Data
    public static class TmdbSeasonSummary {
        @JsonProperty("season_number")
        private Integer seasonNumber;

        private String name;

        @JsonProperty("episode_count")
        private Integer episodeCount;
    }

    @Data
    public static class TmdbNextEpisodeToAir {
        private String name;

        @JsonProperty("air_date")
        private String airDate;

        @JsonProperty("season_number")
        private Integer seasonNumber;

        @JsonProperty("episode_number")
        private Integer episodeNumber;
    }
}