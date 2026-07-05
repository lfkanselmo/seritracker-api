package com.seritracker.infrastructure.adapter.out.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TmdbSeasonResponse {

    @JsonProperty("season_number")
    private Integer seasonNumber;

    private String name;

    @JsonProperty("episodes")
    private List<TmdbEpisode> episodes;

    @Data
    public static class TmdbEpisode {
        @JsonProperty("episode_number")
        private Integer episodeNumber;

        private String name;

        @JsonProperty("air_date")
        private String airDate;
    }
}
