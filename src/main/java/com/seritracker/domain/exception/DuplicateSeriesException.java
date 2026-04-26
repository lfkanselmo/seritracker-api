package com.seritracker.domain.exception;

public class DuplicateSeriesException extends RuntimeException {

    public DuplicateSeriesException(Integer tmdbId) {
        super("Series with tmdbId " + tmdbId + " already exists in user list");
    }
}