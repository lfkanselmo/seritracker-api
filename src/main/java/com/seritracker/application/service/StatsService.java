package com.seritracker.application.service;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        int totalEpisodesWatched = 0;
        long totalMinutesWatched = 0;
        int episodesWatchedThisYear = 0;
        UserSeries mostWatchedSeriesThisYear = null;
        int mostWatchedSeriesEpisodeCount = 0;
        Map<String, Integer> genreCountsThisYear = new LinkedHashMap<>();
        TreeSet<LocalDate> watchDatesThisYear = new TreeSet<>();

        for (UserSeries series : allSeries) {
            List<EpisodeWatch> watches = episodeWatchRepository.findByUserSeriesId(series.getId());
            if (watches.isEmpty()) continue;

            totalEpisodesWatched += watches.size();

            Series tmdbData = fetchTmdbDataSafely(series);

            if (tmdbData != null && tmdbData.getEpisodeRuntimeMinutes() != null) {
                totalMinutesWatched += (long) watches.size() * tmdbData.getEpisodeRuntimeMinutes();
            }

            List<EpisodeWatch> watchesThisYear = watches.stream()
                    .filter(w -> w.getWatchedAt().getYear() == currentYear)
                    .toList();

            if (watchesThisYear.isEmpty()) continue;

            episodesWatchedThisYear += watchesThisYear.size();
            watchesThisYear.forEach(w -> watchDatesThisYear.add(w.getWatchedAt().toLocalDate()));

            if (watchesThisYear.size() > mostWatchedSeriesEpisodeCount) {
                mostWatchedSeriesEpisodeCount = watchesThisYear.size();
                mostWatchedSeriesThisYear = series;
            }

            if (tmdbData != null) {
                for (String genre : tmdbData.getGenres()) {
                    genreCountsThisYear.merge(genre, watchesThisYear.size(), Integer::sum);
                }
            }
        }

        YearSummary currentYearSummary = YearSummary.builder()
                .year(currentYear)
                .episodesWatched(episodesWatchedThisYear)
                .topGenres(topGenres(genreCountsThisYear))
                .mostWatchedSeriesTitle(mostWatchedSeriesThisYear != null ? mostWatchedSeriesThisYear.getTitle() : null)
                .mostWatchedSeriesEpisodeCount(mostWatchedSeriesThisYear != null ? mostWatchedSeriesEpisodeCount : null)
                .longestStreakDays(longestStreak(watchDatesThisYear))
                .build();

        return UserStats.builder()
                .totalEpisodesWatched(totalEpisodesWatched)
                .totalMinutesWatched(totalMinutesWatched)
                .totalSeriesTracked(allSeries.size())
                .totalSeriesCompleted((int) allSeries.stream().filter(s -> s.getStatus() == SeriesStatus.COMPLETED).count())
                .currentYear(currentYearSummary)
                .build();
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
}
