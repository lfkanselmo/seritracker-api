package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import com.seritracker.domain.model.Notification;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder
public class NotificationResponse {

    Long      id;
    Integer   tmdbId;
    String    seriesTitle;
    String    episodeCode;
    LocalDate airDate;
    LocalDateTime sentAt;
    Boolean   read;
    Boolean   isToday;
    Boolean   isTomorrow;

    public static NotificationResponse from(Notification domain) {
        LocalDate today    = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        return NotificationResponse.builder()
                .id(domain.getId())
                .tmdbId(domain.getTmdbId())
                .seriesTitle(domain.getSeriesTitle())
                .episodeCode(domain.getEpisodeCode())
                .airDate(domain.getAirDate())
                .sentAt(domain.getSentAt())
                .read(domain.getRead())
                .isToday(domain.getAirDate().equals(today))
                .isTomorrow(domain.getAirDate().equals(tomorrow))
                .build();
    }
}