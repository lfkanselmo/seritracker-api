package com.seritracker.domain.port.out;

import com.seritracker.domain.model.PageRequest;
import com.seritracker.domain.model.PageResult;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;

import java.util.List;
import java.util.Optional;

public interface UserSeriesRepository {
    UserSeries save(UserSeries userSeries);
    Optional<UserSeries> findById(Long id);
    PageResult<UserSeries> findAllByUserId(Long userId, PageRequest pageRequest);
    List<UserSeries> findAllByUserIdAndStatus(Long userId, SeriesStatus status);
    PageResult<UserSeries> findByUserIdAndStatus(Long userId, SeriesStatus status, PageRequest pageRequest);
    boolean existsByUserIdAndTmdbId(Long userId, Integer tmdbId);
    void deleteById(Long id);
    List<Long> findAllUserIds();
    List<UserSeries> findAllForUser(Long userId);
}
