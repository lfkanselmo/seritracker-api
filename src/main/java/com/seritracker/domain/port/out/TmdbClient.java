package com.seritracker.domain.port.out;

import com.seritracker.domain.model.Episode;
import com.seritracker.domain.model.Series;

import java.util.List;

public interface TmdbClient {
    List<Series> searchSeries(String query);
    Series getSeriesDetails(Integer tmdbId);
    List<Episode> getSeasonEpisodes(Integer tmdbId, Integer seasonNumber);
}