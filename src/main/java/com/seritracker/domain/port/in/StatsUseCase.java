package com.seritracker.domain.port.in;

import com.seritracker.domain.model.UserStats;

public interface StatsUseCase {
    UserStats getStats(Long userId);
}
