package com.seritracker.application.service;

import com.seritracker.domain.model.Badge;
import com.seritracker.domain.model.BadgeCode;
import com.seritracker.domain.model.EpisodeWatch;
import com.seritracker.domain.model.GenreStat;
import com.seritracker.domain.model.Series;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.model.UserStats;
import com.seritracker.domain.model.YearSummary;
import com.seritracker.domain.port.in.StatsUseCase;
import com.seritracker.domain.port.out.EpisodeWatchRepository;
import com.seritracker.domain.port.out.TmdbClient;
import com.seritracker.domain.port.out.UserSeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService implements StatsUseCase {

    private static final int TOP_GENRES_LIMIT = 5;

    private final UserSeriesRepository userSeriesRepository;
    private final EpisodeWatchRepository episodeWatchRepository;
    private final TmdbClient tmdbClient;

    @Override
    public UserStats getStats(Long userId) {
        List<UserSeries> allSeries = userSeriesRepository.findAllForUser(userId);
        int currentYear = LocalDate.now().getYear();

        WatchAccumulator acc = new WatchAccumulator();
        for (UserSeries series : allSeries) {
            accumulateSeriesWatches(series, currentYear, acc);
        }

        int totalSeriesCompleted = (int) allSeries.stream().filter(s -> s.getStatus() == SeriesStatus.COMPLETED).count();
        int currentStreakDays = currentStreak(acc.allWatchDates);

        return UserStats.builder()
                .totalEpisodesWatched(acc.totalEpisodesWatched)
                .totalMinutesWatched(acc.totalMinutesWatched)
                .totalSeriesTracked(allSeries.size())
                .totalSeriesCompleted(totalSeriesCompleted)
                .currentStreakDays(currentStreakDays)
                .badges(computeBadges(acc.totalEpisodesWatched, totalSeriesCompleted, currentStreakDays, acc.allGenresWatched.size()))
                .currentYear(buildYearSummary(currentYear, acc))
                .build();
    }

    private void accumulateSeriesWatches(UserSeries series, int currentYear, WatchAccumulator acc) {
        List<EpisodeWatch> watches = episodeWatchRepository.findByUserSeriesId(series.getId());
        if (watches.isEmpty()) return;

        acc.totalEpisodesWatched += watches.size();
        watches.forEach(w -> acc.allWatchDates.add(w.getWatchedAt().toLocalDate()));

        Series tmdbData = fetchTmdbDataSafely(series);
        accumulateTmdbData(watches.size(), tmdbData, acc);

        List<EpisodeWatch> watchesThisYear = watches.stream()
                .filter(w -> w.getWatchedAt().getYear() == currentYear)
                .toList();

        if (!watchesThisYear.isEmpty()) {
            accumulateThisYearStats(series, watchesThisYear, tmdbData, acc);
        }
    }

    private void accumulateTmdbData(int watchCount, Series tmdbData, WatchAccumulator acc) {
        if (tmdbData == null) return;

        if (tmdbData.getEpisodeRuntimeMinutes() != null) {
            acc.totalMinutesWatched += (long) watchCount * tmdbData.getEpisodeRuntimeMinutes();
        }
        acc.allGenresWatched.addAll(tmdbData.getGenres());
    }

    private void accumulateThisYearStats(UserSeries series, List<EpisodeWatch> watchesThisYear,
                                          Series tmdbData, WatchAccumulator acc) {
        acc.episodesWatchedThisYear += watchesThisYear.size();
        watchesThisYear.forEach(w -> acc.watchDatesThisYear.add(w.getWatchedAt().toLocalDate()));

        if (watchesThisYear.size() > acc.mostWatchedSeriesEpisodeCount) {
            acc.mostWatchedSeriesEpisodeCount = watchesThisYear.size();
            acc.mostWatchedSeriesThisYear = series;
        }

        if (tmdbData == null) return;
        for (String genre : tmdbData.getGenres()) {
            acc.genreCountsThisYear.merge(genre, watchesThisYear.size(), Integer::sum);
        }
    }

    private YearSummary buildYearSummary(int currentYear, WatchAccumulator acc) {
        return YearSummary.builder()
                .year(currentYear)
                .episodesWatched(acc.episodesWatchedThisYear)
                .topGenres(topGenres(acc.genreCountsThisYear))
                .mostWatchedSeriesTitle(acc.mostWatchedSeriesThisYear != null ? acc.mostWatchedSeriesThisYear.getTitle() : null)
                .mostWatchedSeriesEpisodeCount(acc.mostWatchedSeriesThisYear != null ? acc.mostWatchedSeriesEpisodeCount : null)
                .longestStreakDays(longestStreak(acc.watchDatesThisYear))
                .build();
    }

    private static final class WatchAccumulator {
        int totalEpisodesWatched;
        long totalMinutesWatched;
        int episodesWatchedThisYear;
        UserSeries mostWatchedSeriesThisYear;
        int mostWatchedSeriesEpisodeCount;
        final Map<String, Integer> genreCountsThisYear = new LinkedHashMap<>();
        final TreeSet<LocalDate> watchDatesThisYear = new TreeSet<>();
        final TreeSet<LocalDate> allWatchDates = new TreeSet<>();
        final Set<String> allGenresWatched = new HashSet<>();
    }

    private Series fetchTmdbDataSafely(UserSeries series) {
        try {
            return tmdbClient.getSeriesDetails(series.getTmdbId());
        } catch (Exception e) {
            log.warn("Failed to fetch TMDB details for stats, tmdbId={}", series.getTmdbId(), e);
            return null;
        }
    }

    private List<GenreStat> topGenres(Map<String, Integer> genreCounts) {
        return genreCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(TOP_GENRES_LIMIT)
                .map(e -> GenreStat.builder().genre(e.getKey()).episodeCount(e.getValue()).build())
                .toList();
    }

    private int longestStreak(TreeSet<LocalDate> sortedDates) {
        if (sortedDates.isEmpty()) return 0;

        List<LocalDate> dates = new ArrayList<>(sortedDates);
        int longest = 1;
        int current = 1;

        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).equals(dates.get(i - 1).plusDays(1))) {
                current++;
            } else {
                current = 1;
            }
            longest = Math.max(longest, current);
        }

        return longest;
    }

    /**
     * Racha activa hasta hoy — se corta si el último día visto no fue hoy ni ayer,
     * a diferencia de longestStreak() que busca el máximo histórico dentro de un rango.
     */
    private int currentStreak(TreeSet<LocalDate> allWatchDates) {
        if (allWatchDates.isEmpty()) return 0;

        LocalDate mostRecent = allWatchDates.last();
        LocalDate today = LocalDate.now();
        if (mostRecent.isBefore(today.minusDays(1))) return 0;

        int streak = 1;
        LocalDate cursor = mostRecent;
        while (allWatchDates.contains(cursor.minusDays(1))) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        return streak;
    }

    private List<Badge> computeBadges(int totalEpisodesWatched, int totalSeriesCompleted,
                                       int currentStreakDays, int distinctGenresWatched) {
        return List.of(
                badge(BadgeCode.FIRST_EPISODE, totalEpisodesWatched, 1),
                badge(BadgeCode.BINGE_WATCHER, totalEpisodesWatched, 100),
                badge(BadgeCode.TRUE_FAN, totalEpisodesWatched, 500),
                badge(BadgeCode.FIRST_COMPLETE, totalSeriesCompleted, 1),
                badge(BadgeCode.COLLECTOR, totalSeriesCompleted, 5),
                badge(BadgeCode.WEEK_STREAK, currentStreakDays, 7),
                badge(BadgeCode.MONTH_STREAK, currentStreakDays, 30),
                badge(BadgeCode.GENRE_EXPLORER, distinctGenresWatched, 5)
        );
    }

    private Badge badge(BadgeCode code, int current, int target) {
        return Badge.builder()
                .code(code)
                .earned(current >= target)
                .progressCurrent(Math.min(current, target))
                .progressTarget(target)
                .build();
    }
}
