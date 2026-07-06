package com.seritracker.domain.port.in;

import com.seritracker.domain.model.UpcomingEpisode;

import java.util.List;

public interface CalendarUseCase {
    List<UpcomingEpisode> getUpcomingEpisodes(Long userId);
}
