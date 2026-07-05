package com.seritracker.application.service;

import com.seritracker.domain.exception.SeriesNotFoundException;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.port.out.UserSeriesRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class SeriesLookup {

    private SeriesLookup() {
    }

    static UserSeries findOrThrow(UserSeriesRepository userSeriesRepository, Long userId, Long id) {
        UserSeries series = userSeriesRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Series id={} not found", id);
                    return new SeriesNotFoundException(id);
                });

        if (!series.getUserId().equals(userId)) {
            log.warn("Series id={} does not belong to userId={}", id, userId);
            throw new SeriesNotFoundException(id);
        }

        return series;
    }
}
