package com.seritracker.domain.port.in;

import com.seritracker.domain.model.UserSeries;

public interface CreateSeriesUseCase {
    UserSeries createSeries(Long userId, Integer tmdbId, String status);
}