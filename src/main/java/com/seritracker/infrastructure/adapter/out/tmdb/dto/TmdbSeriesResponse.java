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

    @Data
    public static class TmdbNetwork {
        private String name;
    }

    @Data
    public static class TmdbGenre {
        private String name;
    }
}