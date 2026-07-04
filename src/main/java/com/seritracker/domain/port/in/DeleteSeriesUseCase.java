package com.seritracker.domain.port.in;

public interface DeleteSeriesUseCase {
    void deleteSeries(Long userId, Long id);
}