package com.seritracker.domain.exception;

public class SeriesNotFoundException extends RuntimeException {

    public SeriesNotFoundException(Long id) {
        super("Series not found with id: " + id);
    }

    public SeriesNotFoundException(Integer tmdbId) {
        super("Series not found with tmdbId: " + tmdbId);
    }
}