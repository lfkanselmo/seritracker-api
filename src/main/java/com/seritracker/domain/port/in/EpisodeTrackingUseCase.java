package com.seritracker.domain.port.in;

import com.seritracker.domain.model.EpisodeInfo;
import com.seritracker.domain.model.SeriesEpisodesSummary;
import com.seritracker.domain.model.UserSeries;

import java.util.List;

public interface EpisodeTrackingUseCase {
    SeriesEpisodesSummary getSeasonsSummary(Long userId, Long seriesId);
    List<EpisodeInfo> getSeasonEpisodes(Long userId, Long seriesId, Integer seasonNumber);
    UserSeries markEpisode(Long userId, Long seriesId, Integer seasonNumber, Integer episodeNumber, boolean watched);
    UserSeries markEpisodesWatched(Long userId, Long seriesId, Integer seasonNumber, List<Integer> episodeNumbers);
}
