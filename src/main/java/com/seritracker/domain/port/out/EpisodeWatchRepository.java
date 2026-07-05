package com.seritracker.domain.port.out;

import com.seritracker.domain.model.EpisodeWatch;

import java.util.List;

public interface EpisodeWatchRepository {
    List<EpisodeWatch> findByUserSeriesId(Long userSeriesId);
    EpisodeWatch save(EpisodeWatch episodeWatch);
    List<EpisodeWatch> saveAll(List<EpisodeWatch> episodeWatches);
    void deleteByUserSeriesIdAndSeasonNumberAndEpisodeNumber(Long userSeriesId, Integer seasonNumber, Integer episodeNumber);
    long countByUserSeriesId(Long userSeriesId);
}
