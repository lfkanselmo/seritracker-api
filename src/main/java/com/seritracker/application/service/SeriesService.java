package com.seritracker.application.service;

import com.seritracker.domain.exception.DuplicateSeriesException;
import com.seritracker.domain.exception.SeriesNotFoundException;
import com.seritracker.domain.model.PageRequest;
import com.seritracker.domain.model.PageResult;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
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
    public UserSeries createSeries(Long userId, Integer tmdbId, SeriesStatus status) {
        log.info("Creating series tmdbId={} for userId={} with status={}", tmdbId, userId, status);
        validateNoDuplicate(userId, tmdbId);

        Series tmdbData = tmdbClient.getSeriesDetails(tmdbId);

        UserSeries userSeries = UserSeries.builder()
                .userId(userId)
                .tmdbId(tmdbId)
                .title(tmdbData.getTitle())
                .posterUrl(tmdbData.getPosterUrl())
                .totalEpisodes(tmdbData.getTotalEpisodes())
                .network(tmdbData.getNetwork())
                .status(status)
                .watchedEpisodes(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserSeries saved = userSeriesRepository.save(userSeries);
        log.info("Series id={} title='{}' created successfully for userId={}", saved.getId(), saved.getTitle(), userId);
        return saved;
    }

    @Override
    public UserSeries updateStatus(Long userId, Long id, SeriesStatus status) {
        log.info("Updating status of series id={} to {}", id, status);
        UserSeries existing = findOrThrow(userId, id);
        UserSeries updated = userSeriesRepository.save(existing.withStatus(status));
        log.info("Series id={} status updated to {}", id, status);
        return updated;
    }

    @Override
    public UserSeries updateRating(Long userId, Long id, Integer rating) {
        log.info("Updating rating of series id={} to {}", id, rating);
        UserSeries existing = findOrThrow(userId, id);
        return userSeriesRepository.save(existing.withRating(rating));
    }

    @Override
    public UserSeries updateWatchedEpisodes(Long userId, Long id, Integer episodes) {
        log.info("Updating watched episodes of series id={} to {}", id, episodes);
        UserSeries existing = findOrThrow(userId, id);
        return userSeriesRepository.save(existing.withWatchedEpisodes(episodes));
    }

    @Override
    public void deleteSeries(Long userId, Long id) {
        log.info("Deleting series id={}", id);
        findOrThrow(userId, id);
        userSeriesRepository.deleteById(id);
        log.info("Series id={} deleted successfully", id);
    }

    @Override
    public PageResult<UserSeries> listAllByUser(Long userId, PageRequest pageRequest) {
        log.debug("Listing series for userId={} page={} size={}", userId, pageRequest.getPage(), pageRequest.getSize());
        return userSeriesRepository.findAllByUserId(userId, pageRequest);
    }

    @Override
    public PageResult<UserSeries> listByStatus(Long userId, SeriesStatus status, PageRequest pageRequest) {
        log.debug("Listing series for userId={} status={} page={} size={}", userId, status, pageRequest.getPage(), pageRequest.getSize());
        return userSeriesRepository.findByUserIdAndStatus(userId, status, pageRequest);
    }

    @Override
    public UserSeries getById(Long userId, Long id) {
        log.debug("Fetching series id={}", id);
        return findOrThrow(userId, id);
    }

    // ── Métodos privados ───────────────────────────────────────────────

    private UserSeries findOrThrow(Long userId, Long id) {
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

    private void validateNoDuplicate(Long userId, Integer tmdbId) {
        if (userSeriesRepository.existsByUserIdAndTmdbId(userId, tmdbId)) {
            log.warn("Duplicate series tmdbId={} for userId={}", tmdbId, userId);
            throw new DuplicateSeriesException(tmdbId);
        }
    }
}