package com.seritracker.domain.port.in;

import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;

import java.util.List;

public interface SearchSeriesUseCase {
    List<UserSeries> listAllByUser(Long userId);
    List<UserSeries> listByStatus(Long userId, SeriesStatus status);
    UserSeries getById(Long id);
}