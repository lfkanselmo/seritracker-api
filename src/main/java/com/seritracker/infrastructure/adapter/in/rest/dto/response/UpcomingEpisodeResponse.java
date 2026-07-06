package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import com.seritracker.domain.model.UpcomingEpisode;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class UpcomingEpisodeResponse {

    Long userSeriesId;
    Integer tmdbId;
    String seriesTitle;
    String posterUrl;
    Integer seasonNumber;
    Integer episodeNumber;
    String episodeTitle;
    LocalDate airDate;
    Boolean isToday;
    Boolean isTomorrow;

    public static UpcomingEpisodeResponse from(UpcomingEpisode domain) {
        LocalDate today    = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        return UpcomingEpisodeResponse.builder()
                .userSeriesId(domain.getUserSeriesId())
                .tmdbId(domain.getTmdbId())
                .seriesTitle(domain.getSeriesTitle())
                .posterUrl(domain.getPosterUrl())
                .seasonNumber(domain.getSeasonNumber())
                .episodeNumber(domain.getEpisodeNumber())
                .episodeTitle(domain.getEpisodeTitle())
                .airDate(domain.getAirDate())
                .isToday(domain.getAirDate().equals(today))
                .isTomorrow(domain.getAirDate().equals(tomorrow))
                .build();
    }
}
