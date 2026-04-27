package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.port.out.UserSeriesRepository;
import com.seritracker.infrastructure.adapter.out.persistence.mapper.UserSeriesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserSeriesRepositoryAdapter implements UserSeriesRepository {

    private final JpaUserSeriesRepository jpaRepository;
    private final UserSeriesMapper mapper;

    @Override
    public UserSeries save(UserSeries userSeries) {
        return mapper.toDomain(
                jpaRepository.save(mapper.toEntity(userSeries))
        );
    }

    @Override
    public Optional<UserSeries> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<UserSeries> findAllByUserId(Long userId) {
        return jpaRepository.findAllByUserId(userId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<UserSeries> findByUserIdAndStatus(Long userId, SeriesStatus status) {
        return jpaRepository.findByUserIdAndStatus(userId, status.name())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByUserIdAndTmdbId(Long userId, Integer tmdbId) {
        return jpaRepository.existsByUserIdAndTmdbId(userId, tmdbId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}