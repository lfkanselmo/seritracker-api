package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.infrastructure.adapter.out.persistence.entity.UserSeriesEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaUserSeriesRepository extends JpaRepository<UserSeriesEntity, Long> {
    Page<UserSeriesEntity> findAllByUserId(Long userId, Pageable pageable);
    Page<UserSeriesEntity> findAllByUserIdAndTitleContainingIgnoreCase(Long userId, String search, Pageable pageable);
    List<UserSeriesEntity> findByUserIdAndStatus(Long userId, String status);
    Page<UserSeriesEntity> findByUserIdAndStatus(Long userId, String status, Pageable pageable);
    Page<UserSeriesEntity> findByUserIdAndStatusAndTitleContainingIgnoreCase(Long userId, String status, String search, Pageable pageable);
    boolean existsByUserIdAndTmdbId(Long userId, Integer tmdbId);
    List<UserSeriesEntity> findByUserId(Long userId);

    @Query("SELECT DISTINCT u.userId FROM UserSeriesEntity u")
    List<Long> findAllDistinctUserIds();
}
