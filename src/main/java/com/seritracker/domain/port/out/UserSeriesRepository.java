package com.seritracker.domain.port.out;

import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;

import java.util.List;
import java.util.Optional;

public interface UserSeriesRepository {
    UserSeries save(UserSeries userSeries);
    Optional<UserSeries> findById(Long id);
    List<UserSeries> findAllByUserId(Long userId);
    List<UserSeries> findByUserIdAndStatus(Long userId, SeriesStatus status);
    boolean existsByUserIdAndTmdbId(Long userId, Integer tmdbId);
    void deleteById(Long id);
}