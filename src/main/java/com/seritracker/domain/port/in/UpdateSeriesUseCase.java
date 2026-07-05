package com.seritracker.domain.port.in;

import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;

public interface UpdateSeriesUseCase {
    UserSeries updateStatus(Long userId, Long id, SeriesStatus status);
    UserSeries updateRating(Long userId, Long id, Integer rating);
    UserSeries updateWatchedEpisodes(Long userId, Long id, Integer episodes);
    UserSeries updateNotes(Long userId, Long id, String notes);
}