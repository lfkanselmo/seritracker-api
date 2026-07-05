package com.seritracker.infrastructure.adapter.out.tmdb.dto;

import lombok.Data;
import java.util.List;

@Data
public class TmdbSearchResponse {
    private List<TmdbSearchResult> results;

    @Data
    public static class TmdbSearchResult {
        private Integer id;
        private String name;

        @com.fasterxml.jackson.annotation.JsonProperty("poster_path")
        private String posterPath;
    }
}