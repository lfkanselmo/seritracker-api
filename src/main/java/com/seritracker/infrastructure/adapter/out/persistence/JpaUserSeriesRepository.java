package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.infrastructure.adapter.out.persistence.entity.UserSeriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaUserSeriesRepository extends JpaRepository<UserSeriesEntity, Long> {
    List<UserSeriesEntity> findAllByUserId(Long userId);
    List<UserSeriesEntity> findByUserIdAndStatus(Long userId, String status);
    boolean existsByUserIdAndTmdbId(Long userId, Integer tmdbId);
}