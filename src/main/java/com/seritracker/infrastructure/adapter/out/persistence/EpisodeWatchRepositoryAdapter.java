package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.domain.model.EpisodeWatch;
import com.seritracker.domain.port.out.EpisodeWatchRepository;
import com.seritracker.infrastructure.adapter.out.persistence.entity.EpisodeWatchEntity;
import com.seritracker.infrastructure.adapter.out.persistence.mapper.EpisodeWatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EpisodeWatchRepositoryAdapter implements EpisodeWatchRepository {

    private final JpaEpisodeWatchRepository jpaRepository;
    private final EpisodeWatchMapper mapper;

    @Override
    public List<EpisodeWatch> findByUserSeriesId(Long userSeriesId) {
        return jpaRepository.findByUserSeriesId(userSeriesId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public EpisodeWatch save(EpisodeWatch episodeWatch) {
        return mapper.toDomain(
                jpaRepository.save(mapper.toEntity(episodeWatch))
        );
    }

    @Override
    public List<EpisodeWatch> saveAll(List<EpisodeWatch> episodeWatches) {
        List<EpisodeWatchEntity> entities = episodeWatches.stream()
                .map(mapper::toEntity)
                .toList();

        return jpaRepository.saveAll(entities)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteByUserSeriesIdAndSeasonNumberAndEpisodeNumber(Long userSeriesId, Integer seasonNumber, Integer episodeNumber) {
        jpaRepository.deleteByUserSeriesIdAndSeasonNumberAndEpisodeNumber(userSeriesId, seasonNumber, episodeNumber);
    }

    @Override
    public long countByUserSeriesId(Long userSeriesId) {
        return jpaRepository.countByUserSeriesId(userSeriesId);
    }
}
