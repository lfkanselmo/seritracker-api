package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class Series {
    Integer tmdbId;
    String title;
    String posterUrl;
    List<String> genres;
    String network;
    Integer totalEpisodes;
    LocalDate nextAirDate;
    List<SeasonSummary> seasons;
}