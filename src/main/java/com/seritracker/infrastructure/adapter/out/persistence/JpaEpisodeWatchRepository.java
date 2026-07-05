package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.infrastructure.adapter.out.persistence.entity.EpisodeWatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface JpaEpisodeWatchRepository extends JpaRepository<EpisodeWatchEntity, Long> {

    List<EpisodeWatchEntity> findByUserSeriesId(Long userSeriesId);

    long countByUserSeriesId(Long userSeriesId);

    @Modifying
    @Transactional
    @Query("DELETE FROM EpisodeWatchEntity e WHERE e.userSeriesId = :userSeriesId " +
            "AND e.seasonNumber = :seasonNumber AND e.episodeNumber = :episodeNumber")
    void deleteByUserSeriesIdAndSeasonNumberAndEpisodeNumber(Long userSeriesId, Integer seasonNumber, Integer episodeNumber);
}
