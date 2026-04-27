package com.seritracker.application.service;

import com.seritracker.domain.exception.DuplicateSeriesException;
import com.seritracker.domain.exception.SeriesNotFoundException;
import com.seritracker.domain.model.Series;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.port.in.CreateSeriesUseCase;
import com.seritracker.domain.port.in.DeleteSeriesUseCase;
import com.seritracker.domain.port.in.SearchSeriesUseCase;
import com.seritracker.domain.port.in.UpdateSeriesUseCase;
import com.seritracker.domain.port.out.TmdbClient;
import com.seritracker.domain.port.out.UserSeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeriesService implements
        CreateSeriesUseCase,
        UpdateSeriesUseCase,
        DeleteSeriesUseCase,
        SearchSeriesUseCase {

    private final UserSeriesRepository userSeriesRepository;
    private final TmdbClient tmdbClient;

    @Override
    public UserSeries createSeries(Long userId, Integer tmdbId, String status) {
        validateNoDuplicate(userId, tmdbId);

        Series tmdbData = tmdbClient.getSeriesDetails(tmdbId);

        UserSeries userSeries = UserSeries.builder()
                .userId(userId)
                .tmdbId(tmdbId)
                .title(tmdbData.getTitle())
                .posterUrl(tmdbData.getPosterUrl())
                .totalEpisodes(tmdbData.getTotalEpisodes())
                .network(tmdbData.getNetwork())
                .status(SeriesStatus.valueOf(status))
                .watchedEpisodes(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return userSeriesRepository.save(userSeries);
    }

    @Override
    public UserSeries updateStatus(Long id, SeriesStatus status) {
        UserSeries existing = findOrThrow(id);
        return userSeriesRepository.save(existing.withStatus(status));
    }

    @Override
    public UserSeries updateRating(Long id, Integer rating) {
        UserSeries existing = findOrThrow(id);
        return userSeriesRepository.save(existing.withRating(rating));
    }

    @Override
    public UserSeries updateWatchedEpisodes(Long id, Integer episodes) {
        UserSeries existing = findOrThrow(id);
        return userSeriesRepository.save(existing.withWatchedEpisodes(episodes));
    }

    @Override
    public void deleteSeries(Long id) {
        findOrThrow(id);
        userSeriesRepository.deleteById(id);
    }

    @Override
    public List<UserSeries> listAllByUser(Long userId) {
        return userSeriesRepository.findAllByUserId(userId);
    }

    @Override
    public List<UserSeries> listByStatus(Long userId, SeriesStatus status) {
        return userSeriesRepository.findByUserIdAndStatus(userId, status);
    }

    @Override
    public UserSeries getById(Long id) {
        return findOrThrow(id);
    }

    // ── Métodos privados de apoyo ──────────────────────────────────────────

    private UserSeries findOrThrow(Long id) {
        return userSeriesRepository.findById(id)
                .orElseThrow(() -> new SeriesNotFoundException(id));
    }

    private void validateNoDuplicate(Long userId, Integer tmdbId) {
        if (userSeriesRepository.existsByUserIdAndTmdbId(userId, tmdbId)) {
            throw new DuplicateSeriesException(tmdbId);
        }
    }
}