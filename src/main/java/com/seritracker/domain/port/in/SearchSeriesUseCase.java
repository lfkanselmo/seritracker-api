package com.seritracker.domain.port.in;

import com.seritracker.domain.model.PageRequest;
import com.seritracker.domain.model.PageResult;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;

public interface SearchSeriesUseCase {
    PageResult<UserSeries> listAllByUser(Long userId, PageRequest pageRequest);
    PageResult<UserSeries> listByStatus(Long userId, SeriesStatus status, PageRequest pageRequest);
    UserSeries getById(Long userId, Long id);
}
