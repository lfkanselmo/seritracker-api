package com.seritracker.application.service;

import com.seritracker.domain.model.Notification;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.port.in.CheckUpcomingEpisodesUseCase;
import com.seritracker.domain.port.out.NotificationRepository;
import com.seritracker.domain.port.out.TmdbClient;
import com.seritracker.domain.port.out.UserSeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeCheckService implements CheckUpcomingEpisodesUseCase {

    private final NotificationRepository notificationRepository;
    private final UserSeriesRepository   userSeriesRepository;
    private final TmdbClient             tmdbClient;

    @Override
    @Scheduled(cron = "0 0 8 * * *") // Todos los días a las 8am
    public void checkUpcomingEpisodes() {
        log.info("Starting upcoming episodes check");

        List<Long> userIds = notificationRepository.findAllUserIds();
        log.info("Checking episodes for {} users", userIds.size());

        userIds.forEach(this::checkEpisodesForUser);

        log.info("Upcoming episodes check completed");
    }

    // ── Métodos privados ───────────────────────────────────────────────

    private void checkEpisodesForUser(Long userId) {
        List<UserSeries> watchingSeries = userSeriesRepository
                .findByUserIdAndStatus(userId, SeriesStatus.WATCHING);

        watchingSeries.forEach(series -> checkSeriesForUser(userId, series));
    }

    private void checkSeriesForUser(Long userId, UserSeries series) {
        try {
            var tmdbData = tmdbClient.getSeriesDetails(series.getTmdbId());

            if (tmdbData.getNextAirDate() == null) return;

            LocalDate nextAirDate = tmdbData.getNextAirDate();
            LocalDate today       = LocalDate.now();
            LocalDate tomorrow    = today.plusDays(1);

            if (!nextAirDate.equals(today) && !nextAirDate.equals(tomorrow)) return;

            String episodeCode = buildEpisodeCode(series.getTmdbId());

            if (notificationRepository.existsByUserIdAndTmdbIdAndEpisodeCode(
                    userId, series.getTmdbId(), episodeCode)) return;

            Notification notification = Notification.builder()
                    .userId(userId)
                    .tmdbId(series.getTmdbId())
                    .seriesTitle(series.getTitle())
                    .episodeCode(episodeCode)
                    .airDate(nextAirDate)
                    .read(false)
                    .build();

            notificationRepository.save(notification);
            log.info("Notification created for userId={} series='{}' airDate={}",
                    userId, series.getTitle(), nextAirDate);

        } catch (Exception e) {
            log.error("Failed to check episodes for userId={} tmdbId={}",
                    userId, series.getTmdbId(), e);
        }
    }

    private String buildEpisodeCode(Integer tmdbId) {
        return "tmdb-" + tmdbId + "-" + LocalDate.now();
    }
}
