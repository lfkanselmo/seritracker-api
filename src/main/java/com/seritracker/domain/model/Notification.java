package com.seritracker.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder
public class Notification {
    Long      id;
    Long      userId;
    Integer   tmdbId;
    String    seriesTitle;
    String    episodeCode;
    LocalDate airDate;
    LocalDateTime sentAt;
    Boolean   read;
}